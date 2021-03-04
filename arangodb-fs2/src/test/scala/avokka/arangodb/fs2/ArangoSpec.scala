package avokka.arangodb.fs2

import avokka.arangodb.protocol.ArangoError
import avokka.arangodb.types.{CollectionName, DatabaseName}
import avokka.test.ArangodbContainer
import cats.effect._
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ArangoSpec
    extends FixtureAsyncFlatSpec
    with AsyncIOSpec
    with CatsResourceIO[Arango[IO]]
    with Matchers
    with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()
  override val resource = Arango[IO](container.configuration)

  it should "get version" in { arango =>
    arango.server.version().map { res =>
      res.header.responseCode should be (200)
      res.body.version should startWith (container.version)
    }
  }

  it should "get version with details" in { arango =>
    arango.server.version(details = true).map { res =>
      res.header.responseCode should be (200)
      res.body.details should not be (empty)
    }
  }

  it should "have a _system and test database" in { arango =>
    arango.server.databases().map { res =>
      res.header.responseCode should be (200)
      res.body.result should contain (DatabaseName.system)
      res.body.result should contain (DatabaseName("test"))
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
    } yield {
      created.header.responseCode should be (201)
      created.body.result should be (true)

      listed.body.result should contain (scratchName)

      info.header.responseCode should be (200)
      info.body.result.name should be (scratchName)

      dropped.header.responseCode should be(200)
      dropped.body.result should be (true)
    }
  }

  it should "fail creating database with invalid name" in { arango =>
    arango.database(DatabaseName("@")).create().redeem({
      case e: ArangoError.Response =>
        e.header.responseCode should be (400)
        e.error.errorNum should be (1229)
      case e => fail(e)
    }, { r =>
      fail(s"Expected a ArangoError.Response but received: $r")
    })
  }

  it should "create, read and drop a collection" in { arango =>
    val test = arango.database(DatabaseName("test"))
    val tempName = CollectionName("temp")
    val temp = test.collection(tempName)

    for {
      created <- temp.create()
      listed  <- test.collections()
      info    <- temp.info()
      dropped <- temp.drop()
    } yield {
      created.header.responseCode should be (200)
      created.body.name should be (tempName)

      listed.body.result.map(_.name) should contain (tempName)

      info.header.responseCode should be (200)
      info.body.name should be (tempName)

      dropped.header.responseCode should be(200)
      dropped.body.id should be (created.body.id)
    }
  }

}
