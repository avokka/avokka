package avokka.arangodb
package api

import avokka.velocypack._

/**
  * @param handle document handle
  * @param patch representation of a document update as an object
  * @param keepNull If the intention is to delete existing attributes with the patch command, the URL query parameter *keepNull* can be used with a value of *false*. This will modify the behavior of the patch command to remove any attributes from the existing document that are contained in the patch document with an attribute value of *null*.   (optional)
  * @param mergeObjects Controls whether objects (not arrays) will be merged if present in both the existing and the patch document. If set to *false*, the value in the patch document will overwrite the existing document&#39;s value. If set to *true*, objects will be merged. The default is *true*.   (optional)
  * @param waitForSync Wait until document has been synced to disk.   (optional)
  * @param ignoreRevs By default, or if this is set to *true*, the *_rev* attributes in  the given document is ignored. If this is set to *false*, then the *_rev* attribute given in the body document is taken as a precondition. The document is only updated if the current revision is the one specified.   (optional)
  * @param returnOld Return additionally the complete previous revision of the changed  document under the attribute *old* in the result.   (optional)
  * @param returnNew Return additionally the complete new document under the attribute *new* in the result.   (optional)
  * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the updated document. This option can be used to save some network traffic.   (optional)
  * @param ifMatch You can conditionally update a document based on a target revision id by using the *if-match* HTTP header.   (optional)
  * @tparam T document type
  * @tparam P patch type
  */
case class DocumentUpdate[T, P](
    handle: DocumentHandle,
    patch: P,
    keepNull: Boolean = false,
    mergeObjects: Boolean = true,
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
    silent: Boolean = false,
    ifMatch: Option[String] = None,
) {
  def parameters: Map[String, String] = Map(
    "keepNull" -> keepNull.toString,
    "mergeObjects" -> mergeObjects.toString,
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

object DocumentUpdate {

  implicit def api[P: VPackEncoder, T: VPackDecoder]: Api.Aux[ArangoDatabase, DocumentUpdate[T, P], P, Document.Response[T]] =
    new Api[ArangoDatabase, DocumentUpdate[T, P], P] {
      override type Response = Document.Response[T]
      override def header(database: ArangoDatabase, command: DocumentUpdate[T, P]): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.PATCH,
        request = s"/_api/document/${command.handle.path}",
        parameters = command.parameters,
        meta = command.meta,
      )
      override def body(context: ArangoDatabase, command: DocumentUpdate[T, P]): P = command.patch
      override val encoder: VPackEncoder[P] = implicitly
    }

}
