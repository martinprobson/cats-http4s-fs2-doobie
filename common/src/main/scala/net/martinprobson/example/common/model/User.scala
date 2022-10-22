package net.martinprobson.example.common.model

import User.*

case class User(id: USER_ID, name: String)

object User {

  def apply(id: USER_ID, name: String): User = new User(id, name)
  def apply(name: String): User = new User(UNASSIGNED_USER_ID, name)

  type USER_ID = Long
  val UNASSIGNED_USER_ID = 0L
}
