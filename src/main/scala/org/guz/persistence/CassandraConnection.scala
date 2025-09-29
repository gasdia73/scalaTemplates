package org.guz.persistence

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

trait CassandraConnection {
  private lazy val cluster = Cluster.builder()
    .addContactPoint("")
    .withCredentials("cassandra", "cassandra")
    .withoutMetrics()
    .build()

  lazy val session: Session = cluster.connect("user_details")

}
