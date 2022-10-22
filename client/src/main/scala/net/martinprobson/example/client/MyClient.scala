package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import io.circe.generic.auto.*
import net.martinprobson.example.common.model.User
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{EntityDecoder, EntityEncoder}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MyClient extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  implicit val userEncoder: EntityEncoder[IO, User] = jsonEncoderOf[IO, User]

  implicit val usersEncoder: EntityEncoder[IO, List[User]] = jsonEncoderOf[IO, List[User]]

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is
    * lifted into a Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = program >> log.info("Hello")

  def call(client: Client[IO]): IO[String] = client
    .expect[String]("http://localhost:8085/hello")
    .flatMap(s => { log.info(s"Got $s"); IO(s) })

  val program: IO[Unit] = EmberClientBuilder
    .default[IO]
    .build
    .use { client =>
      for {
        _ <- log.info("In client")
        _ <- call(client)
      } yield ()
    }
}
