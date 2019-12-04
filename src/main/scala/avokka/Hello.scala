package avokka

import akka.actor._
import akka.stream._
import avokka.arangodb.AvokkaDatabase
import avokka.velocystream._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val session = new VSession("bak")
    val db = new AvokkaDatabase(session)

    val auth = session.authenticate("root", "root")

    val version = db.apiVersion()

    println(Await.result(auth, 10.seconds))
    println(Await.result(version, 10.seconds))
    println(Await.result(db.collections(), 10.seconds))

    Await.ready(system.terminate(), 1.minute)
  }

}
