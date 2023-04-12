package net.martinprobson.example.client

import cats.effect.IO
import fs2.Stream
import net.martinprobson.example.common.Source
import net.martinprobson.example.common.model.User

case class ErrorSource(private val size: Int) extends Source {
  def stream: Stream[IO, User] =
    Stream
      .iterate(1)(_ + 1)
      .map(n => User(n, s"fail-$n", s"Email-$n"))
      .covary[IO]
      .take(size)
}
