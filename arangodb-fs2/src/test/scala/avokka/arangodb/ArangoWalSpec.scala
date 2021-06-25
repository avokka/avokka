package avokka.arangodb

class ArangoWalSpec extends ArangoIOBase {

  it should "wal tail" in { arango =>
    assumeArangoVersion("3.7") // arango 3.6 fails to generate a dump response
    arango.db.wal.tail().map { res =>
      res.header.responseCode should be (200)
      res.body should not be empty
    }
  }

}
