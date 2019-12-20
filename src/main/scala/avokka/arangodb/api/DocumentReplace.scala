package avokka.arangodb
package api

import avokka.velocypack._

/**
  * @param handle document handle
  * @param document representation of a document update as an object
  * @param waitForSync Wait until document has been synced to disk.   (optional)
  * @param ignoreRevs By default, or if this is set to *true*, the *_rev* attributes in  the given document is ignored. If this is set to *false*, then the *_rev* attribute given in the body document is taken as a precondition. The document is only updated if the current revision is the one specified.   (optional)
  * @param returnOld Return additionally the complete previous revision of the changed  document under the attribute *old* in the result.   (optional)
  * @param returnNew Return additionally the complete new document under the attribute *new* in the result.   (optional)
  * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the updated document. This option can be used to save some network traffic.   (optional)
  * @param ifMatch You can conditionally update a document based on a target revision id by using the *if-match* HTTP header.   (optional)
  * @tparam T document type
  */
case class DocumentReplace[T](
    handle: DocumentHandle,
    document: T,
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
    silent: Boolean = false,
    ifMatch: Option[String] = None,
) {
  def parameters: Map[String, String] = Map(
    "waitForSync" -> waitForSync.toString,
    "ignoreRevs" -> ignoreRevs.toString,
    "returnOld" -> returnOld.toString,
    "returnNew" -> returnNew.toString,
    "silent" -> silent.toString,
  )
  def meta: Map[String, String] = {
    val m = Map.newBuilder[String, String]
    ifMatch.foreach(m += "If-Match" -> _)
    m.result
  }
}

object DocumentReplace {

  implicit def api[T: VPackEncoder : VPackDecoder]: Api.Aux[Database, DocumentReplace[T], T, Document.Response[T]] =
    new Api[Database, DocumentReplace[T], T] {
      override type Response = Document.Response[T]
      override def header(database: Database, command: DocumentReplace[T]): Request.HeaderTrait = Request.Header(
        database = database.name,
        requestType = RequestType.PUT,
        request = s"/_api/document/${command.handle.path}",
        parameters = command.parameters,
        meta = command.meta,
      )
      override def body(context: Database, command: DocumentReplace[T]): T = command.document
      override val encoder: VPackEncoder[T] = implicitly
    }

}
