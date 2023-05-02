package net.martinprobson.example.server.db.repository

import cats.effect.{IO, Ref}
import cats.implicits.toTraverseOps
import fs2.Stream
import net.martinprobson.example.common.model.User
import net.martinprobson.example.common.model.User.USER_ID
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.collection.immutable.SortedMap

class InMemoryUserRepository(db: Ref[IO, SortedMap[USER_ID, User]], counter: Ref[IO, Long]) extends UserRepository {

  override def addUser(user: User): IO[User] = for {
    logger <- Slf4jLogger.create[IO]
    _ <- logger.debug(s"About to create : $user")
    id <- counter.modify(x => (x + 1, x + 1))
    _ <- db.update(users => users.updated(key = id, value = user))
    user <- IO(User(id, user.name, user.email))
    _ <- logger.debug(s"Created user: $user")
  } yield user

  override def addUsers(users: List[User]): IO[List[User]] = users.traverse(addUser)

  override def getUser(id: USER_ID): IO[Option[User]] =
    db.get.map { users => users.get(key = id).map { user => User(id, user.name, user.email) } }

  override def getUserByName(name: String): IO[List[User]] = db.get.map { users =>
    users
      .filter { case (_, user) => user.name == name }
      .map { case (id, user) => User(id, user.name, user.email) }
      .toList
  }

  override def getUsers: IO[List[User]] = db.get.map { users =>
    users.map { case (id, user) => User(id, user.name, user.email) }.toList
  }

  override def getUsersStream: fs2.Stream[IO, User] = Stream.evalSeq(getUsers)

  override def countUsers: IO[Long] = db.get.flatMap { users => IO(users.size.toLong) }
  override def getUserPaged(pageNo: Int, pageSize: Int): IO[List[User]] = db.get.flatMap { users =>
    IO(users.slice(pageNo * pageSize, pageNo * pageSize + pageSize).toList.map { case (_, user) => user })
  }

}

object InMemoryUserRepository {

  def empty: IO[UserRepository] = for {
    db <- Ref[IO].of(SortedMap.empty[USER_ID, User])
    counter <- Ref[IO].of(0L)
  } yield new InMemoryUserRepository(db, counter)
}
