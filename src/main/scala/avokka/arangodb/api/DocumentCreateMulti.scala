package avokka.arangodb
package api

import avokka.velocypack._

/**
  * Create document
  * @param documents documents value
  * @param waitForSync Wait until document has been synced to disk.   (optional)
  * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
  * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
  * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
  * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
  * @tparam T document type
  */
case class DocumentCreateMulti[T](
    documents: List[T],
    waitForSync: Boolean = false,
    returnNew: Boolean = false,
    returnOld: Boolean = false,
    silent: Boolean = false,
    overwrite: Boolean = false,
) {
  def parameters = Map(
    "waitForSync" -> waitForSync.toString,
    "returnNew" -> returnNew.toString,
    "returnOld" -> returnOld.toString,
    "silent" -> silent.toString,
    "overwrite" -> overwrite.toString,
  )
}

object DocumentCreateMulti { self =>

  implicit def api[T: VPackDecoder: VPackEncoder]
    : Api.Aux[Collection, DocumentCreateMulti[T], List[T], List[Document.Response[T]]] =
    new Api[Collection, DocumentCreateMulti[T], List[T]] {
      override type Response = List[Document.Response[T]]
      override def header(collection: Collection, command: DocumentCreateMulti[T]): Request.HeaderTrait =
        Request.Header(
          database = collection.database.name,
          requestType = RequestType.POST,
          request = s"/_api/document/${collection.name}",
          parameters = command.parameters
        )

      override def body(collection: Collection, command: DocumentCreateMulti[T]): List[T] = command.documents
      override val encoder: VPackEncoder[List[T]] = implicitly
    }
}
