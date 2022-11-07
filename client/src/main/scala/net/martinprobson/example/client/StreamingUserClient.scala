package net.martinprobson.example.client

import cats.effect.{IO, IOApp}
import fs2.{Stream, text}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object StreamingUserClient extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def userStream(client: Client[IO], req: Request[IO]): Stream[IO, Byte] = for {
    c <- Stream(client)
    sr <- Stream.eval(IO(req))
    res <- c.stream(sr).flatMap(_.body)
  } yield res

  def stream(client: Client[IO]): Stream[IO,String] = {
    val request = Request[IO](Method.GET, uri"http://localhost:8085/usersstream")
    val s = userStream(client, request).chunks.flatMap(c => Stream.chunk(c))
    s.through(text.utf8.decode).evalTap(l => log.info(l))
  }

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is
    * lifted into a Resource so that it can be used to setup the connection pool.
    */
  //override def run: IO[Unit] = stream.compile.drain
  override  def run: IO[Unit] = EmberClientBuilder
    .default[IO]
    .withRetryPolicy(RateLimitRetry.retry)
    .build
    .use { client =>
      stream(client).compile.drain
    }
}
