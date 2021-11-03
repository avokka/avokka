package avokka.arangodb

class ArangoWalSpec extends ArangoIOBase {

  it should "wal tail" in { arango =>
    // arango 3.6 fails to generate a dump response
    withArangoVersion("3.7.0") {
      arango.db.wal.tail().map { res =>
        res.header.responseCode should be(200)
        res.body should not be empty
      }
    }
  }

}
