package avokka

import akka.actor._
import akka.stream._
import avokka.arangodb.api.Cursor
import avokka.arangodb.{Collection, Database, Session}
import avokka.velocypack._
import scodec.Codec

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  case class Country
  (
    _id: String,
    _key: String,
    _rev: String,
    name: String,
    flag: String,
  )

  implicit val countryCodec: Codec[Country] = VPackRecord[Country].codecWithDefaults

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    val session = new Session("bak")
    val auth = session.authenticate("root", "root")

    val db = new Database(session, "v10")
    val countries = new Collection(db, "countries")

    println(Await.result(auth.value, 10.seconds))
//    println(Await.result(session.databases(), 10.seconds))
//    println(Await.result(db.collections(), 10.seconds))
//    println(Await.result(db.collection("nope"), 10.seconds))
//    println(Await.result(db.document[Country]("countries/FR"), 10.seconds))
//    println(Await.result(countries.document[Country]("FR"), 10.seconds))
    println(Await.result(countries.revision(), 10.seconds))

    println(Await.result(db.cursor[VPackObject, String](Cursor(
      query = "FOR c IN countries RETURN c.name",
      bindVars = VPackObject()
    )), 10.seconds))

    Await.ready(system.terminate(), 1.minute)
  }

}
