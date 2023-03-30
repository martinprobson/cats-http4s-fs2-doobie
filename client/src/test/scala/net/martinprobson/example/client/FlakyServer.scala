package net.martinprobson.example.client

import cats.effect.IO
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import org.http4s.{HttpRoutes, Request}
import org.http4s.circe.jsonEncoder
import io.circe.generic.auto.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object FlakyServer {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def postUser(request: Request[IO]): IO[User] = for {
    user <- request.as[User]
    _ <- log.info(s"Got User: $user")
  } yield user


  def httpRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@POST -> Root / "user" =>
      postUser(req).flatMap(u => Ok(u.asJson))
  }

  def httpRoutes2: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _@POST -> Root / "user" =>
      log.info("In hello world!") >> BadRequest("Server Error!")
  }
}
