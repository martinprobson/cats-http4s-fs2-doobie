package net.martinprobson.example.server.db.repository

import cats.effect.*
import cats.free.Free
import cats.syntax.all.*
import doobie.*
import doobie.free.connection
import doobie.free.connection.ConnectionOp
import doobie.implicits.*
import fs2.Stream
import net.martinprobson.example.common.model.User
import net.martinprobson.example.common.model.User.USER_ID
import net.martinprobson.example.server.db.repository
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class DoobieUserRepository(xa: Transactor[IO]) extends UserRepository {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  private def insert(user: User): IO[User] = (for
    _ <- sql"INSERT INTO user (name, email) VALUES (${user.name},${user.email})".update.run
    id <- sql"SELECT last_insert_id()".query[Long].unique
    user <- Free.pure[ConnectionOp, User](User(id, user.name, user.email))
  yield user).transact(xa)

  private def select(id: USER_ID): ConnectionIO[Option[User]] =
    sql"SELECT id, name, email FROM user WHERE id = $id".query[User].option

  private def selectCount: ConnectionIO[Long] =
    sql"SELECT COUNT(*) FROM user".query[Long].unique

  private def selectAll: Stream[IO, User] =
    sql"SELECT id, name, email FROM user".query[User].stream.transact(xa)

  private def selectByName(name: String): ConnectionIO[List[User]] =
    sql"SELECT id, name, email FROM user WHERE name = $name"
      .query[User]
      .stream
      .compile
      .toList

  private def selectPaged(pageNo: Int, pageSize: Int): ConnectionIO[List[User]] = {
    val offset = pageNo * pageSize
    sql"SELECT id, name, email FROM user ORDER BY id LIMIT $pageSize OFFSET $offset"
      .query[User]
      .stream
      .compile
      .toList
  }

  override def addUser(user: User): IO[User] = for {
    _ <- log.debug(s"About to create : $user")
    user <- insert(user)
    _ <- log.debug(s"Created user: $user")
  } yield user

  override def addUsers(users: List[User]): IO[List[User]] = users.traverse(addUser)

  override def getUser(id: USER_ID): IO[Option[User]] = select(id).transact(xa)

  override def getUserPaged(pageNo: Int, pageSize: Int): IO[List[User]] = selectPaged(pageNo, pageSize).transact(xa)

  override def getUserByName(name: String): IO[List[User]] = selectByName(name).transact(xa)

  override def getUsers: IO[List[User]] = getUsersStream.compile.toList

  override def getUsersStream: Stream[IO, User] = selectAll

  override def countUsers: IO[Long] = selectCount.transact(xa)

  def createTable: IO[Int] =
    sql"""
         |create table if not exists user
         |(
         |    id   int auto_increment
         |        primary key,
         |    name  varchar(100) null,
         |    email varchar(100) null
         |         );
         |""".stripMargin.update.run.transact(xa)
}

object DoobieUserRepository {
  def apply(xa: Transactor[IO]): IO[DoobieUserRepository] = for {
    userRepository <- IO(new DoobieUserRepository(xa))
    _ <- userRepository.createTable
  } yield userRepository

}
