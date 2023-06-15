package net.martinprobson.example.common.config

import cats.Id
import cats.implicits.*
import japgolly.clearconfig.*

final case class Config(
  threads: Int,
  driverClassName: String,
  url: String,
  user: String,
  password: String,
  capacity: Int,
  refillEvery: Long,
  directory: String,
  filenamePrefix: String,
  userErrorFilename: String,
  userErrorMsgFilename: String
)

object Config extends App {

  private def configSources: ConfigSources[Id] =
    ConfigSource.environment[Id] >
      ConfigSource.propFileOnClasspath[Id]("/application.properties", optional = false) >
      ConfigSource.system[Id]

  private def cfg: ConfigDef[Config] = (
    ConfigDef.need[Int]("threads"),
    ConfigDef.need[String]("driverClassName"),
    ConfigDef.need[String]("url"),
    ConfigDef.need[String]("user"),
    ConfigDef.need[String]("password"),
    ConfigDef.need[Int]("capacity"),
    ConfigDef.need[Long]("refill_every"),
    ConfigDef.getOrUse("directory",System.getProperty("java.io.tmpdir")),
    ConfigDef.getOrUse("filename_prefix","test"),
    ConfigDef.need[String]("user_error_filename"),
    ConfigDef.need[String]("user_error_msg_filename")
  ).mapN(apply)

  lazy val config: Config = cfg.run(configSources).getOrDie()

  private val (_, report) = cfg.withReport.run(configSources).getOrDie()
  println(report.mapUnused(_.withoutSources(ConfigSourceName.system, ConfigSourceName.environment)).full)
}
