package avokka.arangodb
package api

import avokka.velocypack._

final case class Index(
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

object Index {
  implicit val decoder: VPackDecoder[Index] = VPackDecoder.gen
}
