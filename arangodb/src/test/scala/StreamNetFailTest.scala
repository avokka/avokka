import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import avokka.arangodb.api.Version
import avokka.arangodb._
import avokka.velocypack._
import com.typesafe.config.ConfigFactory
import scodec.bits.BitVector

import scala.concurrent.Await
import scala.concurrent.duration._

object StreamNetFailTest {
  /*
  val bytes = ArangoRequest.Header(
    database = DatabaseName.system,
    requestType = RequestType.GET,
    request = "/_api/version",
  ).toVPackBits.getOrElse(BitVector.empty).bytes
*/
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val session = new ArangoSession(ArangoConfiguration(ConfigFactory.load()).copy(port = 80))

    val version = session(Version())
    val versionDetails = session(Version(details = true))

    import system.dispatcher

    val r = for {
      v <- version
      vd <- versionDetails
    } yield (v, vd)
    println(Await.result(r, 10.seconds))

    //println(Await.result(version, 10.seconds))
    //println(Await.result(versionDetails, 10.seconds))

    Await.ready(system.terminate(), 1.minute)

  }
}
