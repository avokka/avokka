package avokka.arangodb

import _root_.akka.actor.ActorSystem
import _root_.akka.testkit.{TestKit, TestKitBase}
import avokka.arangodb.akka.Arango
import avokka.arangodb.protocol.ArangoError
import avokka.arangodb.types.*
import com.dimafeng.testcontainers.scalatest.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import avokka.test.*
import com.dimafeng.testcontainers.GenericContainer

import scala.concurrent.Future

class ArangoSpec
    extends AsyncFlatSpec
    with TestKitBase
    with Matchers
    with BeforeAndAfterAll
    with TestContainerForAll {

  override implicit lazy val system: ActorSystem = ActorSystem("arangodb-session")

  override val containerDef: GenericContainer.Def[ArangodbContainer] = ArangodbContainer.Def()

  def withArango[A](f: (Containers, Arango, ArangoServer[Future]) => A) = withContainers { container =>
    val arango: Arango = Arango(container.configuration)
    val client = arango.server
    f(container, arango, client)
  }

  it should "get version" in withArango { (container, arango, client) =>
    client.version().map { res =>
      res.header.responseCode should be (200)
      res.body.version should startWith (container.version)
    }
  }

  it should "get version with details" in withArango { (container, arango, client) =>
    client.version(details = true).map { res =>
      res.header.responseCode should be (200)
      res.body.details should not be (empty)
    }
  }

  it should "have a _system and test database" in withArango { (container, arango, client) =>
    client.databases().map { res =>
      res.header.responseCode should be (200)
      res.body should contain (DatabaseName.system)
      res.body should contain (DatabaseName("test"))
    }
  }

  val scratchName = DatabaseName("scratch")

  it should "create, read and drop a database" in withArango { (container, arango, client) =>
    val scratch = arango.database(scratchName)
    for {
      created <- scratch.create()
      listed  <- client.databases()
      info    <- scratch.info()
      dropped <- scratch.drop()
    } yield {
      created.header.responseCode should be (201)
      created.body should be (true)

      listed.body should contain (scratchName)

      info.header.responseCode should be (200)
      info.body.name should be (scratchName)

      dropped.header.responseCode should be(200)
      dropped.body should be (true)
    }
  }

  it should "fail creating database with invalid name" in withArango { (container, arango, client) =>
    recoverToExceptionIf[ArangoError.Response] {
      arango.database(DatabaseName("@")).create()
    }.map { e =>
      e.header.responseCode should be (400)
      e.error.errorNum should be (1229)
    }
  }

  val tempName = CollectionName("temp")

  it should "create, read and drop a collection" in withArango { (container, arango, client) =>
    val test = arango.database(DatabaseName("test"))
    val temp = test.collection(tempName)
    for {
      created <- temp.create()
      listed  <- test.collections()
      info    <- temp.info()
      dropped <- temp.drop()
    } yield {
      created.header.responseCode should be (200)
      created.body.name should be (tempName)

      listed.body.map(_.name) should contain (tempName)

      info.header.responseCode should be (200)
      info.body.name should be (tempName)

      dropped.header.responseCode should be(200)
      dropped.body.id should be (created.body.id)
    }
  }

  override def afterAll(): Unit = withArango { (container, arango, client) =>
    arango.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}
