package avokka.arangodb

import avokka.velocypack._
import cats.syntax.either._
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec.bits.BitVector

case class Response[T]
(
  header: Response.Header,
  body: T
)

object Response {

  case class Header
  (
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

  def decode[T](bits: BitVector)(implicit bodyDecoder: VPackDecoder[T]): Either[VPackError, Response[T]] = {
    println("response header slice", toSlice(bits).toString)
    bits.as[Header].flatMap { header =>
      println("response header vpack", header.value)
      println("response body slice", toSlice(header.remainder).toString)
      if (header.value.responseCode >= 400) {
        header.remainder.as[ResponseError].flatMap(_.value.asLeft)
      }
      else {
        println("response body raw", header.remainder.bytes.toHex)
        println("response body vpack", codecs.vpackDecoder.decode(header.remainder))
        header.remainder.as[T].flatMap(body => Response(header.value, body.value).asRight)
      }
    }
    /*
    for {
      header <- bits.fromVPack[ResponseHeader]
      body   <- header.remainder.fromVPack[T]
    } yield Response(header.value, body.value)
    */

  }

}
