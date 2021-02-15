package avokka.arangodb
package api

import avokka.velocypack._
import types._

/**
  * Replaces multiple documents in the specified collection with the
  * ones in the body, the replaced documents are specified by the _key
  * attributes in the body documents.
  *
  * @param documents   representation of an array of documents
  * @param waitForSync Wait until the new documents have been synced to disk
  * @param ignoreRevs  By default, or if this is set to true, the _rev attributes in
  *                    the given documents are ignored. If this is set to false, then
  *                    any _rev attribute given in a body document is taken as a
  *                    precondition. The document is only replaced if the current revision
  *                    is the one specified
  * @param returnOld   Return additionally the complete previous revision of the changed
  *                    documents under the attribute old in the result
  * @param returnNew   Return additionally the complete new documents under the attribute new
  *                    in the result
  * @tparam T document type
  */
final case class DocumentReplaceMulti[T](
    collection: CollectionName,
    documents: List[T],
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
) {
  def parameters: Map[String, String] = Map(
    "waitForSync" -> waitForSync.toString,
    "ignoreRevs" -> ignoreRevs.toString,
    "returnOld" -> returnOld.toString,
    "returnNew" -> returnNew.toString,
  )
}

object DocumentReplaceMulti {

  implicit def api[T: VPackEncoder: VPackDecoder]
    : Api.Aux[ArangoDatabase, DocumentReplaceMulti[T], List[T], List[Document.Response[T]]] =
    new Api[ArangoDatabase, DocumentReplaceMulti[T], List[T]] {
      override type Response = List[Document.Response[T]]
      override def header(database: ArangoDatabase,
                          command: DocumentReplaceMulti[T]): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.PUT,
          request = s"/_api/document/${command.collection}",
          parameters = command.parameters,
        )
      override def body(database: ArangoDatabase, command: DocumentReplaceMulti[T]): List[T] =
        command.documents
      override val encoder: VPackEncoder[List[T]] = VPackEncoder.listEncoder(
        implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)))
    }

}
