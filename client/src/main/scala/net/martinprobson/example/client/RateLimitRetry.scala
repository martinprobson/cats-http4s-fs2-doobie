package net.martinprobson.example.client

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import fs2.io.ClosedChannelException
import org.http4s.{Response, Status}
import org.http4s.client.middleware.RetryPolicy
import org.typelevel.ci.CIString
import org.slf4j.{Logger, LoggerFactory}

import java.io.IOException
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object RateLimitRetry {

  val log: Logger = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))

  def retry: RetryPolicy[IO] = { (_, result, retries) =>
      val (retry, duration) = isThrottleResponseAndDuration(result)
      if ((retries <= 100) && retry) {
        val backoff = duration * retries
        log.warn(s"In retry logic - waiting for $backoff - retries = $retries ")
        backoff.some
        //duration.some
      }
      else {
        None
      }
  }

  def isThrottleResponseAndDuration(result: Either[Throwable, Response[IO]]):
  (Boolean, FiniteDuration) = {
    val defaultDuration: FiniteDuration = FiniteDuration(0,TimeUnit.MILLISECONDS)
    result match {
      case Right(response) => if (response.status == Status.TooManyRequests) {
        val duration = response.headers.get(CIString("x-reset")) match {
          case Some(nel) => nel.head.value
          case None => "60000"
        }
        (true, FiniteDuration(duration.toLong,TimeUnit.MILLISECONDS))
      } else {
        (false,defaultDuration)
      }
      case Left(_: ClosedChannelException) => (true,defaultDuration)
      case Left(ex: IOException) =>
        val msg = ex.getMessage
        ( msg == "Connection reset by peer" || msg == "Broken pipe",defaultDuration)
      case _ => (false,defaultDuration)
    }
  }
}
