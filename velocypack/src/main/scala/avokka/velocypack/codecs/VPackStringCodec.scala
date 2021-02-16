package avokka.velocypack
package codecs

import scodec.codecs.{fixedSizeBytes, utf8}
import scodec.{Decoder, Encoder}
import VPackType.{StringLongType, StringShortType, StringType}

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
private[codecs] object VPackStringCodec {

  private[codecs] val encoder: Encoder[VString] = Encoder { v =>
    for {
      bits <- utf8.encode(v.value)
      len = bits.size / 8
      head = if (len > 126) StringLongType.bits ++ ulongBytes(len, 8)
      else StringShortType.fromLength(len.toInt).bits
    } yield head ++ bits
  }

  private[codecs] def decoder(t: StringType): Decoder[VString] =
    for {
      len <- t.lengthDecoder
      str <- fixedSizeBytes(len, utf8)
    } yield VString(str)
}
