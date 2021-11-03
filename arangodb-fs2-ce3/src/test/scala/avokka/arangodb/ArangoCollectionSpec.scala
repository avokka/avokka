package avokka.arangodb

import avokka.arangodb.fs2._
import avokka.arangodb.models.{CollectionSchema, CollectionStatus}
import avokka.arangodb.protocol.{ArangoError, ArangoErrorNum}
import avokka.arangodb.types._
import avokka.velocypack._
import avokka.velocypack.circe._
import cats.effect._
import io.circe.literal._

class ArangoCollectionSpec extends ArangoIOBase {

  val collectionName = CollectionName("countries")

  def collection(arango: Arango[IO]): ArangoCollection[IO] = arango.db.collection(collectionName)

  it should "create, read and drop a collection" in { arango =>
    val tempName = CollectionName("temp")
    val temp = arango.db.collection(tempName)

    for {
      created <- temp.create()
      listed  <- arango.db.collections()
      info    <- temp.info()
      dropped <- temp.drop()
    } yield {
      created.header.responseCode should be (200)
      created.body.name should be (tempName)

      listed.body.map(_.name) should contain (tempName)

      info.header.responseCode should be (200)
      info.body.name should be (tempName)

      dropped.header.responseCode should be(200)
      dropped.body.id should be (created.body.id)
    }
  }

  it should "checksum" in { arango =>
    collection(arango).checksum().map { res =>
      res.header.responseCode should be (200)
      res.body.checksum should not be (empty)
    }
  }

  it should "count" in { arango =>
    collection(arango).documents.count().map { res =>
      res.header.responseCode should be (200)
      res.body.count should be > 200L
    }
  }

  it should "revision" in { arango =>
    collection(arango).revision().map { res =>
      res.header.responseCode should be (200)
      res.body.revision should not be (empty)
    }
  }

  it should "properties" in { arango =>
    collection(arango).properties().map { res =>
      res.header.responseCode should be (200)
      res.body.name should be (collectionName)
    }
  }

  it should "truncate" in { arango =>
    val tempName = CollectionName("temp")
    val temp = arango.db.collection(tempName)

    for {
      _ <- temp.create()
      _ <- temp.documents.insert(VObject.empty, waitForSync = true)
      a <- temp.documents.count()
      t <- temp.truncate()
      b <- temp.documents.count()
      _ <- temp.drop()
    } yield {
      a.body.count should be (1L)

      t.header.responseCode should be (200)
      t.body.name should be (tempName)

      b.body.count should be (0)
    }
  }

  it should "unload, load" in { arango =>
    val tempName = CollectionName("temp")
    val temp = arango.db.collection(tempName)

    for {
      _ <- temp.create()
      u <- temp.unload()
      l <- temp.load()
      _ <- temp.drop()
    } yield {
      u.header.responseCode should be (200)
      u.body.name should be (tempName)
      u.body.status should be (CollectionStatus.Unloaded)

      l.header.responseCode should be (200)
      l.body.name should be (tempName)
      l.body.status should be (CollectionStatus.Loaded)
    }
  }

  it should "rename" in { arango =>
    val tempName = CollectionName("temp")
    val temp = arango.db.collection(tempName)
    val temp2Name = CollectionName("temp2")
    val temp2 = arango.db.collection(temp2Name)

    for {
      _ <- temp.create()
      r <- temp.rename(temp2Name)
      _ <- temp2.drop()
    } yield {
      r.header.responseCode should be (200)
      r.body.name should be (temp2Name)
    }
  }

  it should "support schema" in { arango =>
    assumeArangoVersion("3.7.1")

    val tempName = CollectionName("temp")
    val temp = arango.db.collection(tempName)

    for {
      c <- temp.create(_.copy(
        schema = Some(CollectionSchema(
          rule = json"""
                {
                  "properties": { "nums": { "type": "array", "items": { "type": "number", "maximum": 6 } } },
                  "additionalProperties": { "type": "string" },
                  "required": ["nums"]
                }
                """.toVPack,
          level = "moderate",
          message = "The document does not contain an array of numbers in attribute 'nums', or one of the numbers is bigger than 6."
        ))
      ))

      _ <- temp.documents.insert(VObject(
        "x" -> VString("additional")
      )).redeem({
        case e: ArangoError.Response =>
          e.code should be (400)
          e.num should be (ArangoErrorNum.VALIDATION_FAILED)
        case e => fail(e)
      }, { r => fail(s"Expected a ArangoError.Response but received: $r") })

      _ <- temp.documents.insert(VObject(
        "nums" -> VTrue
      )).redeem({
        case e: ArangoError.Response =>
          e.code should be (400)
          e.num should be (ArangoErrorNum.VALIDATION_FAILED)
        case e => fail(e)
      }, { r => fail(s"Expected a ArangoError.Response but received: $r") })

      _ <- temp.documents.insert(VObject(
        "nums" -> VArray(VLong(100))
      )).redeem({
        case e: ArangoError.Response =>
          e.code should be (400)
          e.num should be (ArangoErrorNum.VALIDATION_FAILED)
        case e => fail(e)
      }, { r => fail(s"Expected a ArangoError.Response but received: $r") })

      i <- temp.documents.insert(VObject(
        "nums" -> VArray(VLong(5), VSmallint(0)),
        "x" -> VString("additional")
      ))

      _ <- temp.drop()
    } yield {
      c.header.responseCode should be (200)
      i.header.responseCode should be (202)
    }
  }
}
