package avokka.arangodb

import avokka.arangodb.fs2._
import avokka.arangodb.models.GraphEdgeDefinition
import avokka.arangodb.protocol.{ArangoError, ArangoErrorNum}
import avokka.arangodb.types._
import avokka.velocypack._
import avokka.velocypack.circe._
import cats.effect._
import io.circe.literal._

class ArangoGraphSpec extends ArangoIOBase {

  val graphName: GraphName = GraphName("grf")

  def graph(arango: Arango[IO]): ArangoGraph[IO] = arango.db.graph(graphName)

  val neighName: CollectionName = CollectionName("neigh")
  val countriesName: CollectionName = CollectionName("countries")

  it should "create, read and drop a graph" in { arango =>
    val tempName = GraphName("gtemp")
    val temp = arango.db.graph(tempName)

    for {
      created <- temp.create(edgeDefinitions = GraphEdgeDefinition(
        neighName, from = List(countriesName), to = List(countriesName)
      ) :: Nil)
      listed  <- arango.db.graphs()
      info    <- temp.info()
      vColls  <- temp.vertexCollections()
      dropped <- temp.drop()
    } yield {
      created.header.responseCode should be (202)
      created.body.name should be (tempName)

      listed.body.map(_.name) should contain (tempName)

      info.header.responseCode should be (200)
      info.body.name should be (tempName)

      vColls.header.responseCode should be (200)
      vColls.body should not be (empty)
      vColls.body should contain (countriesName)

      dropped.header.responseCode should be(202)
      dropped.body should be (true)
    }
  }

  it should "add and remove vertex collection" in { arango =>
    val graph = arango.db.graph(graphName)
    val vertex = CollectionName("other")
    for {
      _ <- graph.create()
      added <- graph.addVertexCollection(vertex)
      removed <- graph.removeVertexCollection(vertex)
      _ <- graph.drop()
    } yield {
      added.header.responseCode should be (202)
      added.body.orphanCollections should contain (vertex)

      removed.header.responseCode should be (202)
      removed.body.orphanCollections should not contain (vertex)
    }
  }

}
