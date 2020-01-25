import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import avokka.arangodb.api.Version
import avokka.arangodb._
import avokka.velocypack._
import com.typesafe.config.ConfigFactory
import cats.syntax.traverse._
import cats.syntax.show._
import cats.instances.vector._
import cats.instances.future._

import scala.concurrent.Await
import scala.concurrent.duration._

object StreamNetFailTest {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val session = new ArangoSession(ArangoConfiguration(ConfigFactory.load()))
    val db = new ArangoDatabase(session, DatabaseName("v10"))
    val countries = new ArangoCollection(db, CollectionName("countries"))

    val version = session(Version())
    val versionDetails = session(Version(details = true))
    val fr = db(countries.read[VPack.VObject](DocumentKey("FR")))

    import system.dispatcher
    val ls = Vector("FR", "DE", "IT", "IS", "GB", "DD").traverse(k => db(countries.read[VPack.VObject](DocumentKey(k))))

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
