package net.martinprobson.example.server

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
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

  test("Invalid URL") {
    val service: HttpRoutes[IO] = userService(InMemoryUserRepository.empty)
    val request: Request[IO] = Request[IO](Method.GET, uri"/")
    val result: IO[Response[IO]] = service.orNotFound.run(request)
    result.asserting(resp => resp.status shouldBe Status.NotFound)
  }

  test("Hello response") {
    val service: HttpRoutes[IO] = userService(InMemoryUserRepository.empty)
    val request: Request[IO] = Request[IO](Method.GET, uri"/hello")
    val result: IO[Response[IO]] = service.orNotFound.run(request)
    result.asserting(resp => resp.status shouldBe Status.Ok)
    //FIXME There must be a better way of doing this!!!
    result.flatMap(resp => resp.bodyText.compile.toList.asserting(_.shouldBe(List("Hello world!"))))
  }
}
