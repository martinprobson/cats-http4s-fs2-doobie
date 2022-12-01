
# CATS, FS2, http4s and Doobie Example

## Overview
This example project shows how the following Scala functional libraries fit together: -

| Library                                           | Description                                   |
|---------------------------------------------------|-----------------------------------------------|
| [CATS](https://typelevel.org/cats/)               | Functional programming abstractions in Scala  |
| [CATS Effect](https://typelevel.org/cats-effect/) | Pure asynchronous runtime for Scala           |
| [fs2](https://fs2.io/#/)                          | Functional streams for Scala                  |
| [http4s](https://http4s.org)                      | Typeful, functional, streaming http for Scala |
| [Doobie](https://tpolecat.github.io/doobie/)      | Functional JDBC layer for Scala               |
| [Circe](https://circe.github.io/circe/)           | A Json library for Scala powered by CATS      |

The project is configured as separate sbt sub-projects as follows: -

| Project | Description                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| root    | The root project that aggregates the separate sub-projects below                                                                                         |
| common  | Common code shared between all sub-projects                                                                                                              |
| Files   | Use fs2 streaming, CATS effect, Circe and the fs2 io packages to generate and read files of Json objects                                                 |
| Server  | Simple http4s server that accepts requests to post/get/stream `User` objects to a backend database (either in memory db, or JDBC (via Doobie DB library) |
| Client  | http4s client that uses files project to read `User` objects and posts them to the server endpoint(s) above                                              |

The structure and main classes of each sub-project are described below.

## Files Project

### GenerateUserFiles
[GenerateUserFiles](files/src/main/scala/net/martinprobson/example/files/GenerateUserFiles.scala) 
 uses fs2 streaming to generate x files of n 
 [User](common/src/main/scala/net/martinprobson/example/common/model/User.scala) 
 objects (a `User` object is just a simple case class of ids and names).

The number of files and number of user objects to generate per file is controlled by the `numFiles` and `numOfLines` parameters to the main `generateUserFiles` method.

### ReadUserFiles
[ReadUserFiles](files/src/main/scala/net/martinprobson/example/files/ReadUserFiles.scala)  uses fs2 streaming to read the 
generated files into a fs2 stream of `Stream[IO,User]`

The User objects are encoded a Json objects using [Circe](https://circe.github.io/circe/).

## Server Project
[Server](server/src/main/scala/net/martinprobson/example/server/Server.scala) builds a http4s server with a `UserService` that responds to the following endpoints: -

| Http Method | Endpoint    | Description                                                                        |
|-------------|-------------|------------------------------------------------------------------------------------|
| GET         | /users      | Return a list of all users                                                         |
| GET         | /user/{id}  | Return user defined by id or Http status 404 if not found                          |
| GET         | /userstream | Return a fs2 Stream of all users                                                   |
| GET         | /hello      | Return the string "Hello world!"                                                   |
| GET         | /seconds    | Return an infinite fs2 Stream of the string "Hello" and a timestamp every 1 second |
| POST        | /user       | Post the user defined in the request to the user repository                        |

### UserRepository
The [UserRepository](server/src/main/scala/net/martinprobson/example/server/db/repository/UserRepository.scala) defines the interface
methods for the user repository. This interface is implemented by: -
* [InMemoryUserRepository](server/src/main/scala/net/martinprobson/example/server/db/repository/InMemoryUserRepository.scala) - Holds the users in a simple in memory Map.
* [DoobieUserRepository](server/src/main/scala/net/martinprobson/example/server/db/repository/InMemoryUserRepository.scala) - Uses [Doobie](https://tpolecat.github.io/doobie/) to implement the repository with JDBC. The JDBC connection parameters should be defined in the [config properties file](common/src/main/resources/application.properties).

### Rate Limiter
The server projet also implements a simple, token bucket-based [rate limiter](server/src/main/scala/net/martinprobson/example/server/RateLimit.scala).

## Client Project
[MyClient](client/src/main/scala/net/martinprobson/example/client/MyClient.scala) builds a http4s client that uses the Files project to read in the file(s) of user objects and posts them to the server (in parallel). It then uses [StreamingUserClient](client/src/main/scala/net/martinprobson/example/client/StreamingUserClient.scala) to stream them back out again.

The [RateLimitRetry](client/src/main/scala/net/martinprobson/example/client/RateLimitRetry.scala)  object handles the retry logic if a 429 response if returned from the server.

## Common Project
Holds common code shared between projects above, such as configuration and the user object.
