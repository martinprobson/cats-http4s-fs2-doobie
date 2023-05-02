package net.martinprobson.example.server.db.repository

import weaver.SimpleIOSuite
import net.martinprobson.example.common.model.User

object UserRepositoryTests extends SimpleIOSuite {

  test("countUsers <> 0") {
    for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1", "Email1"))
      _ <- userRepository.addUser(User("User2", "Email2"))
      _ <- userRepository.addUser(User("User3", "Email3"))
      count <- userRepository.countUsers
    } yield expect(count == 3)
  }

  test("countUsers == 0") {
    for {
      userRepository <- InMemoryUserRepository.empty
      count <- userRepository.countUsers
    } yield expect(count == 0)
  }

  test("addUser/getUser") {
    for {
      userRepository <- InMemoryUserRepository.empty
      u <- userRepository.addUser(User("User1", "Email1"))
      user <- userRepository.getUser(u.id)
    } yield expect(user.contains(User(1, "User1", "Email1")))
  }

  test("addUsers") {
    for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1", "Email1"))
      _ <- userRepository.addUser(User("User2", "Email2"))
      _ <- userRepository.addUser(User("User3", "Email3"))
      count <- userRepository.countUsers
    } yield expect(count == 3)
  }

  test("addUser/getUserByName") {
    for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1", "Email1"))
      user <- userRepository.getUserByName("User1")
    } yield expect(user == List(User(1, "User1", "Email1")))
  }

  test("getUsers") {
    for {
      userRepository <- InMemoryUserRepository.empty
      _ <- userRepository.addUser(User("User1", "Email1"))
      _ <- userRepository.addUser(User("User2", "Email2"))
      _ <- userRepository.addUser(User("User3", "Email3"))
      users <- userRepository.getUsers
    } yield expect(users == List(User(1, "User1", "Email1"), User(2, "User2", "Email2"), User(3, "User3", "Email3")))
  }
}
