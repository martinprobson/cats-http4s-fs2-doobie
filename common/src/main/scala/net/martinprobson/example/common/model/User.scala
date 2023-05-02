package net.martinprobson.example.common.model

import User.*
import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

case class User(id: USER_ID, name: String, email: String)

object User {

  given EntityEncoder[IO, User] = jsonEncoderOf[IO, User]
  given EntityEncoder[IO, List[User]] = jsonEncoderOf[IO, List[User]]
  given EntityDecoder[IO, User] = jsonOf[IO, User]

  def apply(id: USER_ID, name: String, email: String): User = new User(id, name, email)
  def apply(name: String, email: String): User = new User(UNASSIGNED_USER_ID, name, email)

  type USER_ID = Long
  val UNASSIGNED_USER_ID = 0L
}
