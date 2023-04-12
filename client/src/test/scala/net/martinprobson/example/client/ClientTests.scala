package net.martinprobson.example.client

import cats.effect.IO
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import net.martinprobson.example.common.model.User
import net.martinprobson.example.common.MemorySource
import org.http4s.implicits.*
import org.http4s.*
import org.http4s.client.Client
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

object ClientTests extends SimpleIOSuite {

  def log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

//  def getResponse(request: Request[IO]): IO[Response[IO]] =
//    FlakyServer.httpRoutes.orNotFound.run(request)

  test("Flaky Server") {
    val httpApp: HttpApp[IO] = FlakyServer.httpRoutes.orNotFound
    val client: Client[IO] = Client.fromHttpApp(httpApp)
    for {
      _ <- UserClient.userClient(client, ErrorSource(10000).stream.interleaveAll(MemorySource(20000).stream))
    } yield success
  }
}
