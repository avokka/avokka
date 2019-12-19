package avokka.arangodb
package api

import avokka.velocypack._

/**
 * @param name                     The name of the collection.
 * @param distributeShardsLike     (The default is *\"\"*): in an Enterprise Edition cluster, this attribute binds the specifics of sharding for the newly created collection to follow that of a specified existing collection. **Note**: Using this parameter has consequences for the prototype collection. It can no longer be dropped, before the sharding-imitating collections are dropped. Equally, backups and restores of imitating collections alone will generate warnings (which can be overridden) about missing sharding prototype.
 * @param doCompact                whether or not the collection will be compacted (default is *true*) This option is meaningful for the MMFiles storage engine only.
 * @param indexBuckets             The number of buckets into which indexes using a hash table are split. The default is 16 and this number has to be a power of 2 and less than or equal to 1024.  For very large collections one should increase this to avoid long pauses when the hash table has to be initially built or resized, since buckets are resized individually and can be initially built in parallel. For example, 64 might be a sensible value for a collection with 100 000 000 documents. Currently, only the edge index respects this value, but other index types might follow in future ArangoDB versions. Changes (see below) are applied when the collection is loaded the next time. This option is meaningful for the MMFiles storage engine only.
 * @param isSystem                 If *true*, create a  system collection. In this case *collection-name* should start with an underscore. End users should normally create non-system collections only. API implementors may be required to create system collections in very special occasions, but normally a regular collection will do. (The default is *false*)
 * @param isVolatile               If *true* then the collection data is kept in-memory only and not made persistent. Unloading the collection will cause the collection data to be discarded. Stopping or re-starting the server will also cause full loss of data in the collection. Setting this option will make the resulting collection be slightly faster than regular collections because ArangoDB does not enforce any synchronization to disk and does not calculate any CRC checksums for datafiles (as there are no datafiles). This option should therefore be used for cache-type collections only, and not for data that cannot be re-created otherwise. (The default is *false*) This option is meaningful for the MMFiles storage engine only.
 * @param journalSize              The maximal size of a journal or datafile in bytes. The value must be at least `1048576` (1 MiB). (The default is a configuration parameter) This option is meaningful for the MMFiles storage engine only.
 * @param keyOptions               additional options for key generation
 * @param numberOfShards           (The default is *1*): in a cluster, this value determines the number of shards to create for the collection. In a single server setup, this option is meaningless.
 * @param replicationFactor        (The default is *1*): in a cluster, this attribute determines how many copies of each shard are kept on different DBServers. The value 1 means that only one copy (no synchronous replication) is kept. A value of k means that k-1 replicas are kept. Any two copies reside on different DBServers. Replication between them is  synchronous, that is, every write operation to the \"leader\" copy will be replicated  to all \"follower\" replicas, before the write operation is reported successful.  If a server fails, this is detected automatically and one of the servers holding  copies take over, usually without an error being reported.
 * @param shardKeys                (The default is *[ \"_key\" ]*): in a cluster, this attribute determines which document attributes are used to determine the target shard for documents. Documents are sent to shards based on the values of their shard key attributes. The values of all shard key attributes in a document are hashed, and the hash value is used to determine the target shard. **Note**: Values of shard key attributes cannot be changed once set.   This option is meaningless in a single server setup.
 * @param shardingStrategy         This attribute specifies the name of the sharding strategy to use for  the collection. Since ArangoDB 3.4 there are different sharding strategies  to select from when creating a new collection. The selected *shardingStrategy*  value will remain fixed for the collection and cannot be changed afterwards.  This is important to make the collection keep its sharding settings and always find documents already distributed to shards using the same initial sharding algorithm.  The available sharding strategies are: - `community-compat`: default sharding used by ArangoDB   Community Edition before version 3.4 - `enterprise-compat`: default sharding used by ArangoDB   Enterprise Edition before version 3.4 - `enterprise-smart-edge-compat`: default sharding used by smart edge   collections in ArangoDB Enterprise Edition before version 3.4 - `hash`: default sharding used for new collections starting from version 3.4   (excluding smart edge collections) - `enterprise-hash-smart-edge`: default sharding used for new   smart edge collections starting from version 3.4  If no sharding strategy is specified, the default will be *hash* for all collections, and *enterprise-hash-smart-edge* for all smart edge collections (requires the *Enterprise Edition* of ArangoDB).  Manually overriding the sharding strategy does not yet provide a  benefit, but it may later in case other sharding strategies are added.
 * @param smartJoinAttribute       In an *Enterprise Edition* cluster, this attribute determines an attribute of the collection that must contain the shard key value of the referred-to  smart join collection. Additionally, the shard key for a document in this  collection must contain the value of this attribute, followed by a colon,  followed by the actual primary key of the document.  This feature can only be used in the *Enterprise Edition* and requires the *distributeShardsLike* attribute of the collection to be set to the name of another collection. It also requires the *shardKeys* attribute of the collection to be set to a single shard key attribute, with an additional ':' at the end. A further restriction is that whenever documents are stored or updated in the  collection, the value stored in the *smartJoinAttribute* must be a string.
 * @param `type`                   (The default is *2*): the type of the collection to create. The following values for *type* are valid:  - *2*: document collection - *3*: edge collection
 * @param waitForSync              If *true* then the data is synchronized to disk before returning from a document create, update, replace or removal operation. (default: false)
 * @param waitForSyncReplication   Default is 1 which means the server will only report success back to the
 *                                 client if all replicas have created the collection. Set to 0 if you want
 *                                 faster server responses and don't care about full replication.
 * @param enforceReplicationFactor Default is 1 which means the server will check if there are enough replicas
 *                                 available at creation time and bail out otherwise. Set to 0 to disable
 *                                 this extra check.
 */
case class CollectionCreate
(
  name: CollectionName,
  distributeShardsLike: String = "",
  doCompact: Boolean = true,
  indexBuckets: Option[Long] = None,
  isSystem: Boolean = false,
  isVolatile: Boolean = false,
  journalSize: Option[Long] = None,
  keyOptions: Option[CollectionCreate.KeyOptions] = None,
  numberOfShards: Long = 1,
  replicationFactor: Long = 1,
  shardKeys: List[String] = List("_key"),
  shardingStrategy: Option[String] = None,
  smartJoinAttribute: Option[String] = None,
  `type`: CollectionType = CollectionType.Document,
  waitForSync: Boolean = false,
  waitForSyncReplication: Int = 1,
  enforceReplicationFactor: Int = 1,
)
{
  def parameters = Map(
    "waitForSyncReplication" -> waitForSyncReplication.toString,
    "enforceReplicationFactor" -> enforceReplicationFactor.toString,
  )
}

object CollectionCreate { self =>

  /**
   * additional options for key generation. If specified, then *keyOptions* should be a JSON array containing the following attributes:
   *
   * @param allowUserKeys if set to *true*, then it is allowed to supply own key values in the *_key* attribute of a document. If set to *false*, then the key generator will solely be responsible for generating keys and supplying own key values in the *_key* attribute of documents is considered an error.
   * @param increment     increment value for *autoincrement* key generator. Not used for other key generator types.
   * @param offset        Initial offset value for *autoincrement* key generator. Not used for other key generator types.
   * @param `type`        specifies the type of the key generator. The currently available generators are *traditional*, *autoincrement*, *uuid* and *padded*.  The *traditional* key generator generates numerical keys in ascending order. The *autoincrement* key generator generates numerical keys in ascending order,  the inital offset and the spacing can be configured The *padded* key generator generates keys of a fixed length (16 bytes) in ascending lexicographical sort order. This is ideal for usage with the _RocksDB_ engine, which will slightly benefit keys that are inserted in lexicographically ascending order. The key generator can be used in a single-server or cluster. The *uuid* key generator generates universally unique 128 bit keys, which  are stored in hexadecimal human-readable format. This key generator can be used in a single-server or cluster to generate \"seemingly random\" keys. The keys  produced by this key generator are not lexicographically sorted.
   */
  case class KeyOptions
  (
    allowUserKeys: Option[Boolean] = None,
    increment: Option[Long] = None,
    offset: Option[Long] = None,
    `type`: Option[String] = None,
  )

  object KeyOptions {
    implicit val encoder: VPackEncoder[KeyOptions] = VPackRecord[KeyOptions].encoder
  }

  implicit val encoder: VPackEncoder[CollectionCreate] = VPackRecord[CollectionCreate].encoder

  implicit val api: Api.Command.Aux[Database, CollectionCreate, CollectionInfo.Response] = new Api.Command[Database, CollectionCreate] {
    override type Response = CollectionInfo.Response
    override def header(database: Database, command: CollectionCreate): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.POST,
      request = s"/_api/collection",
      parameters = command.parameters
    )
    override val encoder: VPackEncoder[CollectionCreate] = self.encoder
  }
}
