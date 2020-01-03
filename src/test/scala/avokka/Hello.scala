package avokka

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import avokka.velocypack._
import avokka.arangodb._

import scala.concurrent.duration._
import scala.concurrent.Await

object Hello {

  implicit val system: ActorSystem = ActorSystem("avokka")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Country(
                      _id: DocumentHandle = DocumentHandle.empty,
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: String = "",
                      name: String,
                      flag: String,
  )
  implicit val countryEncoder: VPackEncoder[Country] = VPackRecord[Country].encoder
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

    val session = new Session("bak")
    val auth = session(Request.Authentication(user = "root", password = "root"))

    val db = new Database(session, DatabaseName("v10"))
    val countries = new Collection(db, CollectionName("countries"))

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
    println(Await.result(db(countries.read[Country](DocumentKey("FR"))), 10.seconds))
//    println(Await.result(countries.properties(), 10.seconds))
//    println(Await.result(countries(IndexList), 10.seconds))
//    println(Await.result(db(IndexRead("countries/0")), 10.seconds))

    /*
    val res = Await.result(db(
      countries.lookup[Country](List(DocumentKey("DE"), DocumentKey("FR")))
    ), 10.seconds)
    println(res)
*/
    /*
    val res = Await.result(db(Cursor[Map[String, Int], Photo](
      query = "FOR p IN photos LIMIT @limit RETURN p",
      bindVars = Map("limit" -> 10), batchSize = Some(2)
    )), 10.seconds).right.get.body
    println(res)
    println(Await.result(db(CursorNext[Photo](res.id.get)), 10.seconds))
    println(Await.result(db(CursorDelete(res.id.get)), 10.seconds))
*/
    /*
    Await.result(db.source(countries.all[Country].withBatchSize(4))
        .wireTap(println(_))
        .runWith(Sink.ignore)
    , 10.seconds)
     */
    val scratch = new Database(session, DatabaseName("scratch"))
    val country = new Collection(scratch, CollectionName("country"))

    /*
    println(Await.result(session(DatabaseCreate(scratch.name)), 1.minute))
    println(Await.result(scratch(CollectionCreate(name = country.name)), 1.minute))

    if (false) {
      println(Await.result(country(DocumentCreateMulti(List(Country(name = "a", flag = "a"), Country(name = "b", flag = "b")), returnNew = true)), 1.minute))
      val doc = Await.result(country(DocumentCreate(Country(name = "Moi", flag = "[X]"), returnNew = true)), 1.minute)
      println(doc)
      val res = doc.right.get.body.`new`.get
      println(Await.result(scratch(DocumentUpdate[Country, VObject](res._id, VObject("test" :> true))), 1.minute))
      println(Await.result(scratch(DocumentReplace[Country](res._id, res.copy(name = "Vous"))), 1.minute))
      println(Await.result(country(DocumentUpdateMulti[Country, VObject](List(VObject("_key" :> res._key, "test" :> true)), returnNew = true)), 1.minute))
      //    println(Await.result(scratch(DocumentRemove[Country](res._id)), 1.minute))
      println(Await.result(country(DocumentRemoveMulti[Country, DocumentKey](List(res._key), returnOld = true)), 1.minute))
      println(Await.result(country(CollectionTruncate), 1.minute))
      println(Await.result(country(CollectionUnload), 1.minute))
    }

    val idx = Await.result(country(IndexHash(fields = List("title"))), 1.minute)
    println(idx)
    println(Await.result(country(IndexList), 10.seconds))
    println(Await.result(scratch(IndexDelete(idx.right.get.body.id)), 1.minute))
    println(Await.result(country(IndexList), 10.seconds))

    println(Await.result(country(CollectionDrop()), 1.minute))
    println(Await.result(scratch(DatabaseDrop), 1.minute))
*/
    Await.ready(system.terminate(), 1.minute)
  }

}
