package net.martinprobson.example.server

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.Stream

import scala.concurrent.duration.*
import java.util.Date

object InfiniteStream extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /** Generates an infinite stream that emits the String "Hello" and a timestamp every second.
    */
  val stream: Stream[IO, String] =
    Stream
      .eval(IO("Hello"))
      .repeat // Infinite stream of String "Hello" - Stream[IO, String]
      .zipWithIndex // Generate an index for each element - Stream[IO, (String,Long)]
      .map { case (msg, idx) => s"$msg - $idx" } // Map the tuple (String, Long) => String - Stream[IO, String]
      .zipLeft(
        Stream.awakeEvery[IO](1.seconds)
      ) // Stream.awakeEvery acts as a timer, so we emit the left stream every second
      .map { msg =>
        {
          val ts = new Date()
          s"$msg - $ts\n"
        } // Add a timestamp to the String
      }
      .evalTap(s => log.info(s)) // log each entry.

  /** Test our infinite stream by taking 10 entries (we drain the result as we are logging the output via evalTap).
    * @return
    *   Unit
    */

  override def run: IO[Unit] = stream.take(10).compile.drain

}
