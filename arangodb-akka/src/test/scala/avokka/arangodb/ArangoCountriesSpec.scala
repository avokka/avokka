package avokka.arangodb

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import avokka.arangodb.api._
import avokka.arangodb.types._
import avokka.velocypack._
import cats.data.EitherT
import cats.instances.future._
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class ArangoCountriesSpec
    extends AsyncFlatSpec
    with TestKitBase
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer {
  import ArangoCountriesSpec._

  override implicit lazy val system: ActorSystem = ActorSystem("arangodb-countries")

  override val container = ArangodbContainer.Def().start()

  val dbName = DatabaseName("test")

  val session: ArangoSession = new ArangoSession(container.configuration.copy(database = dbName))

  val collName = CollectionName("countries")

  val collection = new ArangoCollection(session.db, collName)

  it should "have test database" in {
    EitherT(session(DatabaseList())).map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.result should contain(dbName)
    }.rethrowT
  }

  it should "have a countries collection" in {
    EitherT(session.db(CollectionList())).map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.result.map(_.name) should contain(collName)
    }.rethrowT
  }

  it should "have france at key FR" in {
    EitherT(session.db(DocumentRead[Country](DocumentHandle(collName, DocumentKey("FR"))))).map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.name should be("France")
    }.rethrowT
  }

  it should "have france at key FR (collection method)" in {
    EitherT(session.db(collection.read[Country](DocumentKey("FR")))).map { res =>
      res.header.responseCode should be(200)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.name should be("France")
    }.rethrowT
  }

  it should "create, read, update, delete document" in {
    val key = DocumentKey("XX")
    (for {
      c <- EitherT(session.db(collection.create(Country(key, name = "country name"), returnNew = true)))
      r <- EitherT(session.db(collection.read[Country](key)))
      u <- EitherT(session.db(collection.update[Country, VObject](key, VObject("name" -> "updated name".toVPack))))
      d <- EitherT(session.db(collection.remove[Country](key)))
    } yield {
      c.header.responseCode should be(202)
      c.header.`type` should be(MessageType.ResponseFinal)
      c.body._id should be(collection.handle(key))
      c.body.`new`.value._rev.repr should not be (empty)

      r.header.responseCode should be(200)
      r.body._key should be(key)

      u.header.responseCode should be(202)
      u.body._oldRev.value should be(r.body._rev)
      u.body._rev should not be (r.body._rev)

      d.header.responseCode should be(202)
      d.body._rev should be(u.body._rev)
    }).rethrowT
  }

  it should "create multiple documents" in {
    val key1 = DocumentKey("X1")
    val key2 = DocumentKey("X2")
    EitherT(session.db(DocumentCreateMulti(collection.name, List(
      Country(key1, name = "country 1"), Country(key2, name = "country 2")
    )))).map { res =>
      res.header.responseCode should be(202)
      res.header.`type` should be(MessageType.ResponseFinal)
      res.body.size should be(2)
      res.body.map(_._key) should contain(key1)
      res.body.map(_._key) should contain(key2)
    }.rethrowT
  }

  it should "query with cursor batch size" in {
    (for {
      cursor <- EitherT(session.db(Cursor[VObject, Country](
        query = "FOR c IN @@col LIMIT @limit RETURN c",
        bindVars = VObject(
          "limit" -> 10.toVPack,
          "@col" -> collection.name.toVPack
        ),
        batchSize = Some(6)
      )))
      next <- EitherT(session.db(CursorNext[Country](cursor.body.id.get)))
    } yield {
      cursor.header.responseCode should be(201)
      cursor.body.result.size should be(6)
      cursor.body.hasMore should be(true)

      next.header.responseCode should be(200)
      next.body.result.size should be(4)
      next.body.hasMore should be(false)
    }).rethrowT
  }

  override def afterAll(): Unit = {
    session.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}

object ArangoCountriesSpec {
  case class Country(
      _key: DocumentKey = DocumentKey.empty,
      _rev: DocumentRevision = DocumentRevision.empty,
      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackRecord[Country].encoder
  implicit val countryDecoder: VPackDecoder[Country] = VPackRecord[Country].decoder
}
