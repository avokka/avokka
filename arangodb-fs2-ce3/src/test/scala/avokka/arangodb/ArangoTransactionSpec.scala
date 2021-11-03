package avokka.arangodb

import avokka.arangodb.types._

class ArangoTransactionSpec extends ArangoIOBase {

  val collectionName = CollectionName("countries")

  it should "begin, commit" in { arango =>
    for {
      trx <- arango.db.transactions.begin(read = List(collectionName))
      st  <- trx.status()
      c   <- trx.commit()
    } yield {
      trx.id.repr should not be (empty)

      st.body.id should be (trx.id)
      st.body.status should be ("running")

      c.body.id should be (trx.id)
      c.body.status should be ("committed")
    }
  }

  it should "begin, abort" in { arango =>
    for {
      trx <- arango.db.transactions.begin(read = List(collectionName))
      st  <- trx.status()
      c   <- trx.abort()
    } yield {
      trx.id.repr should not be (empty)

      st.body.id should be (trx.id)
      st.body.status should be ("running")

      c.body.id should be (trx.id)
      c.body.status should be ("aborted")
    }
  }

  it should "list" in { arango =>
    for {
      trx <- arango.db.transactions.begin(read = List(collectionName))
      ls  <- arango.db.transactions.list()
      _   <- trx.abort()
    } yield {
      trx.id.repr should not be (empty)

      ls.body.transactions.map(_.id) should contain (trx.id)
    }
  }
}
