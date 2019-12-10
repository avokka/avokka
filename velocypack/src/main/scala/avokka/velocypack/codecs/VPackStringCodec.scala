package avokka.velocypack.codecs

import avokka.velocypack.VPackString
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{fixedSizeBytes, int64L, uint8L, utf8}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of VPackString
 *
 * 0x40-0xbe : UTF-8-string, using V - 0x40 bytes (not Unicode characters!)
 * length 0 is possible, so 0x40 is the empty string
 * maximal length is 126, note that strings here are not zero-terminated
 *
 * 0xbf : long UTF-8-string, next 8 bytes are length of string in bytes (not Unicode characters)
 * as little endian unsigned integer, note that long strings are not zero-terminated and may contain zero bytes
 */
object VPackStringCodec {
  /** lower bound of head for small strings */
  val smallByte = 0x40
  /** head for long strings */
  val longByte  = 0xbf

  val encoder: Encoder[VPackString] = new Encoder[VPackString] {
    override def sizeBound: SizeBound = SizeBound.atLeast(8)

    override def encode(v: VPackString): Attempt[BitVector] = {
      for {
        bits  <- utf8.encode(v.value)
        len   = bits.size / 8
        head  = if (len > 126) BitVector(longByte) ++ ulongBytes(len, 8)
                else BitVector(smallByte + len)
      } yield head ++ bits
    }
  }

  def decoder(t: VPackTypeLength): Decoder[VPackString] = new Decoder[VPackString] {
    override def decode(bits: BitVector): Attempt[DecodeResult[VPackString]] = {
      for {
        len <- t.lengthDecoder.decode(bits)
        str <- fixedSizeBytes(len.value, utf8).decode(len.remainder)
      } yield str.map(VPackString.apply)
    }
  }

}
