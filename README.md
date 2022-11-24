
# CATS, FS2, http4s and Doobie Example

## Overview
This example project shows how the following Scala functional frameworks fit together: -

| Framework                                         | Description                                   |
|---------------------------------------------------|-----------------------------------------------|
| [CATS](https://typelevel.org/cats/)               | Functional programming abstractions in Scala  |
| [CATS Effect](https://typelevel.org/cats-effect/) | Pure asynchronous runtime for Scala           |
| [fs2](https://fs2.io/#/)                          | Functional streams for Scala                  |
| [http4s](https://http4s.org)                      | Typeful, functional, streaming http for Scala |
| [Doobie](https://tpolecat.github.io/doobie/)      | Functional JDBC layer for Scala               |

## Files Module

### GenerateUserFiles
The [GenerateUserFiles](files/src/main/scala/net/martinprobson/example/files/GenerateUserFiles.scala) 
object uses fs2 streaming to generate x files of n 
 [User](common/src/main/scala/net/martinprobson/example/common/model/User.scala) 
 objects (a `User` object is just a simple case class of ids and names).

The number of files and number of user objects to generate per file is controlled by the `numFiles` and `numOfLines` parameters to the main `generateUserFiles` method.

### ReadUserFiles
The [ReadUserFiles](files/src/main/scala/net/martinprobson/example/files/ReadUserFiles.scala) object uses fs2 streaming to read the 
generated files into a fs2 stream of `Stream[IO,User]`

The User objects are encoded a Json objects using [Circe](https://circe.github.io/circe/).
2. The stream of `User` objects is posted to a http endpoint which saves them to either an InMemory, h2 or mysql database.
3. The `users` are read out of the database again and output to a log.

## Server Module

## Client Module

## Common Module
## Modules
Describe each of the modules and the main objects
This archive contains a template Sbt project for a Scala application. It includes [Scala Style](http://www.scalastyle.org/)
and [Scala format](https://scalameta.org/scalafmt/) configuration as well as the [Type safe configuration library](https://github.com/lightbend/config)
and [Logback logging](https://logback.qos.ch/).


TODO
Introduction with purpose
Describe every module
SQL backend / InMemory backend
