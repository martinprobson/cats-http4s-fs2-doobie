package net.martinprobson.example.server

import cats.effect.IO
import net.martinprobson.example.common.config.Config
import org.http4s.{Headers, HttpApp, Response, Status}
import org.http4s.server.middleware.Throttle

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object RateLimit {

  /**
    * Send a throttle response back to the client if we have exceeded or capacity.
    * @param retryAfter hint to client to retry after x milliseconds. This is sent back in the response header
    * @return Response which will be sent back to the client
    */
  private def throttleResponse[IO[_]](retryAfter: Option[FiniteDuration]): Response[IO] =
    retryAfter match {
    case None => Response[IO](Status.TooManyRequests)
    case Some(duration) =>
      Response[IO](
        Status.TooManyRequests,
        headers = Headers("x-reset" -> duration.toMillis.toString)
      )
  }

  /**
    * Define a token bucket for our rate limiter, that refills with a configurable number of tokens every x milliseconds.
    */
  private val tokenBucket: IO[Throttle.TokenBucket[IO]] = for {
    config <- Config.loadConfig
    bucket <- Throttle.TokenBucket.local[IO](config.capacity,
      FiniteDuration(config.refillEvery,TimeUnit.MILLISECONDS))
  } yield bucket

  /**
    * This is the rate limiter HttpApp that we can wrap our original HttpApp with to provide rate limiting.
    * @param httpApp The http app that we want to rate limit
    * @return The original http app wrapped with out rate limiter
    */
  def throttle(httpApp: HttpApp[IO]): IO[HttpApp[IO]] = for {
    bucket <- tokenBucket
    throttle = Throttle.httpApp[IO](bucket, throttleResponse[IO] _)(httpApp)
  } yield throttle

}
