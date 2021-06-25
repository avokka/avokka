package avokka.arangodb

class ArangoWalSpec extends ArangoIOBase {

  it should "wal tail" in { arango =>
    arango.db.wal.tail().map { res =>
      res.header.responseCode should be (200)
      res.body should not be empty
    }
  }

}
