package avokka.arangodb

import avokka.arangodb.types.{CollectionName, DatabaseName, DocumentHandle, GraphName}
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import models._
import protocol._

trait ArangoGraph[F[_]] {
  /** graph name */
  def name: GraphName

  /**
    * Create a new graph in the graph module
    *
    * @param edgeDefinitions An array of definitions for the relations of the graph.
    * @param orphanCollections An array of additional vertex collections. Documents within these collections do not have edges within this graph.
    * @param waitForSync define if the request should wait until everything is synced to disc. Will change the success response code.
    * @return new graph information
    */
  def create(
    edgeDefinitions: List[GraphEdgeDefinition] = List.empty,
    orphanCollections: List[String] = List.empty,
    waitForSync: Boolean = false,
  ): F[ArangoResponse[GraphInfo]]

  /** Get graph information */
  def info(): F[ArangoResponse[GraphInfo]]

  /**
    * Drop the graph
    *
    * @param dropCollections Drop collections of this graph as well. Collections will only be dropped if they are not used in other graphs.
    * @return deletion result
    */
  def drop(dropCollections: Boolean = false): F[ArangoResponse[Boolean]]

  /** Lists all vertex collections used in this graph */
  def vertexCollections(): F[ArangoResponse[Vector[CollectionName]]]

  /** Add an additional vertex collection to the graph. */
  def addVertexCollection(collection: CollectionName): F[ArangoResponse[GraphInfo]]

  /**
    * Remove a vertex collection form the graph.
    * @param dropCollection Drop the collection as well. Collection will only be dropped if it is not used in other graphs.
    * @return graph information
    */
  def removeVertexCollection(collection: CollectionName, dropCollection: Boolean = false): F[ArangoResponse[GraphInfo]]

  def collection(collection: CollectionName): ArangoGraphCollection[F]

  def vertex(handle: DocumentHandle): ArangoGraphVertex[F]

  def edge(handle: DocumentHandle): ArangoGraphEdge[F]
}

object ArangoGraph {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, _name: GraphName): ArangoGraph[F] = new ArangoGraph[F] {
    override def name: GraphName = _name

    private val path: String = API_GHARIAL + "/" + _name.repr

    override def create(
      edgeDefinitions: List[GraphEdgeDefinition],
      orphanCollections: List[String],
      waitForSync: Boolean,
    ): F[ArangoResponse[GraphInfo]] =
      POST(
        database,
        API_GHARIAL,
        Map(
          "waitForSync" -> waitForSync.toString,
        )
      ).body(
        VObject(
          "name" -> name.toVPack,
          "edgeDefinitions" -> edgeDefinitions.toVPack,
          "orphanCollections" -> orphanCollections.toVPack
        )
      )
        .execute[F, GraphInfo.Response]
        .map(_.map(_.graph))

    override def info(): F[ArangoResponse[GraphInfo]] =
      GET(database, path)
        .execute[F, GraphInfo.Response]
        .map(_.map(_.graph))

    override def drop(dropCollections: Boolean): F[ArangoResponse[Boolean]] =
      DELETE(database, path, Map("dropCollections" -> dropCollections.toString))
        .execute[F, RemovedResult]
        .map(_.map(_.removed))

    override def vertexCollections(): F[ArangoResponse[Vector[CollectionName]]] =
      GET(database, path + "/vertex")
        .execute[F, GraphCollections]
        .map(_.map(_.collections))


    private val vertexPath: String = path + "/vertex"

    override def addVertexCollection(collection: CollectionName): F[ArangoResponse[GraphInfo]] =
      POST(database, vertexPath)
        .body(VObject("collection" -> collection.toVPack))
        .execute[F, GraphInfo.Response].map(_.map(_.graph))

    override def removeVertexCollection(collection: CollectionName, dropCollection: Boolean): F[ArangoResponse[GraphInfo]] =
      DELETE(
        database,
        vertexPath + "/" + collection.repr,
        Map(
          "dropCollection" -> dropCollection.toString
        )
      ).execute[F, GraphInfo.Response].map(_.map(_.graph))


    override def collection(collection: CollectionName): ArangoGraphCollection[F] = ArangoGraphCollection(database, name, collection)

    override def vertex(handle: DocumentHandle): ArangoGraphVertex[F] = ArangoGraphVertex(database, name, handle)

    override def edge(handle: DocumentHandle): ArangoGraphEdge[F] = ArangoGraphEdge(database, name, handle)
  }
}
