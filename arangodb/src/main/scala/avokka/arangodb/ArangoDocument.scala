package avokka.arangodb

import avokka.arangodb.models._
import avokka.arangodb.protocol.{ArangoClient, ArangoResponse}
import avokka.arangodb.types._
import avokka.velocypack._
import cats.Functor

/**
  * Arango document API
  *
  * @tparam F effect
  * @see [[https://www.arangodb.com/docs/stable/http/document-working-with-documents.html]]
  */
trait ArangoDocument[F[_]] {

  /** document handle */
  def handle: DocumentHandle

  /**
    * Returns the document identified by *document-handle*. The returned document contains three special attributes:
    * *_id* containing the document handle, *_key* containing key which uniquely identifies a document
    * in a given collection and *_rev* containing the revision.
    *
    * @tparam T          The type of the document.
    * @param ifNoneMatch If the "If-None-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has a different revision than the
    *                    given Etag. Otherwise an HTTP 304 is returned.
    * @param ifMatch     If the "If-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has the same revision as the
    *                    given Etag. Otherwise a HTTP 412 is returned.
    */
  def read[T: VPackDecoder](
      ifNoneMatch: Option[String] = None,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None,
  ): F[ArangoResponse[T]]

  /**
    * Like read, but only returns the header fields and not the body. You can use this call to get the current revision of a document or check if the document was deleted.
    *
    * @param ifNoneMatch  If the “If-None-Match” header is given, then it must contain exactly one Etag. If the current document revision is not equal to the specified Etag, an HTTP 200 response is returned. If the current document revision is identical to the specified Etag, then an HTTP 304 is returned.
    * @param ifMatch      If the “If-Match” header is given, then it must contain exactly one Etag. The document is returned, if it has the same revision as the given Etag. Otherwise a HTTP 412 is returned.
    */
  def head(
      ifNoneMatch: Option[String] = None,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None,
  ): F[ArangoResponse.Header]

  /**
    * Removes a document
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
  def remove[T: VPackDecoder](
      waitForSync: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None,
  ): F[ArangoResponse[Document[T]]]

  /**
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
  def update[T: VPackDecoder, P: VPackEncoder](
      patch: P,
      keepNull: Boolean = false,
      mergeObjects: Boolean = true,
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      silent: Boolean = false,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None,
  ): F[ArangoResponse[Document[T]]]

  /**
    * @param document representation of a document update as an object
    * @param waitForSync Wait until document has been synced to disk.   (optional)
    * @param ignoreRevs By default, or if this is set to *true*, the *_rev* attributes in  the given document is ignored. If this is set to *false*, then the *_rev* attribute given in the body document is taken as a precondition. The document is only updated if the current revision is the one specified.   (optional)
    * @param returnOld Return additionally the complete previous revision of the changed  document under the attribute *old* in the result.   (optional)
    * @param returnNew Return additionally the complete new document under the attribute *new* in the result.   (optional)
    * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the updated document. This option can be used to save some network traffic.   (optional)
    * @param ifMatch You can conditionally update a document based on a target revision id by using the *if-match* HTTP header.   (optional)
    * @tparam T document type
    */
  def replace[T: VPackEncoder: VPackDecoder](
      document: T,
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      silent: Boolean = false,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None,
  ): F[ArangoResponse[Document[T]]]

  /**
    * build an UPSERT query at key with INSERT+UPDATE from obj
    * @param obj vpack object
    * @return query
    */
  def upsert(obj: VObject): ArangoQuery[F, VObject]
}

object ArangoDocument {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName, _handle: DocumentHandle): ArangoDocument[F] =
    new ArangoDocument[F] {

      override val handle: DocumentHandle = _handle

      private val path: String = API_DOCUMENT + "/" + handle.path

      override def read[T: VPackDecoder](
          ifNoneMatch: Option[String],
          ifMatch: Option[String],
          transaction: Option[TransactionId],
      ): F[ArangoResponse[T]] =
        GET(
          database,
          path,
          meta = Map(
            "If-None-Match" -> ifNoneMatch,
            "If-Match" -> ifMatch,
            Transaction.KEY -> transaction.map(_.repr)
          ).collectDefined
        ).execute

      override def head(
          ifNoneMatch: Option[String],
          ifMatch: Option[String],
          transaction: Option[TransactionId],
      ): F[ArangoResponse.Header] =
        HEAD(
          database,
          path,
          meta = Map(
            "If-None-Match" -> ifNoneMatch,
            "If-Match" -> ifMatch,
            Transaction.KEY -> transaction.map(_.repr)
          ).collectDefined
        ).execute

      override def remove[T: VPackDecoder](
          waitForSync: Boolean,
          returnOld: Boolean,
          silent: Boolean,
          ifMatch: Option[String],
          transaction: Option[TransactionId],
      ): F[ArangoResponse[Document[T]]] =
        DELETE(
          database,
          path,
          Map(
            "waitForSync" -> waitForSync.toString,
            "returnOld" -> returnOld.toString,
            "silent" -> silent.toString,
          ),
          Map(
            "If-Match" -> ifMatch,
            Transaction.KEY -> transaction.map(_.repr)
          ).collectDefined
        ).execute

      override def update[T: VPackDecoder, P: VPackEncoder](
          patch: P,
          keepNull: Boolean,
          mergeObjects: Boolean,
          waitForSync: Boolean,
          ignoreRevs: Boolean,
          returnOld: Boolean,
          returnNew: Boolean,
          silent: Boolean,
          ifMatch: Option[String],
          transaction: Option[TransactionId],
      ): F[ArangoResponse[Document[T]]] =
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
            "silent" -> silent.toString,
          ),
          Map(
            "If-Match" -> ifMatch,
            Transaction.KEY -> transaction.map(_.repr)
          ).collectDefined
        ).body(patch).execute

      override def replace[T: VPackEncoder: VPackDecoder](
          document: T,
          waitForSync: Boolean,
          ignoreRevs: Boolean,
          returnOld: Boolean,
          returnNew: Boolean,
          silent: Boolean,
          ifMatch: Option[String],
          transaction: Option[TransactionId],
      ): F[ArangoResponse[Document[T]]] =
        ArangoClient[F].execute(
          PUT(
            database,
            path,
            Map(
              "waitForSync" -> waitForSync.toString,
              "ignoreRevs" -> ignoreRevs.toString,
              "returnOld" -> returnOld.toString,
              "returnNew" -> returnNew.toString,
              "silent" -> silent.toString,
            ),
            Map(
              "If-Match" -> ifMatch,
              Transaction.KEY -> transaction.map(_.repr)
            ).collectDefined
          ).body(document)
        )(
          implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)),
          implicitly
        )

      override def upsert(obj: VObject): ArangoQuery[F, VObject] = {
        val kvs = obj.values.keys
          .map { key =>
            key + ":@" + key
          }
          .mkString(",")
        ArangoQuery[F, VObject](
          database,
          Query(
            s"UPSERT {_key:@_key} INSERT {_key:@_key,$kvs} UPDATE {$kvs} IN @@collection RETURN NEW",
            obj.updated("@collection", handle.collection).updated("_key", handle.key)
          )
        )
      }
    }

}
