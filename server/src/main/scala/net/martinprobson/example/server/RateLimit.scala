package net.martinprobson.example.server

import cats.effect.IO
import net.martinprobson.example.common.config.Config
import org.http4s.{Headers, HttpApp, Response, Status}
import org.http4s.server.middleware.Throttle

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object RateLimit {

  private def throttleResponse[IO[_]](retryAfter: Option[FiniteDuration]): Response[IO] =
    retryAfter match {
    case None => Response[IO](Status.TooManyRequests)
    case Some(duration) =>
      Response[IO](
        Status.TooManyRequests,
        headers = Headers("x-reset" -> duration.toMillis.toString)
      )
  }

  private val tokenBucket: IO[Throttle.TokenBucket[IO]] = for {
    config <- Config.loadConfig
    bucket <- Throttle.TokenBucket.local[IO](config.capacity,
      FiniteDuration(config.refillEvery,TimeUnit.MILLISECONDS))
  } yield bucket

  def throttle(httpApp: HttpApp[IO]): IO[HttpApp[IO]] = for {
    bucket <- tokenBucket
    throttle = Throttle.httpApp[IO](bucket, throttleResponse[IO] _)(httpApp)
  } yield throttle

}
