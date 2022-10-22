package net.martinprobson.example.server

import cats.effect.{IO, IOApp, Resource}
import doobie.hikari.HikariTransactor
import doobie.{ExecutionContexts, Transactor}
import com.comcast.ip4s.*
import org.http4s.circe.{jsonEncoder, jsonEncoderOf, jsonOf}
import org.http4s.dsl.io.*
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Headers,
  HttpRoutes,
  Request,
  Response,
  Status
}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import net.martinprobson.example.server.db.repository.{DoobieUserRepository, UserRepository}
import net.martinprobson.example.common.model.User
import net.martinprobson.example.server.db.config.Config
import org.http4s.server.middleware.Throttle
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

object Server extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  implicit val userEncoder: EntityEncoder[IO, User] = jsonEncoderOf[IO, User]

  implicit val usersEncoder: EntityEncoder[IO, List[User]] = jsonEncoderOf[IO, List[User]]

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

  def postUser(request: Request[IO])(userRepository: IO[UserRepository]): IO[User] = for {
    user <- request.as[User]
    _ <- log.info(s"Got User: $user")
    dbUser <- userRepository.flatMap(_.addUser(user))
    _ <- log.info(s"Added User: $dbUser to Db")
  } yield dbUser

  def getUser(id: Long)(userRepository: IO[UserRepository]): IO[Option[User]] = for {
    _ <- log.info(s"In getUser: $id")
    u <- userRepository.flatMap(_.getUser(id))
    _ <- log.info(s"Found: $u")
  } yield u

  def getUsers(userRepository: IO[UserRepository]): IO[List[User]] = for {
    _ <- log.info(s"In getUsers")
    users <- userRepository.flatMap(_.getUsers)
    _ <- log.info(s"Got $users")
  } yield users

  def userService(userRepository: IO[UserRepository]): HttpRoutes[IO] = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "user" =>
        postUser(req)(userRepository).flatMap(u => Ok(u.asJson))
      case GET -> Root / "users" =>
        getUsers(userRepository).flatMap(u => Ok(u.asJson))
      case GET -> Root / "user" / LongVar(id) =>
        getUser(id)(userRepository).flatMap {
          case Some(user) => Ok(user.asJson)
          case None       => NotFound()
        }
      case GET -> Root / "hello" =>
        log.info("In hello world!") >> Ok("Hello world!")
    }

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is
    * lifted into a Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = transactor.use { xa =>
    program(xa).flatMap(_ => log.info("Program exit"))
  }

  /** Setup a HikariTransactor connection pool.
    * @return
    *   A Resource containing a HikariTransactor.
    */
  private val transactor: Resource[IO, HikariTransactor[IO]] =
    (for {
      _ <- Resource.eval[IO, Unit](log.info("Setting up transactor"))
      cfg <- Resource.eval[IO, Config](Config.loadConfig)
      ce <- ExecutionContexts.fixedThreadPool[IO](cfg.threads)
      xa <- HikariTransactor
        .newHikariTransactor[IO](cfg.driverClassName, cfg.url, cfg.user, cfg.password, ce)
    } yield xa).onFinalize(log.info("Finalize of transactor"))

  def throttleResponse[IO[_]](retryAfter: Option[FiniteDuration]): Response[IO] = retryAfter match {
    case None => Response[IO](Status.TooManyRequests)
    case Some(duration) =>
      Response[IO](
        Status.TooManyRequests,
        headers = Headers("x-reset" -> duration.toSeconds.toString)
      )
  }

  private def program(xa: Transactor[IO]): IO[Unit] = for {
    _ <- log.info("Program starting ....")
    //userRepository <- InMemoryUserRepository.empty
    userRepository <- DoobieUserRepository(xa)
    bucket <- Throttle.TokenBucket.local[IO](5, 20.seconds)
    throttle = Throttle.httpApp[IO](bucket, throttleResponse[IO] _)(
      userService(IO(userRepository)).orNotFound
    )
    _ <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8085")
      .withHttpApp(throttle)
      .withShutdownTimeout(10.seconds)
      .withLogger(log)
      .build
      .onFinalize(log.info("Shutdown of EmberServer"))
      .use(_ => IO.never)
  } yield ()
}
