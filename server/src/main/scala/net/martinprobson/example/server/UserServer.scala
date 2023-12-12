package net.martinprobson.example.server

import cats.effect.{IO, IOApp}
import doobie.Transactor
import com.comcast.ip4s.*
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import net.martinprobson.example.server.db.repository.{
  DBTransactor,
  DoobieUserRepository,
  UserRepository
}
import net.martinprobson.example.common.model.User
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

object UserServer extends IOApp.Simple {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /** Post the user (defined in the Request) to the user repository
    * @param request
    *   The request containing the user to be posted
    * @param userRepository
    *   The repository holding user objects.
    * @return
    *   The user object that has been posted (wrapped in an IO)
    */
  def postUser(request: Request[IO])(userRepository: UserRepository): IO[User] = for {
    user <- request.as[User]
    _ <- log.debug(s"Got User: $user")
    dbUser <- userRepository.addUser(user)
    _ <- log.debug(s"Added User: $dbUser to Db")
  } yield dbUser

  /** Get an individual users by id or Option.None if the user does not exist
    * @param userRepository
    *   The repository holding user objects.
    * @return
    *   An Option of User wrapped in an IO
    */
  def getUser(id: Long)(userRepository: UserRepository): IO[Option[User]] = for {
    _ <- log.info(s"In getUser: $id")
    u <- userRepository.getUser(id)
    _ <- log.info(s"Found: $u")
  } yield u

  /**
   * Delete a user with the given id.
   * @param id The user id to delete
   * @param userRepository The repository holding user objects
   */
  def deleteUser(id: Long)(userRepository: UserRepository): IO[Int] = for {
    _ <- log.info(s"In deleteUser: $id")
    response <- userRepository.deleteUser(id)
    _ <- log.info(s"Deleted: $id")
  } yield response

  /** List all users defined in the repository
    * @param userRepository
    *   The repository holding user objects.
    * @return
    *   A list of user objects wrapped in an IO.
    */
  def getUsers(userRepository: UserRepository): IO[List[User]] = for {
    _ <- log.info("In getUsers")
    users <- userRepository.getUsers
    _ <- log.info(s"Got $users")
  } yield users

  def getUsersPaged(pageNo: Int, pageSize: Int, userRepository: UserRepository): IO[List[User]] = for {
    _ <- log.info(s"In getUsersPaged pageNo = $pageNo, pageSize = $pageSize")
    users <- userRepository.getUserPaged(pageNo, pageSize)
    _ <- log.info(s"Got $users")
  } yield users

  /** Stream all users defined in the repository
    * @param userRepository
    *   The repository holding user objects.
    * @return
    *   A stream of user objects wrapped in an IO.
   */
  def getUsersStream(userRepository: UserRepository): Stream[IO, User] =
    Stream.eval(log.info("getUsersStream")) >> userRepository.getUsersStream

  /** Count the number of users in the repository
    *
    * @param userRepository
    *   A user repository object used to store/fetch user objects from a db
    * @return
    *   The total count of users in the repository
    */
  def countUsers(userRepository: UserRepository): IO[Long] = for {
    _ <- log.info("In countUsers")
    count <- userRepository.countUsers
    _ <- log.info(s"Got count of $count users")
  } yield count

  /** Define a user service that reponds to the defined http methods and endpoints.
    * @param userRepository
    *   A user repository object used to store/fetch user objects from a db
    * @return
    *   An HttpRoute defining our user service.
    */
  def userService(userRepository: UserRepository): HttpRoutes[IO] = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "user" =>
        postUser(req)(userRepository).flatMap(u => Ok(u.asJson))
      case GET -> Root / "users" =>
        getUsers(userRepository).flatMap(u => Ok(u.asJson))
      case GET -> Root / "users" / "paged" / IntVar(pageNo) / IntVar(pageSize) =>
        getUsersPaged(pageNo, pageSize, userRepository).flatMap(u => Ok(u.asJson))
      case GET -> Root / "users" / "count" =>
        countUsers(userRepository).flatMap(c => Ok(c.asJson))
      case GET -> Root / "user" / LongVar(id) =>
        getUser(id)(userRepository).flatMap {
          case Some(user) => Ok(user.asJson)
          case None       => NotFound()
        }
      case DELETE -> Root / "user" / LongVar(id) =>
        deleteUser(id)(userRepository).flatMap {
          case 0 => NotFound()
          case _ => Ok()
        }
      case GET -> Root / "usersstream" =>
        Ok(getUsersStream(userRepository))
      case GET -> Root / "hello" =>
        log.info("In hello world!") >> Ok("Hello world!")
      case GET -> Root / "seconds" =>
        log.info("seconds") >> Ok(InfiniteStream.stream.take(10))
    }

  /** This is our main entry point where the code will actually get executed.
    *
    * We provide a transactor which will be used by Doobie to execute the SQL statements. Config is lifted into a
    * Resource so that it can be used to setup the connection pool.
    */
  override def run: IO[Unit] = DBTransactor.transactor.use { xa =>
    program(xa).flatMap(_ => log.info("Program exit"))
  }

  /** Start an Ember server to run our Http App.<p> We provide a transactor which will be used by Doobie to execute the
    * SQL statements. Config is lifted into a Resource so that it can be used to setup the connection pool.</p>
    */
  private def program(xa: Transactor[IO]): IO[Unit] = for {
    _ <- log.info("Program starting ....")
    //userRepository <- InMemoryUserRepository.empty
    userRepository <- DoobieUserRepository(xa)
    rateLimit <- RateLimit.throttle(userService(userRepository).orNotFound)
    corsServer <- IO(CORS.policy.withAllowOriginAll(userService(userRepository).orNotFound))
    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8085")
      // uncomment line below to remove rate limiter.
      //.withHttpApp(rateLimit)
      //.withHttpApp(userService(userRepository).orNotFound)
      .withHttpApp(corsServer)
      .withShutdownTimeout(10.seconds)
      .withLogger(log)
      .build
      .onFinalize(log.info("Shutdown of EmberServer"))
      .use(_ => IO.never)
//      .start
//    _ <- IO.println("Server started on port 8085")
//    _ <- IO.println("Press enter to stop the server...")
//    _ <- IO.consoleForIO.readLine
//    _ <- server.cancel
  } yield ()

}
