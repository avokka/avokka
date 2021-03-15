package avokka.arangodb

import avokka.arangodb.api.{CollectionStatus, Index}
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import avokka.test.ArangodbContainer
import avokka.velocypack.VObject
import cats.effect._
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ArangoIndexSpec
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

  it should "create, read, delete" in { arango =>
    val test = arango.database(databaseName)
    val collection = test.collection(collectionName)

    for {
      created <- collection.indexes.createHash(List("name"))
      ls      <- collection.indexes.list()
      deleted <- collection.index(created.body.id).delete()
      ls2     <- collection.indexes.list()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.hash)
      created.body.isNewlyCreated should be (true)

      ls.body.indexes.map(_.`type`) should contain (Index.Type.hash)

      deleted.header.responseCode should be (200)
      deleted.body.id should be (created.body.id)

      ls2.body.indexes.map(_.`type`) should not contain (Index.Type.hash)
    }
  }

  it should "create named" in { arango =>
    val test = arango.database(databaseName)
    val collection = test.collection(collectionName)

    for {
      created <- collection.indexes.createHash(List("name"), name = Some("indextest"))
      ls      <- collection.indexes.list()
      deleted <- collection.index(created.body.id).delete()
    } yield {
      created.header.responseCode should be (201)
      created.body.`type` should be (Index.Type.hash)
      created.body.isNewlyCreated should be (true)
      created.body.name should be("indextest")

      ls.body.indexes.map(_.name) should contain ("indextest")

      deleted.header.responseCode should be (200)
      deleted.body.id should be (created.body.id)
    }
  }

}
