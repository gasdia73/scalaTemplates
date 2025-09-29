package org.guz.persistence

import java.util.UUID

@main def CassandraApp(): Unit =
  // val id = UUID.randomUUID()
  // val usr = User(id, "Coscienza", 23)

  // val mapper = new UserMapper()
  // mapper.save(usr)
  // println(mapper.findById(id))
  // mapper.removeById(usr)
  // println(mapper.findById(id))

  val mapper = new UserMapper()
  mapper.findAll().foreach(println)