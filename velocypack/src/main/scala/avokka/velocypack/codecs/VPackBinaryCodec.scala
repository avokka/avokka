package avokka.velocypack.codecs

import avokka.velocypack.{VPackBinary, VPackString}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{bytes, fixedSizeBytes, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of binary blob
 *
 * 0xc0-0xc7 : binary blob, next V - 0xbf bytes are the length of blob in bytes
 * note that binary blobs are not zero-terminated
 */
object VPackBinaryCodec {
  val minByte = 0xc0
  val maxByte = 0xc7

  val encoder: Encoder[VPackBinary] = new Encoder[VPackBinary] {
    override def sizeBound: SizeBound = SizeBound.atLeast(16)

    override def encode(v: VPackBinary): Attempt[BitVector] = {
      val length = v.value.size
      val lengthBytes = ulongLength(length)
      (BitVector(minByte - 1 + lengthBytes) ++ ulongBytes(length, lengthBytes) ++ v.value.bits).pure[Attempt]
    }
  }

  def decoder(t: VPackType.Binary): Decoder[VPackBinary] = for {
    len <- t.lengthDecoder
    bin <- fixedSizeBytes(len, bytes)
  } yield VPackBinary(bin)

  /*
  def decoder(t: VPackType.Binary): Decoder[VPackBinary] = new Decoder[VPackBinary] {
    override def decode(bits: BitVector): Attempt[DecodeResult[VPackBinary]] = {
      for {
        len <- t.lengthDecoder.decode(bits)
        bin <- fixedSizeBytes(len.value, bytes).decode(len.remainder)
      } yield bin.map(VPackBinary.apply)
    }
  }
   */
}
