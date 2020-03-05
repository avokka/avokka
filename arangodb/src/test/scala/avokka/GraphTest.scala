package avokka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import arangodb._
import arangodb.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object GraphTest {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val session = new ArangoSession(ArangoConfiguration.load())
    val db = new ArangoDatabase(session, DatabaseName("v10"))

    println(Await.result(db(GraphList()), 1.minute))
    println(Await.result(db(GraphInfo("likes")), 1.minute))

    session.closeClient()

    Await.ready(system.terminate(), 1.minute)
  }
}
