package net.martinprobson.example.server.db.repository

import cats.effect.testing.scalatest.AsyncIOSpec
import net.martinprobson.example.common.model.User
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class UserRepositoryTests extends AsyncFunSuite with AsyncIOSpec {

  test("countUsers <> 0") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1"))
      _ <- userRepository.addUser(User("User2"))
      _ <- userRepository.addUser(User("User3"))
      count <- userRepository.countUsers
    } yield count).asserting(_ shouldBe 3)
  }

  test("countUsers == 0") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      count <- userRepository.countUsers
    } yield count).asserting(_ shouldBe 0)
  }

  test("addUser/getUser") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      u <- userRepository.addUser(User("User1"))
      user <- userRepository.getUser(u.id)
    } yield user).asserting {
      case Some(User(_, "User1")) => assert(condition = true)
      case _                      => fail("Fail")
    }
  }

  test("addUsers") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1"))
      _ <- userRepository.addUser(User("User2"))
      _ <- userRepository.addUser(User("User3"))
      count <- userRepository.countUsers
    } yield count).asserting(_ shouldBe 3)
  }

  test("addUser/getUserByName") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1"))
      user <- userRepository.getUserByName("User1")
    } yield user).asserting {
      case List(User(_, "User1")) => assert(true)
      case _                      => fail("Fail")
    }
  }

  test("getUsers") {
    (for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1"))
      _ <- userRepository.addUser(User("User2"))
      _ <- userRepository.addUser(User("User3"))
      users <- userRepository.getUsers
    } yield users).asserting {
      case List(
      User(_, "User1"),
      User(_, "User2"),
      User(_, "User3")
      ) =>
        assert(true)
      case _ => fail("Fail")
    }
  }
}
