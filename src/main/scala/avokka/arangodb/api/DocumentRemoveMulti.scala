package avokka.arangodb
package api

import avokka.velocypack._

/**
  * The body of the request is an array consisting of selectors for
  * documents. A selector can either be a string with a key or a string
  * with a document handle or an object with a _key attribute. This
  * API call removes all specified documents from collection. If the
  * selector is an object and has a _rev attribute, it is a
  * precondition that the actual revision of the removed document in the
  * collection is the specified one
  *
  * @param keys        Removes the document identified by document-handle.
  * @param waitForSync Wait until deletion operation has been synced to disk.
  * @param returnOld   Return additionally the complete previous revision of the changed
  *                    document under the attribute old in the result.
  * @param ignoreRevs  If set to true, ignore any _rev attribute in the selectors. No
  *                    revision check is performed
  * @tparam T          Response body type
  */
case class DocumentRemoveMulti[T, K](
    keys: List[K],
    waitForSync: Boolean = false,
    returnOld: Boolean = false,
    ignoreRevs: Boolean = true,
) {
  def parameters = Map(
    "waitForSync" -> waitForSync.toString,
    "returnOld" -> returnOld.toString,
    "ignoreRevs" -> ignoreRevs.toString,
  )
}

object DocumentRemoveMulti {

  implicit def api[T: VPackDecoder, K: VPackEncoder]
    : Api.Aux[ArangoCollection, DocumentRemoveMulti[T, K], List[K], List[Document.Response[T]]] =
    new Api[ArangoCollection, DocumentRemoveMulti[T, K], List[K]] {
      override type Response = List[Document.Response[T]]
      override def header(collection: ArangoCollection,
                          command: DocumentRemoveMulti[T, K]): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.DELETE,
        request = s"/_api/document/${collection.name}",
        parameters = command.parameters,
      )
      override def body(collection: ArangoCollection, command: DocumentRemoveMulti[T, K]): List[K] =
        command.keys
      override val encoder: VPackEncoder[List[K]] = implicitly
    }
}
