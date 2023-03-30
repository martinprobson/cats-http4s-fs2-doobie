package net.martinprobson.example.client

import cats.effect.IO
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import org.http4s.implicits.*
import org.http4s.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

object ClientTests extends SimpleIOSuite {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def getResponse(request: Request[IO]): IO[Response[IO]] =
    FlakyServer.httpRoutes2.orNotFound.run(request)

  test("Dummy") {
    log.info("Hello") >>
    IO.pure(0).map{ v => expect(v == 0)}
  }

  test("PostUser") {
    val request: Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
      .withEntity(User(User.UNASSIGNED_USER_ID, "TestName", "TestEmail"))
    for {
      resp <- getResponse(request)
      _ <- expect(resp.status == Status.Ok).failFast
      body <- resp.bodyText.compile.toList
      _ <- expect(body.head == User(0, "TestName", "TestEmail").asJson.noSpaces).failFast
    } yield success
  }

}
