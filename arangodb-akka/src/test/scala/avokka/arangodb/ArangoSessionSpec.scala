package avokka.arangodb

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import avokka.arangodb.protocol.ArangoError
import avokka.arangodb.types._
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class ArangoSessionSpec
    extends AsyncFlatSpec
    with TestKitBase
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer {

  override implicit lazy val system: ActorSystem = ActorSystem("arangodb-session")

  override val container = ArangodbContainer.Def().start()

  val arango: ArangoSession = ArangoSession(container.configuration)
  val client = arango.server

  it should "get version" in {
    client.version().map { res =>
      res.header.responseCode should be (200)
      res.body.version should startWith (container.version)
    }
  }

  it should "get version with details" in {
    client.version(details = true).map { res =>
      res.header.responseCode should be (200)
      res.body.details should not be (empty)
    }
  }

  it should "have a _system and test database" in {
    client.databases().map { res =>
      res.header.responseCode should be (200)
      res.body.result should contain (DatabaseName.system)
      res.body.result should contain (DatabaseName("test"))
    }
  }

  val scratchName = DatabaseName("scratch")
  val scratch = arango.database(scratchName)

  it should "create, read and drop a database" in {
    for {
      created <- scratch.create()
      listed  <- client.databases()
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

  it should "fail creating database with invalid name" in {
    recoverToExceptionIf[ArangoError.Resp] {
      arango.database(DatabaseName("@")).create()
    }.map { e =>
      e.header.responseCode should be (400)
      e.error.errorNum should be (1229)
    }
  }

  val test = arango.database(DatabaseName("test"))
  val tempName = CollectionName("temp")
  val temp = test.collection(tempName)

  it should "create, read and drop a collection" in {
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

  override def afterAll(): Unit = {
    arango.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}
