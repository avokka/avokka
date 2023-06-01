package avokka.arangodb

import avokka.arangodb.protocol.{ArangoError, ArangoErrorNum}
import avokka.arangodb.types._

class ArangoDatabaseSpec extends ArangoIOBase {

  val collectionName: CollectionName = CollectionName("countries")

  it should "info" in { arango =>
    arango.db.info().map { res =>
      res.header.responseCode should be (200)
      res.body.name should be (arango.db.name)
      res.body.id should not be (empty)
    }
  }

  it should "create, read and drop a database" in { arango =>
    val scratchName = DatabaseName("scratch")
    val scratch = arango.database(scratchName)

    for {
      created <- scratch.create()
      listed  <- arango.server.databases()
      info    <- scratch.info()
      dropped <- scratch.drop()
      after   <- arango.server.databases()
    } yield {
      created.header.responseCode should be (201)
      created.body should be (true)

      listed.body should contain (scratchName)

      info.header.responseCode should be (200)
      info.body.name should be (scratchName)

      dropped.header.responseCode should be(200)
      dropped.body should be (true)

      after.body should not contain (scratchName)
    }
  }

  it should "fail creating database with invalid name" in { arango =>
    arango.database(DatabaseName("@")).create().redeem({
      case e: ArangoError.Response =>
        e.code should be (400)
        e.num should be (ArangoErrorNum.ARANGO_DATABASE_NAME_INVALID)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Response but received: $r")
    })
  }
}
