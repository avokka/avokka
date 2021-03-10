package avokka.arangodb

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

class ArangoCollectionSpec
    extends FixtureAsyncFlatSpec
    with AsyncIOSpec
    with CatsResourceIO[(Arango[IO], ArangoDatabase[IO], ArangoCollection[IO])]
    with Matchers
    with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()

  override val resource = Arango[IO](container.configuration.copy(database = DatabaseName("test"))).map { arango =>
    val db = arango.database(DatabaseName("test"))
    val collection = db.collection(CollectionName("countries"))
    (arango, db, collection)
  }

  it should "checksum" in { case (_, _, collection) =>
    collection.checksum().map { res =>
      res.header.responseCode should be (200)
      res.body.checksum should not be (empty)
    }
  }

  it should "count" in { case (_, _, collection) =>
    collection.count().map { res =>
      res.header.responseCode should be (200)
      res.body.count should be > 200L
    }
  }

  it should "revision" in { case (_, _, collection) =>
    collection.revision().map { res =>
      res.header.responseCode should be (200)
      res.body.revision should not be (empty)
    }
  }

  it should "properties" in { case (_, _, collection) =>
    collection.properties().map { res =>
      res.header.responseCode should be (200)
      res.body.name should be (CollectionName("countries"))
    }
  }
}
