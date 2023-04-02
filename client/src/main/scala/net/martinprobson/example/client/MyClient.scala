package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import org.http4s.circe.*
import fs2.Stream
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
    * <p>Within the context of an EmberClient, generate some user files, call the postUsers method
    * to read the files back into a fs2 Stream and post them to an http endpoint and then stream them
    * back out again from the server.</p>
    * <p>We provide a transactor which will be used by Doobie to execute the SQL statements. Config is lifted into a
    * Resource so that it can be used to setup the connection pool.</p>
    */
  override def run: IO[Unit] = {
    EmberClientBuilder
      .default[IO]
      .withRetryPolicy(RateLimitRetry.retry)
      .withLogger(log)
      .build
      .onFinalize(log.info("Shutdown of EmberClient"))
      .use ( client => for {
        _ <- GenerateUserFiles.generateUserFiles(1,3)
        _ <- postUsers(client)
          //TODO Look into setting up a flaky server to unit test this
          //TODO Use broadcastThrough to write two failure files, 1 = users 2 = error msg and user
          //TODO See https://stackoverflow.com/a/71603380
          .filter(_.isLeft)
          .evalTap( e => log.error(e.toString))
          .map {
            case Left((_, u)) => u
            case Right(u) => u
          }
          .broadcastThrough()
          .compile
          .drain
 //       _ <- StreamingUserClient.stream(client).compile.drain
      } yield ()
      )
  }

  private def postUser(user: User, client: Client[IO]): IO[Either[(String,User),User]] = {
    def req(user: User): Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
      .withEntity(user)
    log.info(s"call $user") >>
        client.expect(req(user))(jsonOf[IO, User]).map(u => Right(u)).handleError(e => Left((e.toString,user)))
  }

  private def postUsers(client: Client[IO]): Stream[IO, Either[(String,User),User]] = for {
    c <- Stream(client)
    s <- ReadUserFiles.reader
      .parEvalMap(1000)(user => postUser(user, c))
  } yield s
}
