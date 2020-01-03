package avokka.arangodb

import avokka.velocypack.VPack.{VArray, VNone, VString}
import avokka.velocypack._
import cats.Show
import cats.syntax.either._
import scodec.bits.BitVector

case class Response[T](
    header: Response.Header,
    body: T
)

object Response {

  case class Header(
      version: Int,
      `type`: MessageType,
      responseCode: Int,
      meta: Map[String, String] = Map.empty
  )

  object Header {
    implicit val decoder: VPackDecoder[Header] = VPackGeneric[Header].decoder
  }

  def decode[T](bits: BitVector)(
      implicit bodyDecoder: VPackDecoder[T]
  ): Either[ArangoError, Response[T]] = {
    println("response header slice", Show[VPack].show(bits.asVPack.fold(e => VString(e.message), identity)))
    bits.as[Header].leftMap(ArangoError.VPack).flatMap { header =>
      println("response header vpack", header.value)
      if (header.remainder.isEmpty) {
        ArangoError.Head(header.value).asLeft
      } else {
        println("response body slice", Show[VPack].show(header.remainder.asVPack.fold(e => VString(e.message), identity)))
        if (header.value.responseCode >= 400) {
          header.remainder
            .as[ResponseError]
            .leftMap(ArangoError.VPack)
            .flatMap(body => ArangoError.Resp(header.value, body.value).asLeft)
        } else {
          //  println("response body raw", header.remainder.bytes.toHex)
          println("response body vpack", codecs.vpackDecoder.decode(header.remainder))
          header.remainder
            .as[T]
            .leftMap(ArangoError.VPack)
            .flatMap(body => Response(header.value, body.value).asRight)
        }
      }
    }
  }

}
