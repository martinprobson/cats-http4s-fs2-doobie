package net.martinprobson.example.common

import cats.effect.IO
import fs2.Stream
import net.martinprobson.example.common.model.User

/** A Source capable of generating a Stream of User objects.
  */
trait Source {
  def stream: Stream[IO, User]
}

/** Generate an (in memory) stream of Users with a given size.
  *
  * @param size
  *   The number of user objects to generate.
  */
case class MemorySource(private val size: Int) extends Source {
  def stream: Stream[IO, User] =
    Stream
      .iterate(1)(_ + 1)
      .map(n => User(n, s"User-$n", s"Email-$n"))
      .covary[IO]
      .take(size)
}
