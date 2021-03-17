package avokka.arangodb

import avokka.arangodb.models._
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import avokka.test.ArangodbContainer
import cats.effect._
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ArangoTransactionSpec
    extends FixtureAsyncFlatSpec
    with AsyncIOSpec
    with CatsResourceIO[Arango[IO]]
    with Matchers
    with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()
  override val resource = Arango[IO](container.configuration)

  val databaseName = DatabaseName("test")
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
