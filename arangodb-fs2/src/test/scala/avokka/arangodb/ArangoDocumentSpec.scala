package avokka.arangodb

import avokka.arangodb.fs2._
import avokka.arangodb.protocol.{ArangoError, ArangoErrorNum}
import avokka.arangodb.types._
import avokka.velocypack._
import cats.effect._

class ArangoDocumentSpec extends ArangoIOBase {

  val collectionName = CollectionName("countries")
  val usKey = DocumentKey("US")
  val franceKey = DocumentKey("FR")
  val unknownKey = DocumentKey("XX")

  def collection(arango: Arango[IO]): ArangoCollection[IO] = arango.db.collection(collectionName)

  it should "read a document" in { arango =>
    collection(arango).document(franceKey).read[VObject]().map { res =>
      res.header.responseCode should be (200)
      res.body.isEmpty should be (false)
    }
    collection(arango).document(unknownKey).read[VObject]().redeem({
      case e: ArangoError.Response =>
        e.code should be (404)
        e.num should be (ArangoErrorNum.ARANGO_DOCUMENT_NOT_FOUND)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Response but received: $r")
    })
  }

  it should "head a document" in { arango =>
    collection(arango).document(franceKey).head().map { res =>
      res.responseCode should be (200)
    }
    collection(arango).document(unknownKey).head().redeem({
      case e: ArangoError.Header =>
        e.code should be (404)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Header but received: $r")
    })
  }

  it should "replace a document" in { arango =>
    collection(arango).document(usKey).replace[VObject](VObject(
      "name" -> VString("USA")
    )).map { res =>
      res.header.responseCode should be (202)
      res.body._key should be (usKey)
    }
    collection(arango).document(unknownKey).replace[VObject](VObject(
      "name" -> VString("USA")
    )).redeem({
      case e: ArangoError.Response =>
        e.code should be (404)
        e.num should be (ArangoErrorNum.ARANGO_DOCUMENT_NOT_FOUND)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Response but received: $r")
    })
  }

  it should "upsert a document" in { arango =>
    collection(arango).document(usKey).upsert(VObject(
      "name" -> VString("USA")
    )).execute[VObject].map { res =>
      res.header.responseCode should be (202)
      res.body.result should not be (empty)
    }
    collection(arango).document(DocumentKey("USA")).upsert(VObject(
      "name" -> VString("USA")
    )).execute[VObject].map { res =>
      res.header.responseCode should be (201)
      res.body.result should not be (empty)
    }
  }
}
