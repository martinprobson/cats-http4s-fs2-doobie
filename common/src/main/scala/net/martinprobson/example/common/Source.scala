package net.martinprobson.example.common

import cats.effect.IO
import fs2.Stream
import net.martinprobson.example.common.model.User

trait Source {
  def stream: Stream[IO, User]
}

case class MemorySource(private val size: Int) extends Source {
  def stream: Stream[IO, User] =
    Stream
      .iterate(1)(_ + 1)
      .map(n => User(n, s"User-$n", s"Email-$n"))
      .covary[IO]
      .take(size)
}
