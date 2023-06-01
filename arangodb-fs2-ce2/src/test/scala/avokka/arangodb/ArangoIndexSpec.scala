package avokka.arangodb

import avokka.arangodb.models.Index
import avokka.arangodb.types._

class ArangoIndexSpec extends ArangoIOBase {

  val collectionName = CollectionName("countries")

  it should "create, read, delete" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createHash(List("name"))
      ls      <- collection.indexes.list()
      idx     <- collection.index(created.body.id).read()
      deleted <- collection.index(created.body.id).delete()
      ls2     <- collection.indexes.list()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.hash)
      created.body.isNewlyCreated should be (true)

      ls.body.indexes.map(_.`type`) should contain (Index.Type.hash)

      idx.header.responseCode should be (200)
      idx.body.`type` should be (Index.Type.hash)
      idx.body.isNewlyCreated should be (false)

      deleted.header.responseCode should be (200)
      deleted.body.id should be (created.body.id)

      ls2.body.indexes.map(_.`type`) should not contain (Index.Type.hash)
    }
  }

  it should "create named" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createHash(List("name"), name = Some("indextest"))
      ls      <- collection.indexes.list()
      deleted <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.hash)
      created.body.isNewlyCreated should be (true)
      created.body.name should be("indextest")

      ls.body.indexes.map(_.name) should contain ("indextest")

      deleted.header.responseCode should be (200)
      deleted.body.id should be (created.body.id)
    }
  }

  it should "skiplist" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createSkipList(List("name"))
      _       <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.skiplist)
    }
  }

  it should "persistent" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createPersistent(List("name"))
      _       <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.persistent)
    }
  }

  it should "geo" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createGeo(List("name"))
      _       <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.geo)
    }
  }

  it should "fulltext" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createFullText(List("name"))
      _       <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.fulltext)
    }
  }

  it should "ttl" in { arango =>
    val collection = arango.db.collection(collectionName)

    for {
      created <- collection.indexes.createTtl(List("name"), expireAfter = 60)
      _       <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.ttl)
    }
  }
}
