package avokka.arangodb

import avokka.velocypack._
import cats.syntax.either._
import com.arangodb.velocypack.{VPack, VPackSlice}
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

  val vp = new VPack.Builder().build()
  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def decode[T](bits: BitVector)(
      implicit bodyDecoder: VPackDecoder[T]
  ): Either[ArangoError, Response[T]] = {
    println("response header slice", toSlice(bits).toString)
    bits.as[Header].leftMap(ArangoError.VPack).flatMap { header =>
      println("response header vpack", header.value)
      if (header.remainder.isEmpty) {
        ArangoError.Head(header.value).asLeft
      } else {
        println("response body slice", toSlice(header.remainder).toString)
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
