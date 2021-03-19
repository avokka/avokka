package avokka.arangodb

import avokka.arangodb.models.{CollectionCount, Document, Transaction}
import avokka.arangodb.protocol.{ArangoClient, ArangoResponse}
import avokka.arangodb.types.{CollectionName, DatabaseName, TransactionId}
import avokka.velocypack._

/**
  * Arango documents API
  *
  * @tparam F effect
  * @see [[https://www.arangodb.com/docs/stable/http/document-working-with-documents.html]]
  */
trait ArangoDocuments[F[_]] {

  /**
    * Counts the documents in a collection
    * @return
    */
  def count(
      transactionId: Option[TransactionId] = None
  ): F[ArangoResponse[CollectionCount]]

  /**
    * Create a document
    * @param document document value
    * @param waitForSync Wait until document has been synced to disk.   (optional)
    * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
    * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
    * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
    * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
    * @tparam T document type
    */
  def insert[T: VPackEncoder: VPackDecoder](
      document: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
      transaction: Option[TransactionId] = None
  ): F[ArangoResponse[Document[T]]]

  /**
    * Create documents
    * @param documents documents value
    * @param waitForSync Wait until document has been synced to disk.   (optional)
    * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
    * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
    * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
    * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
    * @tparam T document type
    */
  def create[T: VPackDecoder: VPackEncoder](
      documents: Seq[T],
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
      transaction: Option[TransactionId] = None
  ): F[ArangoResponse[Seq[Document[T]]]]

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
  def replace[T: VPackDecoder: VPackEncoder](
      documents: Seq[T],
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  ): F[ArangoResponse[Seq[Document[T]]]]

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
  def update[T: VPackDecoder, P: VPackEncoder](
      patch: Seq[P],
      keepNull: Boolean = false,
      mergeObjects: Boolean = true,
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  ): F[ArangoResponse[Seq[Document[T]]]]

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
    * @tparam K          Key selector type
    */
  def remove[T: VPackDecoder, K: VPackEncoder](
      keys: Seq[K],
      waitForSync: Boolean = false,
      returnOld: Boolean = false,
      ignoreRevs: Boolean = true,
      transaction: Option[TransactionId] = None
  ): F[ArangoResponse[Seq[Document[T]]]]
}

object ArangoDocuments {
  def apply[F[_]: ArangoClient](database: DatabaseName, collection: CollectionName): ArangoDocuments[F] =
    new ArangoDocuments[F] {

      private val path: String = "/_api/document/" + collection.repr

      override def count(
          transactionId: Option[TransactionId]
      ): F[ArangoResponse[CollectionCount]] =
        GET(
          database,
          "/_api/collection/" + collection.repr + "/count",
          meta = Map(
            Transaction.KEY -> transactionId.map(_.repr)
          ).collectDefined
        ).execute

      override def insert[T: VPackEncoder: VPackDecoder](
          document: T,
          waitForSync: Boolean,
          returnNew: Boolean,
          returnOld: Boolean,
          silent: Boolean,
          overwrite: Boolean,
          transaction: Option[TransactionId]
      ): F[ArangoResponse[Document[T]]] =
        ArangoClient[F].execute(
          POST(
            database,
            path,
            Map(
              "waitForSync" -> waitForSync.toString,
              "returnNew" -> returnNew.toString,
              "returnOld" -> returnOld.toString,
              "silent" -> silent.toString,
              "overwrite" -> overwrite.toString,
            ),
            Map(
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(document)
        )(
          implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)),
          implicitly
        )

      override def create[T: VPackDecoder: VPackEncoder](
          documents: Seq[T],
          waitForSync: Boolean,
          returnNew: Boolean,
          returnOld: Boolean,
          silent: Boolean,
          overwrite: Boolean,
          transaction: Option[TransactionId]
      ): F[ArangoResponse[Seq[Document[T]]]] =
        ArangoClient[F].execute(
          POST(
            database,
            path,
            Map(
              "waitForSync" -> waitForSync.toString,
              "returnNew" -> returnNew.toString,
              "returnOld" -> returnOld.toString,
              "silent" -> silent.toString,
              "overwrite" -> overwrite.toString,
            ),
            Map(
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(documents)
        )(
          VPackEncoder.seqEncoder(
            implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes))
          ),
          implicitly
        )

      override def replace[T: VPackDecoder: VPackEncoder](
          documents: Seq[T],
          waitForSync: Boolean,
          ignoreRevs: Boolean,
          returnOld: Boolean,
          returnNew: Boolean,
          transaction: Option[TransactionId]
      ): F[ArangoResponse[Seq[Document[T]]]] =
        ArangoClient[F].execute(
          PUT(
            database,
            path,
            Map(
              "waitForSync" -> waitForSync.toString,
              "ignoreRevs" -> ignoreRevs.toString,
              "returnOld" -> returnOld.toString,
              "returnNew" -> returnNew.toString,
            ),
            Map(
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(documents)
        )(
          VPackEncoder.seqEncoder(
            implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes))
          ),
          implicitly
        )

      override def update[T: VPackDecoder, P: VPackEncoder](
          patch: Seq[P],
          keepNull: Boolean,
          mergeObjects: Boolean,
          waitForSync: Boolean,
          ignoreRevs: Boolean,
          returnOld: Boolean,
          returnNew: Boolean,
          transaction: Option[TransactionId]
      ): F[ArangoResponse[Seq[Document[T]]]] =
        ArangoClient[F].execute(
          PATCH(
            database,
            path,
            Map(
              "keepNull" -> keepNull.toString,
              "mergeObjects" -> mergeObjects.toString,
              "waitForSync" -> waitForSync.toString,
              "ignoreRevs" -> ignoreRevs.toString,
              "returnOld" -> returnOld.toString,
              "returnNew" -> returnNew.toString,
            ),
            Map(
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(patch)
        )

      override def remove[T: VPackDecoder, K: VPackEncoder](
          keys: Seq[K],
          waitForSync: Boolean,
          returnOld: Boolean,
          ignoreRevs: Boolean,
          transaction: Option[TransactionId]
      ): F[ArangoResponse[Seq[Document[T]]]] =
        ArangoClient[F].execute(
          DELETE(
            database,
            path,
            Map(
              "waitForSync" -> waitForSync.toString,
              "returnOld" -> returnOld.toString,
              "ignoreRevs" -> ignoreRevs.toString,
            ),
            Map(
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(keys)
        )
    }
}
