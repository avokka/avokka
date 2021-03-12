package avokka.arangodb

import avokka.arangodb.api.CollectionStatus
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

  it should "create, read and drop a collection" in { arango =>
    val test = arango.database(databaseName)
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

      listed.body.map(_.name) should contain (tempName)

      info.header.responseCode should be (200)
      info.body.name should be (tempName)

      dropped.header.responseCode should be(200)
      dropped.body.id should be (created.body.id)
    }
  }

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
      res.body.name should be (collectionName)
    }
  }

  it should "truncate" in { arango =>
    val test = arango.database(databaseName)
    val tempName = CollectionName("temp")
    val temp = test.collection(tempName)

    for {
      _ <- temp.create()
      _ <- temp.insert(VObject.empty, waitForSync = true)
      a <- temp.count()
      t <- temp.truncate()
      b <- temp.count()
      _ <- temp.drop()
    } yield {
      a.body.count should be (1L)

      t.header.responseCode should be (200)
      t.body.name should be (tempName)

      b.body.count should be (0)
    }
  }

  it should "unload" in { arango =>
    val test = arango.database(databaseName)
    val tempName = CollectionName("temp")
    val temp = test.collection(tempName)

    for {
      _ <- temp.create()
      u <- temp.unload()
      _ <- temp.drop()
    } yield {
      u.header.responseCode should be (200)
      u.body.name should be (tempName)
      u.body.status should be (CollectionStatus.Unloaded)
    }
  }

  it should "rename" in { arango =>
    val test = arango.database(databaseName)
    val tempName = CollectionName("temp")
    val temp = test.collection(tempName)
    val temp2Name = CollectionName("temp2")
    val temp2 = test.collection(temp2Name)

    for {
      _ <- temp.create()
      r <- temp.rename(temp2Name)
      _ <- temp2.drop()
    } yield {
      r.header.responseCode should be (200)
      r.body.name should be (temp2Name)
    }
  }
}
