package avokka.arangodb
package models

import avokka.velocypack._
import enumeratum._

final case class Index(
    fields: List[String],
    id: String,
    name: String,
    `type`: Index.Type,
    isNewlyCreated: Boolean = false,
    selectivityEstimate: Option[Double] = None,
    sparse: Option[Boolean] = None,
    unique: Option[Boolean] = None,
    deduplicate: Option[Boolean] = None,
)

object Index {

  sealed trait Type extends EnumEntry
  object Type extends Enum[Type] with VPackEnum[Type] {

    case object primary extends Type
    case object hash extends Type
    case object skiplist extends Type
    case object persistent extends Type
    case object geo extends Type
    case object geo1 extends Type
    case object geo2 extends Type
    case object fulltext extends Type
    case object edge extends Type
    case object ttl extends Type

    override val values = findValues
  }

  implicit val decoder: VPackDecoder[Index] = VPackDecoder.gen
}
