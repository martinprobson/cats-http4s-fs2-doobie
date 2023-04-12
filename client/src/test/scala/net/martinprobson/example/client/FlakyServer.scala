package net.martinprobson.example.client

import cats.effect.IO
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import org.http4s.HttpRoutes
import org.http4s.circe.jsonEncoder
import io.circe.generic.auto.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object FlakyServer {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def httpRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@POST -> Root / "user" => for {
      user <- req.as[User]
      _ <- log.info(s"Got User: $user")
      resp <- if (user.name.contains("fail")) {
        BadRequest("server error!")
      }
      else {
        Ok(user.asJson)
      }
    } yield resp
  }
}
