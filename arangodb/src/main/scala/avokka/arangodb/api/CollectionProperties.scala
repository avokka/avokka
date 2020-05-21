package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionProperties(
    name: CollectionName
)

object CollectionProperties { self =>

  /**
    * A object which contains key generation options
    *
    * @param allowUserKeys if set to *true*, then it is allowed to supply own key values in the *_key* attribute of a document. If set to *false*, then the key generator is solely responsible for generating keys and supplying own key values in the *_key* attribute of documents is considered an error.
    * @param lastValue
    * @param `type` specifies the type of the key generator. The currently available generators are *traditional*, *autoincrement*, *uuid* and *padded*.
    */
  final case class KeyOptions(
      allowUserKeys: Boolean,
      lastValue: Option[Int],
      `type`: String
  )

  object KeyOptions {
    implicit val decoder: VPackDecoder[KeyOptions] = VPackRecord[KeyOptions].decoderWithDefaults
  }

  /**
    *
    * @param doCompact Whether or not the collection will be compacted. This option is only present for the MMFiles storage engine.
    * @param globallyUniqueId Unique identifier of the collection
    * @param id unique identifier of the collection; *deprecated*
    * @param indexBuckets the number of index buckets *Only relevant for the MMFiles storage engine*
    * @param isSystem true if this is a system collection; usually *name* will start with an underscore.
    * @param isVolatile If *true* then the collection data will be kept in memory only and ArangoDB will not write or sync the data to disk. This option is only present for the MMFiles storage engine.
    * @param journalSize The maximal size setting for journals / datafiles in bytes. This option is only present for the MMFiles storage engine.
    * @param keyOptions
    * @param minReplicationFactor contains how many minimal copies of each shard need to be in sync on different DBServers. The shards will refuse to write, if we have less then these many copies in sync. *Cluster specific attribute.*
    * @param name literal name of this collection
    * @param numberOfShards The number of shards of the collection; *Cluster specific attribute.*
    * @param replicationFactor contains how many copies of each shard are kept on different DBServers.; *Cluster specific attribute.*
    * @param shardKeys contains the names of document attributes that are used to determine the target shard for documents; *Cluster specific attribute.*
    * @param shardingStrategy the sharding strategy selected for the collection; *Cluster specific attribute.* One of 'hash' or 'enterprise-hash-smart-edge'
    * @param smartGraphAttribute Attribute that is used in smart graphs, *Cluster specific attribute.*
    * @param status corrosponds to **statusString**; *Only relevant for the MMFiles storage engine*   - 0: \"unknown\" - may be corrupted   - 1: (deprecated, maps to \"unknown\")   - 2: \"unloaded\"   - 3: \"loaded\"   - 4: \"unloading\"   - 5: \"deleted\"   - 6: \"loading\"
    * @param statusString any of: [\"unloaded\", \"loading\", \"loaded\", \"unloading\", \"deleted\", \"unknown\"] *Only relevant for the MMFiles storage engine*
    * @param `type` The type of the collection:   - 0: \"unknown\"   - 2: regular document collection   - 3: edge collection
    * @param waitForSync If *true* then creating, changing or removing documents will wait until the data has been synchronized to disk.
    */
  final case class Response(
      doCompact: Option[Boolean] = None,
      globallyUniqueId: Option[String],
      id: Option[String],
      indexBuckets: Option[Int] = None,
      isSystem: Boolean,
      isVolatile: Option[Boolean] = None,
      journalSize: Option[Int] = None,
      keyOptions: KeyOptions,
      name: CollectionName,
      status: Option[CollectionStatus] = None,
      statusString: Option[String] = None,
      `type`: Option[CollectionType],
      waitForSync: Boolean,
      minReplicationFactor: Option[Int] = None,
      numberOfShards: Option[Int] = None,
      replicationFactor: Option[Int] = None,
      shardKeys: Option[List[String]] = None,
      shardingStrategy: Option[String] = None,
      smartGraphAttribute: Option[String] = None,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionProperties, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionProperties] {
      override type Response = self.Response
      override def header(database: ArangoDatabase,
                          command: CollectionProperties): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/collection/${command.name}/properties",
        )
    }

}
