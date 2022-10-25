package net.martinprobson.example.common.config

import cats.effect.IO
import cats.implicits.*
import japgolly.clearconfig.*

final case class Config(
  threads: Int,
  driverClassName: String,
  url: String,
  user: String,
  password: String,
  capacity: Int,
  refillEvery: Long
)

object Config {

  private def configSources: ConfigSources[IO] =
    ConfigSource.environment[IO] >
      ConfigSource.propFileOnClasspath[IO]("/application.properties", optional = false) >
      ConfigSource.system[IO]

  private def cfg: ConfigDef[Config] = (
    ConfigDef.need[Int]("threads"),
    ConfigDef.need[String]("driverClassName"),
    ConfigDef.need[String]("url"),
    ConfigDef.need[String]("user"),
    ConfigDef.need[String]("password"),
    ConfigDef.need[Int]("capacity"),
    ConfigDef.need[Long]("refill_every")
  ).mapN(apply)

  val loadConfig: IO[Config] = Config.cfg.run(configSources).flatMap(r => IO(r.getOrDie()))
}
