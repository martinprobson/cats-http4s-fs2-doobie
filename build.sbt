name := "CATS Effect http4s/fs2 with Doobie example"
ThisBuild / scalaVersion := "2.13.10"
//ThisBuild / scalaVersion := "3.2.0"
ThisBuild / version := "0.0.2-SNAPSHOT"
ThisBuild / organization := "net.martinprobson"

val Http4sVersion = "0.23.16"
val CirceVersion = "0.14.0"
val fs2Version = "3.3.0"
val LogbackVersion = "1.2.11"
val DoobieVersion = "1.0.0-RC1"
val ScalaTestVersion = "3.2.11"

val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "ch.qos.logback" % "logback-core" % LogbackVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-literal" % CirceVersion,
  "com.github.japgolly.clearconfig" %% "core" % "3.0.0",
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "org.scalatest" %% "scalatest-flatspec" % ScalaTestVersion % Test
)

lazy val root = project
  .in(file("."))
  .aggregate(common, client, server, files)
  .disablePlugins(AssemblyPlugin)
  .settings(Test / fork := true, run / fork := true)
  .settings(commonSettings)

lazy val common = project
  .in(file("common"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= commonDependencies)
  .disablePlugins(AssemblyPlugin)
  .settings(Test / fork := true, run / fork := true)

lazy val files = project
        .in(file("files"))
        .dependsOn(common)
        .settings(commonSettings)
        .settings(libraryDependencies ++=
                commonDependencies ++
                Seq("co.fs2" %% "fs2-core" % fs2Version,
                    "co.fs2" %% "fs2-io" % fs2Version,
                    "io.circe" %% "circe-core" % CirceVersion,
                    "io.circe" %% "circe-generic" % CirceVersion,
                    "io.circe" %% "circe-parser" % CirceVersion,
                    "io.circe" %% "circe-fs2" % CirceVersion,
                    "io.circe" %% "circe-literal" % CirceVersion)
        )
        .settings(Test / fork := true, run / fork := true)
        .settings(assemblySettings)

lazy val client = project
  .in(file("client"))
  .dependsOn(common,files)
  .settings(commonSettings)
  .settings(libraryDependencies ++=
    commonDependencies ++
    Seq("org.http4s" %% "http4s-ember-client" % Http4sVersion,
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "io.circe" %% "circe-literal" % CirceVersion)
  )
  .settings(Test / fork := true, run / fork := true)
  .settings(assemblySettings)

lazy val server = project
  .in(file("server"))
  .dependsOn(common)
  .settings(commonSettings)
  .settings(libraryDependencies ++=
    commonDependencies ++
    Seq("org.http4s" %% "http4s-ember-server" % Http4sVersion,
        "org.tpolecat" %% "doobie-core" % DoobieVersion,
        "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
        "mysql" % "mysql-connector-java" % "8.0.30",
        "com.h2database" % "h2" % "1.4.200",
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "io.circe" %% "circe-literal" % CirceVersion)
    )
  .settings(Test / fork := true, run / fork := true)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(assemblySettings)
  .settings(dockerExposedPorts := Seq(8085,8085))

lazy val compilerOptions = Seq(
  "-deprecation",         // Emit warning and location for usages of deprecated APIs.
  "-explaintypes",        // Explain type errors in more detail.
  "-Xsource:3",           // Warn for Scala 3 features
  "-Ymacro-annotations",  // For circe macros
  "-Xfatal-warnings",     // Fail the compilation if there are any warnings.
  "-encoding",
  "utf8"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions
)

lazy val assemblySettings = Seq(
  assembly / assemblyJarName := name.value + ".jar",
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case "application.conf"            => MergeStrategy.concat
    case "reference.conf"              => MergeStrategy.concat
    case "module-info.class"           => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)
