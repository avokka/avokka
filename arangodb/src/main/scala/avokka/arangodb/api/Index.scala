package avokka.arangodb
package api

import avokka.velocypack._

object Index {

  final case class Response(
      fields: List[String],
      id: String,
      name: String,
      `type`: String,
      isNewlyCreated: Boolean = false,
      selectivityEstimate: Option[Double] = None,
      sparse: Option[Boolean] = None,
      unique: Option[Boolean] = None,
      deduplicate: Option[Boolean] = None,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

}
