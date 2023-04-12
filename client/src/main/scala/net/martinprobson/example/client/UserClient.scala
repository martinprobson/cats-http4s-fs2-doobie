package net.martinprobson.example.client

//TODO Get rid of Circe and replace with another (Scala 3 compatible) Json library
//TODO Translate to scala3
import cats.effect.{IO, IOApp}
import org.http4s.circe.*
import fs2.Stream
import io.circe.generic.auto.*
import net.martinprobson.example.common.MemorySource
import net.martinprobson.example.common.config.Config
import net.martinprobson.example.common.config.Config.config
import net.martinprobson.example.common.model.User
import net.martinprobson.example.files.{ErrorFileWriter, FileSource, GenerateUserFiles}
import org.http4s.{Method, Request}
import org.http4s.client.Client
import org.http4s.ember.client.*
import org.http4s.implicits.http4sLiteralsSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
object UserClient extends IOApp.Simple {

  private def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  /**
    * Given a client and a source stream of Users, call the [[postUsers]] method to post them
    * to the server and process the results.
    * <p>Any errors in the result stream are written out to two files, one containing the
    * failed Users (in Json format) and the other containing the error message and user.</p>
    * @param client The client used to execute the post .
    * @param source A source stream of users.
    */
  def userClient(client: Client[IO], source: Stream[IO, User]): IO[Unit] = {
    postUsers(client, source)
      .filter(_.isLeft)
      .evalTap(e => log.error(e.toString))
      .map {
        case Left((e, u)) => (e,u)
        case Right(u) => ("",u)
      }
      .broadcastThrough(ErrorFileWriter.write, ErrorFileWriter.writeErrorMsg)
      .compile
      .drain
  }

  /**
    * Build an EmberClient (with an attached [[RateLimitRetry]] policy)
    * and pass it to the [[userClient]] method.
    * @param source The source stream of Users
    */
  def program(source: Stream[IO, User]): IO[Unit] = {
    EmberClientBuilder
      .default[IO]
      .withRetryPolicy(RateLimitRetry.retry)
      .withLogger(log)
      .build
      .onFinalize(log.info("Shutdown of EmberClient"))
      .use( client => userClient(client, source) )
  }

  /**
    * Main entry point for our client program.
    * <ol>
    *   <li>Generate some files containing Json encoded [[net.martinprobson.example.common.model.User]]'s</li>
    *   <li>Call our program with the file source.</li>
    *   </ol>
    */
  //override def run: IO[Unit] =
  //  GenerateUserFiles.generateUserFiles(3, 10, config.directory, config.filenamePrefix) >>
  //  program(FileSource.stream)

  /**
    * Main entry point for out client program, call our program with an in memory generated stream of Users
    */
  override def run: IO[Unit] = program(MemorySource(1000000).stream)

  private def postUser(user: User, client: Client[IO]): IO[Either[(String,User),User]] = {
    def req(user: User): Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
      .withEntity(user)
    log.info(s"call $user") >>
        client.expect(req(user))(jsonOf[IO, User]).map(u => Right(u)).handleError(e => Left((e.toString,user)))
  }

  /**
    * Given a [[net.martinprobson.example.common.Source]] stream of [[net.martinprobson.example.common.model.User]]'s,
    * call [[postUser]] to post each User to the server end-point in parallel.
    * @param client A client that will handle the post call.
    * @param source The source stream of Users.
    * @return A result stream containing an Either giving the result of each post call.
    */
  private def postUsers(client: Client[IO], source: Stream[IO, User]): Stream[IO, Either[(String,User),User]] = for {
    c <- Stream(client)
    result <- source
      .parEvalMapUnorderedUnbounded(user => postUser(user, c))
  } yield result
}
