package net.martinprobson.example.server.db.repository

import cats.effect.IO
import fs2.Stream
import net.martinprobson.example.common.model.User
import net.martinprobson.example.common.model.User.USER_ID

trait UserRepository {

  def addUser(user: User): IO[User]
  def addUsers(users: List[User]): IO[List[User]]
  def getUser(id: USER_ID): IO[Option[User]]
  def getUserByName(name: String): IO[List[User]]
  def getUsers: IO[List[User]]
  def getUsersStream: Stream[IO, User]
  def countUsers: IO[Long]
  def getOrAdd(user: User): IO[User] = for {
    userList <- getUserByName(user.name)
    newUser <-
      userList.headOption match {
        case Some(o) => IO(o)
        case None    => addUser(user)
      }
  } yield newUser
}
