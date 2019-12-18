package avokka.arangodb

import avokka.velocypack._

class Database(val session: Session, databaseName: String) extends ApiContext[Database] {

  lazy val name = DatabaseName(databaseName)

}

object Database {
  val systemName: DatabaseName = DatabaseName("_system")
}
