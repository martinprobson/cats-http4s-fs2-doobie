package net.martinprobson.example.server.db.repository

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import net.martinprobson.example.common.model.User
import net.martinprobson.example.common.model.User.USER_ID
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.Stream
import fs2.*
import net.martinprobson.example.common.config.Config

import java.sql.{Connection, ResultSet, Statement}
import javax.sql.DataSource
import JdbcUserRepository.*

//TODO Error handling is wrong (INSERT errors get swallowed for example)
//TODO DataSource is not being closed on program exit
class JdbcUserRepository(ds: DataSource) extends UserRepository {

  override def deleteUser(id: USER_ID): IO[Int] = deleteUser(ds, id)

  override def addUser(user: User): IO[User] = addUser(ds, user)

  override def addUsers(users: List[User]): IO[List[User]] = users.traverse(addUser)

  override def getUsers: IO[List[User]] = getUsers(ds)

  override def getUser(id: USER_ID): IO[Option[User]] = getUser(ds, id)

  override def getUserByName(name: String): IO[List[User]] = getUsersByName(ds, name)

  override def getUsersStream: fs2.Stream[IO, User] = Stream.evalSeq(getUsers)

  override def getUserPaged(pageNo: Int, pageSize: Int): IO[List[User]] = selectPaged(ds, pageNo, pageSize)

  override def countUsers: IO[Long] = countUsers(ds)


  private def selectPaged(dataSource: DataSource, pageNo: Int, pageSize: Int): IO[List[User]] = {
    createConnection(dataSource)
      .flatMap(createStatement)
      .flatMap(stmt => executeQuery(s"SELECT id, name, email FROM user " +
        "ORDER BY id LIMIT $pageSize OFFSET ${pageNo * pageSize};", stmt))
      .use { rs =>
        IO.blocking {
          val users = scala.collection.mutable.ListBuffer[User]()
          while (rs.next()) {
            users.append(User(rs.getInt(1), rs.getString(2), rs.getString(3)))
          }
          users.toList
        }
      }
  }

  private def addUser(dataSource: DataSource, user: User): IO[User] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .use { stmt =>
        executeUpdate(s"INSERT INTO user (name, email) VALUES ('${user.name}','${user.email}');", stmt)
          executeQuery("SELECT last_insert_id();", stmt).use { rs =>
            IO.blocking {
              if (rs.next()) {
                User(rs.getInt(1), user.name, user.email)
              } else {
                throw new Exception("whoops!")
              }
            }
          }
      }

  private def deleteUser(dataSource: DataSource, id: USER_ID): IO[Int] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .use { stmt =>
        executeUpdate(s"DELETE FROM user WHERE id = $id;", stmt)
      }

  private def getUser(dataSource: DataSource, id: USER_ID): IO[Option[User]] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .flatMap{stmt =>
        executeQuery(s"SELECT id, name, email FROM user WHERE id = $id;", stmt)
      }
      .use { rs =>
        IO.blocking {
          if !rs.next() then None
          else Some(User(rs.getInt(1), rs.getString(2), rs.getString(3)))
        }
      }

  private def countUsers(dataSource: DataSource): IO[Long] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .flatMap(stmt => executeQuery("SELECT COUNT(*) FROM user;", stmt))
      .use { rs =>
        IO.blocking {
          if !rs.next() then 0L
          else rs.getLong(1)
        }
      }

  private def getUsersByName(dataSource: DataSource, name: String): IO[List[User]] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .flatMap(stmt => executeQuery(s"SELECT id, name, email FROM user WHERE name = $name;", stmt))
      .use { rs =>
        IO.blocking {
          val users = scala.collection.mutable.ListBuffer[User]()
          while (rs.next()) {
            users.append(User(rs.getInt(1), rs.getString(2), rs.getString(3)))
          }
          users.toList
        }
      }

  private def getUsers(dataSource: DataSource): IO[List[User]] =
    createConnection(dataSource)
      .flatMap(createStatement)
      .flatMap(stmt => executeQuery("SELECT id, name, email FROM user LIMIT 10;", stmt))
      .use { rs =>
        IO.blocking {
          val users = scala.collection.mutable.ListBuffer[User]()
          while (rs.next()) {
            users.append(User(rs.getInt(1), rs.getString(2), rs.getString(3)))
          }
          users.toList
        }
      }

  private def createConnection(dataSource: DataSource): Resource[IO, Connection] =
    Resource
      .make(log.debug("Create connection") *> IO.blocking(dataSource.getConnection))(connection =>
        IO.blocking(connection.close())
      )
      .onFinalize(log.debug("closing connection"))

  private def createStatement(connection: Connection): Resource[IO, Statement] =
    Resource
      .make(log.debug("Create statement") *> IO.blocking(connection.createStatement))(statement =>
        IO.blocking(statement.close())
      )
      .onFinalize(log.debug("closing statement"))

  private def executeQuery(query: String, statement: Statement): Resource[IO, ResultSet] =
    Resource
      .make(log.debug(s"executeQuery: $query") *> IO.blocking(statement.executeQuery(query)))(resultSet =>
        IO.blocking(resultSet.close())
      )
      .onFinalize(log.debug("closing resultSet"))

  private def executeUpdate(update: String, statement: Statement): IO[Int] = for {
    _ <- log.debug(s"executeUpdate: $update")
    result <- IO.blocking(statement.executeUpdate(update)).debug()
  } yield result

}

object JdbcUserRepository {
  def apply(dataSource: DataSource): IO[JdbcUserRepository] = IO(new JdbcUserRepository(dataSource))

  private def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def dataSource(config: IO[Config]): Resource[IO, DataSource] =
    Resource
      .make(
        log.debug("Create dataSource") *>
          config.map( c => {
            val hikariConfig = new HikariConfig()
            hikariConfig.setJdbcUrl(c.url)
            hikariConfig.setPassword(c.password)
            hikariConfig.setUsername(c.user)
            hikariConfig.setMaximumPoolSize(100)
            new HikariDataSource(hikariConfig)
          })
      )(ds => IO.blocking(ds.close()))
      .onFinalize(log.debug("closing dataSource"))
}
