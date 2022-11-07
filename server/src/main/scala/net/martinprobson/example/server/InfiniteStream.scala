package net.martinprobson.example.server

import cats.effect.IO
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.Stream

import scala.concurrent.duration.*
import java.util.Date

object InfiniteStream {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val stream: Stream[IO, String] =
    Stream
      .eval(IO("Hello"))
      .repeat
      .zipWithIndex
      .map { case (msg, idx) => s"$msg - $idx" }
      .zipLeft(Stream.awakeEvery[IO](1.seconds))
      .map { msg =>
        {
          val ts = new Date()
          s"$msg - $ts\n"
        }
      }
      .evalTap(s => log.info(s))
}
