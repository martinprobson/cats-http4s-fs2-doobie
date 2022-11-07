package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import org.http4s.circe.*
import fs2.{Stream, text}
import io.circe.generic.auto.*
import net.martinprobson.example.common.model.User
import net.martinprobson.example.files.{GenerateUserFiles, ReadUserFiles}
import org.http4s.{Method, Request}
import org.http4s.client.Client
import org.http4s.ember.client.*
import org.http4s.implicits.http4sLiteralsSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
object MyClient extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is lifted into a
    * Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = {
    GenerateUserFiles.generateUserFiles(1000,10000) >>
    postUsers.compile.drain >>
      StreamingUserClient.stream.compile.drain
  }

  def postUser(user: User, client: Client[IO]): IO[User] = {
    def req(user: User): Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
      .withEntity(user)
    log.info(s"call $user") >>
      client.expect(req(user))(jsonOf[IO, User])
  }

  val postUsers: Stream[IO, Unit] = for {
    client <- Stream
      .resource(
        EmberClientBuilder
          .default[IO]
          .withRetryPolicy(RateLimitRetry.retry)
          .build
      )
    _ <- ReadUserFiles.reader
      .parEvalMap(10)(user => postUser(user, client).flatMap(u => log.info(s"Got $u")))
  } yield ()
}
