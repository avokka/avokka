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
    with CatsResourceIO[Arango[IO]]
    with Matchers
    with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()

  val databaseName = DatabaseName("test")
  val collectionName = CollectionName("countries")

  override val resource = Arango[IO](container.configuration)

  def collection(arango: Arango[IO]): ArangoCollection[IO] = arango.database(databaseName).collection(collectionName)

  it should "checksum" in { arango =>
    collection(arango).checksum().map { res =>
      res.header.responseCode should be (200)
      res.body.checksum should not be (empty)
    }
  }

  it should "count" in { arango =>
    collection(arango).count().map { res =>
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
      res.body.name should be (CollectionName("countries"))
    }
  }
}
