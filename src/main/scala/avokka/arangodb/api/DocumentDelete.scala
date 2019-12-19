package avokka.arangodb
package api

import avokka.velocypack._

import scala.collection.mutable

/**
 * Removes a document
 * @param handle      Removes the document identified by document-handle.
 * @param waitForSync Wait until deletion operation has been synced to disk.
 * @param returnOld   Return additionally the complete previous revision of the changed
 *                    document under the attribute old in the result.
 * @param silent      If set to true, an empty object will be returned as response. No meta-data
 *                    will be returned for the removed document. This option can be used to
 *                    save some network traffic.
 * @param ifMatch     You can conditionally remove a document based on a target revision id by
 *                    using the if-match HTTP header.
 * @tparam T          Response body type
 */
case class DocumentDelete[T]
(
  handle: DocumentHandle,
  waitForSync: Boolean = false,
  returnOld: Boolean = false,
  silent: Boolean = false,
  ifMatch: Option[String] = None,
)
{
  def parameters = Map(
    "waitForSync" -> waitForSync.toString,
    "returnOld" -> returnOld.toString,
    "silent" -> silent.toString,
  )
  def meta = {
    val m = new mutable.HashMap[String, String]
    ifMatch.foreach(m.update("If-Match", _))
    m.toMap
  }
}

object DocumentDelete {

  implicit def api[T : VPackDecoder]: Api.EmptyBody.Aux[Database, DocumentDelete[T], Document.Response[T]] = new Api.EmptyBody[Database, DocumentDelete[T]] {
    override type Response = Document.Response[T]
    override def header(database: Database, command: DocumentDelete[T]): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.DELETE,
      request = s"/_api/document/${command.handle.path}",
      parameters = command.parameters,
      meta = command.meta,
    )
  }
}

