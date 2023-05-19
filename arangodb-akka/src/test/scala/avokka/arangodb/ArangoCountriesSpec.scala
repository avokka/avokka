package avokka.arangodb

import avokka.test.*
import _root_.akka.actor.ActorSystem
import _root_.akka.stream.scaladsl.Sink
import _root_.akka.testkit.{TestKit, TestKitBase}
import avokka.arangodb.protocol.MessageType
import avokka.arangodb.types.*
import avokka.velocypack.*
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.*
import com.dimafeng.testcontainers.scalatest.TestContainerForAll

import scala.concurrent.Future

class ArangoCountriesSpec
    extends AsyncFlatSpec
    with TestKitBase
    with Matchers
    with BeforeAndAfterAll
    with TestContainerForAll {
  import ArangoCountriesSpec._

  override implicit lazy val system: ActorSystem = ActorSystem("arangodb-countries")

  override val containerDef: GenericContainer.Def[ArangodbContainer] = ArangodbContainer.Def()

  val dbName = DatabaseName("test")
  val collName = CollectionName("countries")

  def withArango[A](f: (Arango, ArangoDatabase[Future], ArangoCollection[Future]) => A) = withContainers { container =>
    val arango: Arango = Arango(container.configuration.copy(database = dbName))
    val db = arango.db
    val collection = db.collection(collName)
    f(arango, db, collection)
  }

  it should "have test database" in withArango { (arango,_,_) =>
    arango.server.databases().map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body should contain(dbName)
    }
  }

  it should "have a countries collection" in withArango { (_,db,_) =>
    db.collections().map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.map(_.name) should contain(collName)
    }
  }

  it should "have france at key FR" in withArango { (_,db,_) =>
    db.document(DocumentHandle(collName, DocumentKey("FR"))).read[Country]().map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.name should be("France")
    }
  }

  it should "have france at key FR (collection method)" in withArango { (_,db,collection) =>
    collection.document(DocumentKey("FR")).read[Country]().map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.name should be("France")
    }
  }

  it should "create, read, update, delete document" in withArango { (_,db,collection) =>
    val key = DocumentKey("XX")
    for {
      c <- collection.documents.insert(Country(key, name = "country name"), returnNew = true)
      r <- collection.document(key).read[Country]()
      u <- collection.document(key).update[Country, VObject](VObject("name" -> "updated name".toVPack))
      d <- collection.document(key).remove[Country]()
    } yield {
      c.header.responseCode should be(202)
      c.header.`type` should be(MessageType.ResponseFinal)
      c.body._id should be(DocumentHandle(collName, key))
      c.body.`new`.value._rev.repr should not be (empty)

      r.header.responseCode should be(200)
      r.body._key should be(key)

      u.header.responseCode should be(202)
      u.body._oldRev.value should be(r.body._rev)
      u.body._rev should not be (r.body._rev)

      d.header.responseCode should be(202)
      d.body._rev should be(u.body._rev)
    }
  }

  it should "create multiple documents" in withArango { (_,db,collection) =>
    val key1 = DocumentKey("X1")
    val key2 = DocumentKey("X2")
    collection.documents.create(List(
      Country(key1, name = "country 1"), Country(key2, name = "country 2")
    )).map { res =>
      res.header.responseCode should be(202)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.size should be(2)
      res.body.map(_._key) should contain(key1)
      res.body.map(_._key) should contain(key2)
    }
  }

  it should "query akka stream" in withArango { (_,db,collection) =>
    val source = collection.all.batchSize(100).stream[Country]

    source.runWith(Sink.seq).map { s =>
      s.length should be > 200
    }
  }

  it should "query with cursor batch size" in withArango { (_,db,collection) =>
    for {
      cursor <- db.query(
        "FOR c IN @@col LIMIT @limit RETURN c",
        bindVars = VObject(
          "limit" -> 10.toVPack,
          "@col" -> collName.toVPack
        )
      ).batchSize(6).cursor[Country]
      next <- cursor.next()
    } yield {
      cursor.header.responseCode should be(201)
      cursor.body.result.size should be(6)
      cursor.body.hasMore should be(true)

      next.header.responseCode should be(200)
      next.body.result.size should be(4)
      next.body.hasMore should be(false)
    }
  }


  override def afterAll(): Unit = {
    // arango.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}

object ArangoCountriesSpec {
  case class Country(
      _key: DocumentKey = DocumentKey.empty,
      _rev: DocumentRevision = DocumentRevision.empty,
      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.derived
  implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.derived
}
