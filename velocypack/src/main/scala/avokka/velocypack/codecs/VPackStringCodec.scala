package avokka.velocypack.codecs

import avokka.velocypack.VPackString
import scodec.bits.BitVector
import scodec.codecs.{fixedSizeBytes, utf8}
import scodec.{Attempt, Decoder, Encoder, SizeBound}

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
  import VPackType.{StringShortType, StringLongType, StringType}

  val encoder: Encoder[VPackString] = new Encoder[VPackString] {
    override def sizeBound: SizeBound = SizeBound.atLeast(8)

    override def encode(v: VPackString): Attempt[BitVector] = {
      for {
        bits  <- utf8.encode(v.value)
        len   = bits.size / 8
        head  = if (len > 126) StringLongType.bits ++ ulongBytes(len, 8)
                else StringShortType.fromLength(len.toInt).bits
      } yield head ++ bits
    }
  }

  def decoder(t: StringType): Decoder[VPackString] = for {
    len <- t.lengthDecoder
    str <- fixedSizeBytes(len, utf8)
  } yield VPackString(str)
}
