package avokka.arangodb

import avokka.arangodb.fs2._
import avokka.arangodb.protocol.ArangoError
import avokka.arangodb.types._
import avokka.test.ArangodbContainer
import cats.effect._
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ArangoDatabaseSpec
    extends FixtureAsyncFlatSpec
    with AsyncIOSpec
    with CatsResourceIO[Arango[IO]]
    with Matchers
    with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()

  val databaseName = DatabaseName("test")
  val collectionName = CollectionName("countries")

  override val resource = Arango[IO](container.configuration.copy(database = databaseName))

  it should "info" in { arango =>
    arango.db.info().map { res =>
      res.header.responseCode should be (200)
      res.body.name should be (databaseName)
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
      created.body.result should be (true)

      listed.body should contain (scratchName)

      info.header.responseCode should be (200)
      info.body.name should be (scratchName)

      dropped.header.responseCode should be(200)
      dropped.body.result should be (true)

      after.body should not contain (scratchName)
    }
  }

  it should "fail creating database with invalid name" in { arango =>
    arango.database(DatabaseName("@")).create().redeem({
      case e: ArangoError.Response =>
        e.code should be (400)
        e.num should be (1229)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Response but received: $r")
    })
  }
}
