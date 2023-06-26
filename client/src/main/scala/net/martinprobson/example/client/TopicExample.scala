package net.martinprobson.example.client

import cats.effect.*
import fs2.Stream
import fs2.concurrent.Topic
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

object TopicExample extends IOApp.Simple {
  implicit def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val program: IO[Vector[Int]] = Topic[IO, Int].flatMap { topic =>
    val publisher = Stream
      .iterate(0)(_ + 1)
      .covary[IO]
      .through(topic.publish)
    //val subscriber2 = topic.subscribe(10).take(10).evalTap(i => log.info(s"subscriber2 $i"))
    val subscriber = topic
      .subscribe(10)
      .evalTap(i => log.info(s"subscriber1 $i"))
      .zipLeft(Stream.awakeEvery[IO](1.seconds))
      .interruptAfter(20.seconds)
    val subscriber2 = topic
      .subscribe(1)
      .evalTap(i => log.info(s"subscriber2 $i"))
      .zipLeft(Stream.awakeEvery[IO](0.5.seconds))
      .interruptAfter(1.seconds)
    subscriber.concurrently(publisher).concurrently(subscriber2).compile.toVector
  }

  def run: IO[Unit] = program.flatMap(result => log.info(s"result = $result"))
}
