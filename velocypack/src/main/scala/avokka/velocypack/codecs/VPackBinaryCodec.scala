package avokka.velocypack.codecs

import avokka.velocypack.{VPackBinary, VPackValue}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{bytes, fixedSizeBytes}
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of binary blob
 *
 * 0xc0-0xc7 : binary blob, next V - 0xbf bytes are the length of blob in bytes
 * note that binary blobs are not zero-terminated
 */
object VPackBinaryCodec {
  import VPackType.BinaryType

  val encoder: Encoder[VPackBinary] = new Encoder[VPackBinary] {
    override def sizeBound: SizeBound = SizeBound.atLeast(16)

    override def encode(v: VPackBinary): Attempt[BitVector] = {
      val length = v.value.size
      val lengthBytes = ulongLength(length)
      (BinaryType.fromLength(lengthBytes).bits ++ ulongBytes(length, lengthBytes) ++ v.value.bits).pure[Attempt]
    }
  }

  def decoder(t: BinaryType): Decoder[VPackBinary] = for {
    len <- t.lengthDecoder
    bin <- fixedSizeBytes(len, bytes)
  } yield VPackBinary(bin)

  val codec: Codec[VPackBinary] = Codec(encoder, vpackDecoder.emap({
    case v: VPackBinary => v.pure[Attempt]
    case _ => Err("not a vpack binary").raiseError
  }))
}
