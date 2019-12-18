package avokka.arangodb

import avokka.velocypack._

class Database(val session: Session, databaseName: String) extends ApiContext[Database] {

  lazy val name = DatabaseName(databaseName)

  def info() = {
    session.exec[api.DatabaseCurrent](Request.Header(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/database/current"
    )).value
  }

  def collections(excludeSystem: Boolean = false) = {
    session.exec[api.CollectionList](Request.Header(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map(
        "excludeSystem" -> excludeSystem.toString
      )
    )).value
  }

  def document[T](handle: DocumentHandle)(implicit d: VPackDecoder[T]) = {
    session.exec[T](Request.Header(
      database = name,
      requestType = RequestType.GET,
      request = s"/_api/document/${handle.path}"
    )).value
  }

}

object Database {
  val systemName: DatabaseName = DatabaseName("_system")
}