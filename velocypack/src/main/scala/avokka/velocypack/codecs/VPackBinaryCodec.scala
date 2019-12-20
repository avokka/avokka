package avokka.velocypack.codecs

import avokka.velocypack.VPack.VBinary
import cats.syntax.applicative._
import cats.syntax.applicativeError._
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

  val encoder: Encoder[VBinary] = new Encoder[VBinary] {
    override def sizeBound: SizeBound = SizeBound.atLeast(16)

    override def encode(v: VBinary): Attempt[BitVector] = {
      val length = v.value.size
      val lengthBytes = ulongLength(length)
      (BinaryType.fromLength(lengthBytes).bits ++ ulongBytes(length, lengthBytes) ++ v.value.bits)
        .pure[Attempt]
    }
  }

  def decoder(t: BinaryType): Decoder[VBinary] =
    for {
      len <- t.lengthDecoder
      bin <- fixedSizeBytes(len, bytes)
    } yield VBinary(bin)

  val codec: Codec[VBinary] = Codec(encoder, vpackDecoder.emap({
    case v: VBinary => v.pure[Attempt]
    case _          => Err("not a vpack binary").raiseError
  }))
}
