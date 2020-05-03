import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import avokka.arangodb.api.Version
import avokka.arangodb._
import avokka.velocypack._
import cats.syntax.traverse._
import cats.syntax.show._
import cats.instances.vector._
import cats.instances.future._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object StreamNetFailTest {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val session = new ArangoSession(ArangoConfiguration.load())
    val db = new ArangoDatabase(session, DatabaseName("v10"))
    val countries = new ArangoCollection(db, CollectionName("countries"))

    def readCountryById(key: String): Future[Either[ArangoError, ArangoResponse[VPack.VObject]]] = {
      db(countries.read[VPack.VObject](DocumentKey(key)))
    }

    val version = session(Version())
    val versionDetails = session(Version(details = true))
    val fr = readCountryById("FR")

    val ls = Vector.fill(10)("FR").traverse(readCountryById)

    val r = for {
      v <- version
      vd <- versionDetails
      f <- fr
      a <- ls
    } yield (a.map(_.map(_.body.show)), v, f, vd)
    println(Await.result(r, 1.minute))

    //println(Await.result(version, 10.seconds))
    //println(Await.result(versionDetails, 10.seconds))

    session.closeClient()

    Await.ready(system.terminate(), 1.minute)

  }
}
