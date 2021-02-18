package avokka.arangodb

import api._
import avokka.arangodb.protocol.{ArangoError, ArangoProtocol, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.{CollectionName, DatabaseName, DocumentHandle, DocumentKey}
import avokka.velocypack.{VPackDecoder, VPackEncoder}

trait ArangoDocument[F[_]] {

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
  ): F[ArangoResponse[T]]

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
  ): F[ArangoResponse[Document[T]]]
}

object ArangoDocument {

  def apply[F[_]: ArangoProtocol](database: DatabaseName, handle: DocumentHandle): ArangoDocument[F] = new ArangoDocument[F] {

    override def read[T: VPackDecoder](
        ifNoneMatch: Option[String],
        ifMatch: Option[String]
    ): F[ArangoResponse[T]] =
      ArangoProtocol[F].execute(
        ArangoRequest.GET(
          database,
          s"/_api/document/${handle.path}",
          meta = Map(
            "If-None-Match" -> ifNoneMatch,
            "If-Match" -> ifMatch
          ).collectDefined
        )
      )

    override def remove[T: VPackDecoder](
        waitForSync: Boolean,
        returnOld: Boolean,
        silent: Boolean,
        ifMatch: Option[String]
    ): F[ArangoResponse[Document[T]]] =
      ArangoProtocol[F].execute(
        ArangoRequest.DELETE(
          database,
          s"/_api/document/${handle.path}",
          Map(
            "waitForSync" -> waitForSync.toString,
            "returnOld" -> returnOld.toString,
            "silent" -> silent.toString,
          ),
          Map(
            "If-Match" -> ifMatch
          ).collectDefined
        )
      )

    override def update[T: VPackDecoder, P: VPackEncoder](
        patch: P,
        keepNull: Boolean,
        mergeObjects: Boolean,
        waitForSync: Boolean,
        ignoreRevs: Boolean,
        returnOld: Boolean,
        returnNew: Boolean,
        silent: Boolean,
        ifMatch: Option[String]
    ): F[ArangoResponse[Document[T]]] =
      ArangoProtocol[F].execute(
        ArangoRequest
          .PATCH(
            database,
            s"/_api/document/${handle.path}",
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
              "If-Match" -> ifMatch
            ).collectDefined
          )
          .body(patch)
      )

    override def replace[T: VPackEncoder: VPackDecoder](
        document: T,
        waitForSync: Boolean,
        ignoreRevs: Boolean,
        returnOld: Boolean,
        returnNew: Boolean,
        silent: Boolean,
        ifMatch: Option[String]
    ): F[ArangoResponse[Document[T]]] =
      ArangoProtocol[F].execute(
        ArangoRequest
          .PUT(
            database,
            s"/_api/document/${handle.path}",
            Map(
              "waitForSync" -> waitForSync.toString,
              "ignoreRevs" -> ignoreRevs.toString,
              "returnOld" -> returnOld.toString,
              "returnNew" -> returnNew.toString,
              "silent" -> silent.toString,
            ),
            Map(
              "If-Match" -> ifMatch
            ).collectDefined
          )
          .body(document)
      )(
        implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)),
        implicitly
      )
  }

}
