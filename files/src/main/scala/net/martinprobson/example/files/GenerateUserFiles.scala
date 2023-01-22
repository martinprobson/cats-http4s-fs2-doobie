package net.martinprobson.example.files

import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
  * CATS IOApp that generates <code>numFiles</code> of files called <i>test_file_{index}.txt</i> in
  * the /tmp directory, each containing <code>numOfLines</code> of User objects encoded as Json.
  */
object GenerateUserFiles extends IOApp.Simple {
    implicit def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /**
    * Generate user files.
    * @param fileIndex The number of files to generate
    * @param numOfLines The number of Users per file
    */
    def generateUserFile(fileIndex: Int, numOfLines: Long): IO[Unit] =
        Stream
                .iterate(((fileIndex - 1) * numOfLines) + 1)(_ + 1)
                .map(n => User(n, s"User-$n"))
                .covary[IO]
                .take(numOfLines)
                .map(u => u.asJson.noSpaces)
                .evalTap(s => log.info(s"$fileIndex - $s"))
                .intersperse("\n")
                .through(text.utf8.encode)
                .through(Files[IO].writeAll(Path(s"${System.getProperty("java.io.tmpdir")}/test_file_$fileIndex.txt")))
                .compile
                .drain

  /**
    *
    * Generate user files. Each file is generated in parallel (using <code>parTraverse</code>.
    * @param numFiles The number of files to generate
    * @param numOfLines The number of Users per file
    */
    def generateUserFiles(numFiles: Int, numOfLines: Long): IO[Unit] =
      Range
        .inclusive(1,numFiles)
        .toList
        .parTraverse(fileIndex => generateUserFile(fileIndex, numOfLines)) >> log.info("Done")

  /**
    * Main entry point. Generate <code>numFiles</code> each with <code>numOfLines</code> Users.
    */
    override def run: IO[Unit] = generateUserFiles(10,1000)
}
