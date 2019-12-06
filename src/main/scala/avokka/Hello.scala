package avokka

import akka.actor._
import akka.stream._
import avokka.arangodb.{Database, Session}

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val session = new Session("bak")
    val auth = session.authenticate("root", "root")

    val db = new Database(session, "v10")

    println(Await.result(auth.value, 10.seconds))
    println(Await.result(db.collections(), 10.seconds))

    Await.ready(system.terminate(), 1.minute)
  }

}
