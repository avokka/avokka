package avokka

import java.time.Instant

import akka.actor._
import akka.stream._
import avokka.arangodb._
import avokka.arangodb.api._
import avokka.velocypack.VPack._
import avokka.velocypack._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  case class Country(
      _id: DocumentHandle = DocumentHandle.empty,
      _key: DocumentKey = DocumentKey(""),
      _rev: String = "",
      name: String,
      flag: String,
  )
  implicit val countryEncoder: VPackEncoder[Country] = VPackRecord[Country].encoder.mapObject(_.filter {
    case ("_id", DocumentHandle.empty) => false
    case ("_key", VString("")) => false
    case ("_rev", VString("")) => false
    case _ => true
  })
  implicit val countryDecoder: VPackDecoder[Country] = VPackRecord[Country].decoderWithDefaults

  case class Photo(
      _id: DocumentHandle,
      _key: DocumentKey,
      _rev: String,
      title: Option[String] = None,
      slug: Option[String] = None,
      user: Option[String] = None,
      created: Instant,
      deleted: Boolean,
  )
  implicit val commentEncoder: VPackEncoder[Photo] = VPackRecord[Photo].encoder
  implicit val commentDecoder: VPackDecoder[Photo] = VPackRecord[Photo].decoderWithDefaults

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    val session = new Session("bak")
    val auth = session(Request.Authentication(user = "root", password = "root"))

    val db = new Database(session, "v10")
    val countries = new Collection(db, "countries")

    println(Await.result(auth, 10.seconds))
//    println(Await.result(session(Version()), 10.seconds))
//    println(Await.result(session(Version(details = true)), 10.seconds))
//    println(Await.result(session(admin.AdminLog), 10.seconds))
//    println(Await.result(countries(CollectionChecksum()), 10.seconds))
//    println(Await.result(countries(CollectionCount), 10.seconds))
//      println(Await.result(countries(CollectionProperties), 10.seconds))
//    println(Await.result(db(DatabaseInfo), 10.seconds))
//    println(Await.result(db(Engine), 10.seconds))
//    println(Await.result(db.engine(), 10.seconds))
//    println(Await.result(session(DatabaseList()), 10.seconds))
//    println(Await.result(db(CollectionList()), 10.seconds))
//    println(Await.result(db.collection("nope"), 10.seconds))
//    println(Await.result(db(DocumentRead[Country](DocumentHandle("countries/FR"), ifMatch = Some("_ZfKin5f--_"))), 10.seconds))
//    println(Await.result(countries.document[Country]("FR"), 10.seconds))
//    println(Await.result(countries.properties(), 10.seconds))

    val res = Await.result(db(Cursor[Map[String, Int], Photo](
      query = "FOR p IN photos LIMIT @limit RETURN p",
      bindVars = Map("limit" -> 10), batchSize = Some(2)
    )), 10.seconds).right.get.body
    println(res)
    println(Await.result(db(CursorNext[Photo](res.id.get)), 10.seconds))
    println(Await.result(db(CursorDelete(res.id.get)), 10.seconds))

    /*
    val scratch = new Database(session, "scratch")
    val country = new Collection(scratch, "country")

    println(Await.result(session(DatabaseCreate(scratch.name)), 1.minute))
    println(Await.result(scratch(CollectionCreate(name = country.name)), 1.minute))
    println(Await.result(country(DocumentCreateMulti(List(Country(name = "a", flag = "a"), Country(name = "b", flag = "b")), returnNew = true)), 1.minute))
    val doc = Await.result(country(DocumentCreate(Country(name = "Moi", flag = "[X]"), returnNew = true)), 1.minute)
    println(doc)
    val res = doc.right.get.body.`new`.get
    println(Await.result(scratch(DocumentUpdate[Country, VObject](res._id, VObject(Map("test" -> VTrue)))), 1.minute))
//    println(Await.result(scratch(DocumentReplace[Country](res._id, res.copy(name = "Vous"))), 1.minute))
    println(Await.result(country(DocumentUpdateMulti[Country, VObject](List(VObject(Map("_key" -> VString(res._key), "test" -> VTrue))), returnNew = true)), 1.minute))
//    println(Await.result(scratch(DocumentRemove[Country](res._id)), 1.minute))
    println(Await.result(country(DocumentRemoveMulti[Country, DocumentKey](List(res._key), returnOld = true)), 1.minute))
    println(Await.result(country(CollectionTruncate), 1.minute))
    println(Await.result(country(CollectionUnload), 1.minute))
    println(Await.result(country(CollectionDrop()), 1.minute))
    println(Await.result(scratch(DatabaseDrop), 1.minute))
*/
    Await.ready(system.terminate(), 1.minute)
  }

}
