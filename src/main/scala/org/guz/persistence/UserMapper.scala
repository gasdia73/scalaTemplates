package org.guz.persistence

import com.datastax.driver.mapping.MappingManager

import java.util.UUID

class UserMapper extends CassandraConnection {

  private lazy val mapper = new MappingManager(session).mapper(classOf[User])

  def save(user: User): Unit = {
    mapper.save(user)
  }

  def findById(id: UUID): User = {
    mapper.get(id)
  }

  def findAll(): List[User] = {
    import scala.collection.JavaConverters._
    val results = session.execute("SELECT * FROM user")
    mapper.map(results).all().asScala.toList
  }

  def removeById(user: User): Unit = {
    mapper.delete(user)
  }
}