package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import org.http4s.circe.*
import io.circe.generic.auto.*
import net.martinprobson.example.common.model.User
import org.http4s.{Method, Request}
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.ember.client.*
import org.http4s.implicits.http4sLiteralsSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MyClient extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is
    * lifted into a Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = program

  def req(name: String): Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
    .withEntity(User(name))

  def call(name: String, client: Client[IO]): IO[User] = log.info(s"call $name") >> client
    .expect(req(name))(jsonOf[IO, User])

  val program: IO[Unit] = EmberClientBuilder
    .default[IO]
    .withRetryPolicy(RateLimitRetry.retry)
    .build
    .use { c =>
      for {
        _ <- log.info("In client")
        client = RequestLogger(logHeaders = true, logBody = true)(ResponseLogger(logHeaders = true, logBody = true)(c))
        _ <- Range.inclusive(1,20).toList.traverse(i => call(s"name-$i",client).flatMap(u => log.info(s"Got $u")) )
      } yield ()
    }
}
