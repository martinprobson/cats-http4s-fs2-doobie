package net.martinprobson.example.server

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import net.martinprobson.example.server.db.repository.InMemoryUserRepository
import org.http4s.{Method, Request}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import net.martinprobson.example.server.Server.userService
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.implicits.http4sLiteralsSyntax

class ServerTests extends AsyncFunSuite with AsyncIOSpec {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def getResponse(request: Request[IO]): IO[Response[IO]] = {
    InMemoryUserRepository.empty.flatMap { ur =>
      val service: HttpRoutes[IO] = userService(ur)
      service.orNotFound.run(request)
    }
  }

  test("Invalid URL") {
    val request: Request[IO] = Request[IO](Method.GET, uri"/")
    getResponse(request).asserting(resp => resp.status shouldBe Status.NotFound)
  }

  test("Hello response") {
    val request: Request[IO] = Request[IO](Method.GET, uri"/hello")
    val response = getResponse(request)
    response.asserting(resp => resp.status shouldBe Status.Ok)
    response.flatMap(resp => resp.bodyText.compile.toList.asserting(_.shouldBe(List("Hello world!"))))
  }

  test("PostUser") {
    val request: Request[IO] = Request[IO](method = Method.POST, uri"http://localhost:8085/user")
      .withEntity(User(User.UNASSIGNED_USER_ID,"Test"))
    val response = getResponse(request)
    response.asserting(resp => resp.status shouldBe Status.Ok)
    response.flatMap(resp => resp.bodyText.compile.toList.asserting(l => l.head.shouldBe(User(1,"Test").asJson.noSpaces)))
  }
}
