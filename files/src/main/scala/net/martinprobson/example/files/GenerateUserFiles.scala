package net.martinprobson.example.files

import cats.effect.implicits.concurrentParTraverseOps
import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object GenerateUserFiles extends IOApp.Simple {
    implicit def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    def generateUserFile(fileIndex: Int, noOfLines: Long): IO[Unit] =
        Stream
                .iterate(((fileIndex - 1) * noOfLines) + 1)(_ + 1)
                .map(n => User(n, s"User-$n"))
                .covary[IO]
                .take(noOfLines)
                .map(u => u.asJson.noSpaces)
                .evalTap(s => log.info(s"$fileIndex - $s"))
                .intersperse("\n")
                .through(text.utf8.encode)
                .through(Files[IO].writeAll(Path(s"/tmp/test_file_$fileIndex.txt")))
                .compile
                .drain

    def generateUserFiles(noFiles: Int, noOfLines: Long): IO[Unit] =
      Range
        .inclusive(1,noFiles)
        .toList
        .parTraverseN(10)(fileIndex => generateUserFile(fileIndex, noOfLines)) >> log.info("Done")

    override def run: IO[Unit] = generateUserFiles(1,1)
}
