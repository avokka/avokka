package avokka.arangodb

import avokka.velocypack._
import cats.data.Validated
import cats.implicits._
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec.{Attempt, Decoder, Err}
import scodec.bits.BitVector
import scodec.interop.cats._

case class Response[T]
(
  header: ResponseHeader,
  body: T
)

object Response {

  val vp = new VPack.Builder().build()
  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def decode[T](bits: BitVector)(implicit bodyDecoder: VPackDecoder[T]): Either[VPackError, Response[T]] = {
    ResponseHeader.decoder.deserializer.decode(bits).flatMap { header =>
      println(toSlice(header.remainder).toString)
      if (header.value.responseCode >= 400) {
        ResponseError.decoder.deserializer.decodeValue(header.remainder).map(_.asLeft[Response[T]])
      } else {
        bodyDecoder.deserializer.decodeValue(header.remainder).map(body =>
          Response(header.value, body).asRight[VPackError]
        )
      }
    }.fold(err => VPackError.Codec(err).asLeft, identity)

    /*
    for {
      header <- bits.fromVPack[ResponseHeader]
      body   <- header.remainder.fromVPack[T]
    } yield Response(header.value, body.value)
    */

    /*

    for {
      header <- ResponseHeader.codec.decode(bits)
      _ = println(toSlice(header.remainder).toString)
      body   <- if (header.value.responseCode >= 400) {
                  ResponseError.codec.decode(header.remainder).flatMap { err =>
                    Attempt.failure(Err(err.value.errorMessage))
                  }
                }
                else bodyDecoder.decode(header.remainder)
    } yield body.map(b => Response(header.value, b))

     */
//    Decoder.decodeBothCombine(ResponseHeader.codec, tDecoder)(bits)(Response[T])
  }

}
