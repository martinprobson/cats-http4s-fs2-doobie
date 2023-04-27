package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.ember.client.*
import org.http4s.implicits.*
import org.http4s.{Method, Request}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.{Stream, text}
import fs2.{Stream, text}

import scala.concurrent.duration.DurationInt

object StreamingClient extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is lifted into a
    * Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = program

  def getStream(client: Client[IO]): Stream[IO, Byte] = {
    client
      .stream(Request[IO](Method.GET, uri"http://localhost:8085/seconds"))
      .flatMap(_.body)
  }

  val program: IO[Unit] = EmberClientBuilder
    .default[IO]
    .withRetryPolicy(RateLimitRetry.retry)
    .build
    .use { c =>
      getStream(c)
        .through(text.utf8.decode)
        .evalTap(s => log.info(s))
        .take(100)
        //.interruptAfter(60.seconds)
        .compile
        .toList
        .flatMap(s => log.info(s.mkString(",")))
        >> log.info("Done")
    }
}
