package net.martinprobson.example.files

import cats.effect.IO
import fs2.{Pipe, text}
import fs2.io.file.{Files, Path}
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.config.Config.config
import net.martinprobson.example.common.model.User
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps

object ErrorFileWriter {
  implicit def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  def write: Pipe[IO, (String,User), Nothing] = {
    _.map( u => u._2.asJson.noSpaces)
    .evalTap(u => log.info(s"Sending $u to error file - ${config.userErrorFilename}"))
    .intersperse("\n")
    .through(text.utf8.encode)
    .through(Files[IO].writeAll(Path(s"${config.directory}/${config.userErrorFilename}")))
  }

  def writeErrorMsg: Pipe[IO, (String, User), Nothing] = {
    _.map(u => s"${u._1}|${u._2.asJson.noSpaces}")
      .evalTap(u => log.info(s"Sending $u and error msg to error file - ${config.userErrorMsgFilename}"))
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path(s"${config.directory}/${config.userErrorMsgFilename}")))
  }

}
