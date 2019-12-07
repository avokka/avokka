package avokka.arangodb

import avokka.velocypack._
import scodec.Codec
import shapeless.{::, HNil}

case class ResponseHeader
(
  version: Int,
  `type`: MessageType,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)

object ResponseHeader {

  implicit val codec: Codec[ResponseHeader] = VPackGeneric[ResponseHeader].codec(true) /*[
    Int ::
    MessageType ::
    Int ::
    Map[String, String] ::
    HNil
  ].as
*/
}