package avokka

import java.time.Instant

import akka.actor._
import akka.stream._
import avokka.arangodb._
import avokka.arangodb.api._
import avokka.velocypack._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  case class Country
  (
    _id: DocumentHandle = DocumentHandle.empty,
    _key: DocumentKey = DocumentKey(""),
    _rev: String = "",
    name: String,
    flag: String,
  )
  implicit val countryEncoder: VPackEncoder[Country] = VPackRecord[Country].encoder //codecWithDefaults
  implicit val countryDecoder: VPackDecoder[Country] = VPackRecord[Country].decoderWithDefaults //codecWithDefaults

  case class Photo
  (
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
    val auth = session.authenticate("root", "root")

    val db = new Database(session, "v10")
    val countries = new Collection(db, "countries")

    println(Await.result(auth, 10.seconds))
//    println(Await.result(countries(CollectionChecksum()), 10.seconds))
//    println(Await.result(countries(CollectionCount), 10.seconds))
//    println(Await.result(db(Engine), 10.seconds))
//    println(Await.result(db.engine(), 10.seconds))
//    println(Await.result(session.databases(), 10.seconds))
//    println(Await.result(db.collections(), 10.seconds))
//    println(Await.result(db.collection("nope"), 10.seconds))
//    println(Await.result(db.document[Country]("countries/FR"), 10.seconds))
//    println(Await.result(countries.document[Country]("FR"), 10.seconds))
//    println(Await.result(countries.properties(), 10.seconds))

/*
    println(Await.result(db(Cursor[Map[String, Int], Photo](
      query = "FOR p IN photos LIMIT @limit RETURN p",
      bindVars = Map("limit" -> 1)
    )), 10.seconds))
*/

    val scratch = new Database(session, "scratch")
    val country = new Collection(scratch, "country")

    println(Await.result(session(DatabaseCreate(scratch.name)), 1.minute))
    println(Await.result(scratch(CollectionCreate(name = country.name)), 1.minute))
    println(Await.result(country(DocumentCreate(Country(name = "Moi", flag = "[X]"))), 1.minute))
    println(Await.result(country(CollectionDrop()), 1.minute))
    println(Await.result(scratch(DatabaseDrop), 1.minute))

    Await.ready(system.terminate(), 1.minute)
  }

}
