package net.martinprobson.example.files

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Flags, Path}
import fs2.{Stream, text}
import io.circe.fs2.{decoder, stringStreamParser}
import io.circe.generic.decoding.DerivedDecoder.deriveDecoder
import net.martinprobson.example.common.model.User
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ReadUserFiles extends IOApp.Simple {

  implicit def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /**
    * Read a collection of files containing User Json objects into a Stream of [IO,User], decoding the Json
    * into User classes on the way through.
    */
  val reader: Stream[IO, User] = Files[IO]
    .walk(Path(System.getProperty("java.io.tmpdir")), maxDepth = 1, followLinks = false)
    .filter(p => p.fileName.toString.startsWith("test_file_"))
    .map { path =>
      Files[IO]
        .readAll(path, 1000, Flags.Read)
        .through(text.utf8.decode)
        .through(stringStreamParser)
        .through(decoder[IO, User])
        .evalTap(user => log.info(s"user = $user"))
    }
    .parJoinUnbounded
    //or use parJoin(n) to limit concurrency

  /**
    * Test - read the files and count the total number of Users.
    */
  def run: IO[Unit] = reader.compile.count.flatMap(c => log.info(s"Total count = $c"))

}
