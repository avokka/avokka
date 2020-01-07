package avokka.arangodb
package api

import avokka.velocypack._

/**
  * Partially updates documents, the documents to update are specified
  * by the _key attributes in the body objects. The body of the
  * request must contain a JSON array of document updates with the
  * attributes to patch (the patch documents). All attributes from the
  * patch documents will be added to the existing documents if they do
  * not yet exist, and overwritten in the existing documents if they do
  * exist there
  *
  * @param patch        representation of an array of document updates as objects
  * @param keepNull     If the intention is to delete existing attributes with the patch
  *                     command, the URL query parameter keepNull can be used with a value
  *                     of false. This will modify the behavior of the patch command to
  *                     remove any attributes from the existing document that are contained
  *                     in the patch document with an attribute value of null
  * @param mergeObjects Controls whether objects (not arrays) will be merged if present in
  *                     both the existing and the patch document. If set to false, the
  *                     value in the patch document will overwrite the existing document's
  *                     value. If set to true, objects will be merged. The default is true
  * @param waitForSync  Wait until the new documents have been synced to disk
  * @param ignoreRevs   By default, or if this is set to true, the _rev attributes in
  *                     the given documents are ignored. If this is set to false, then
  *                     any _rev attribute given in a body document is taken as a
  *                     precondition. The document is only updated if the current revision
  *                     is the one specified
  * @param returnOld    Return additionally the complete previous revision of the changed
  *                     documents under the attribute old in the result
  * @param returnNew    Return additionally the complete new documents under the attribute new
  *                     in the result
  * @tparam T document type
  * @tparam P patch type
  */
case class DocumentUpdateMulti[T, P](
    patch: List[P],
    keepNull: Boolean = false,
    mergeObjects: Boolean = true,
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
) {
  def parameters: Map[String, String] = Map(
    "keepNull" -> keepNull.toString,
    "mergeObjects" -> mergeObjects.toString,
    "waitForSync" -> waitForSync.toString,
    "ignoreRevs" -> ignoreRevs.toString,
    "returnOld" -> returnOld.toString,
    "returnNew" -> returnNew.toString,
  )
}

object DocumentUpdateMulti {

  implicit def api[P: VPackEncoder, T: VPackDecoder]
    : Api.Aux[ArangoCollection, DocumentUpdateMulti[T, P], List[P], List[Document.Response[T]]] =
    new Api[ArangoCollection, DocumentUpdateMulti[T, P], List[P]] {
      override type Response = List[Document.Response[T]]
      override def header(collection: ArangoCollection,
                          command: DocumentUpdateMulti[T, P]): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.PATCH,
        request = s"/_api/document/${collection.name}",
        parameters = command.parameters,
      )
      override def body(collection: ArangoCollection, command: DocumentUpdateMulti[T, P]): List[P] = command.patch
      override val encoder: VPackEncoder[List[P]] = implicitly
    }

}
