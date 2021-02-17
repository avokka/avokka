package avokka.arangodb

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import avokka.arangodb.api._
import avokka.arangodb.types._
import cats.data.EitherT
import cats.instances.future._
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

  val session: ArangoSession = new ArangoSession(container.configuration)
  val client = ArangoClient(session)

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
      res.body.result should contain (test.name)
    }
  }

  val scratchName = DatabaseName("scratch")
  val scratch = client.database(scratchName)

  /*
  it should "create, read and drop a database" in {
    (for {
      created <- EitherT(session(DatabaseCreate(scratchName)))
      listed  <- EitherT(session(DatabaseList()))
      info    <- EitherT(scratch(DatabaseInfo))
      dropped <- EitherT(scratch(DatabaseDrop))
    } yield {
      created.header.responseCode should be (201)
      created.body.result should be (true)

      listed.body.result should contain (scratchName)

      info.header.responseCode should be (200)
      info.body.result.name should be (scratchName)

      dropped.header.responseCode should be(200)
      dropped.body.result should be (true)
    }).rethrowT
  }

  it should "fail creating database with invalid name" in {
    recoverToExceptionIf[ArangoError.Resp] {
      EitherT(session(DatabaseCreate(DatabaseName("@")))).rethrowT
    }.map { e =>
      e.header.responseCode should be (400)
      e.error.errorNum should be (1229)
    }
  }
*/
  val test = client.database(DatabaseName("test"))
  val temp = scratch.collection(CollectionName("temp"))

  /*
  it should "create, read and drop a collection" in {
    (for {
      created <- EitherT(test(CollectionCreate(temp.name)))
      listed  <- EitherT(test(CollectionList()))
      info    <- EitherT(test(CollectionInfo(temp.name)))
      dropped <- EitherT(test(CollectionDrop(temp.name)))
    } yield {
      created.header.responseCode should be (200)
      created.body.name should be (temp.name)

      listed.body.result.map(_.name) should contain (temp.name)

      info.header.responseCode should be (200)
      info.body.name should be (temp.name)

      dropped.header.responseCode should be(200)
      dropped.body.id should be (created.body.id)
    }).rethrowT
  }
  */

  override def afterAll(): Unit = {
    session.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}
