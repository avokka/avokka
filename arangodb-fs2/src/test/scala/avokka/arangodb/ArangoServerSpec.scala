package avokka.arangodb

import types._
import fs2._
import avokka.test.ArangodbContainer
import cats.effect._
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ArangoServerSpec
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

  it should "role" in { arango =>
    arango.server.role().map { res =>
      res.header.responseCode should be (200)
      res.body.role should be ("SINGLE")
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

}
