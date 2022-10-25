package net.martinprobson.example.common.model

import User.*
import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

case class User(id: USER_ID, name: String)

object User {

  implicit val userEncoder: EntityEncoder[IO, User] = jsonEncoderOf[IO, User]

  implicit val usersEncoder: EntityEncoder[IO, List[User]] = jsonEncoderOf[IO, List[User]]

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

  def apply(id: USER_ID, name: String): User = new User(id, name)
  def apply(name: String): User = new User(UNASSIGNED_USER_ID, name)

  type USER_ID = Long
  val UNASSIGNED_USER_ID = 0L
}
