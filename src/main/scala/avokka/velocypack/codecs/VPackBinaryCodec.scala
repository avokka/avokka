package avokka.velocypack.codecs

import avokka.velocypack.VPackBinary
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{bytes, fixedSizeBytes, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec of binary blob
 *
 * 0xc0-0xc7 : binary blob, next V - 0xbf bytes are the length of blob in bytes
 * note that binary blobs are not zero-terminated
 */
object VPackBinaryCodec extends Codec[VPackBinary] {
  override def sizeBound: SizeBound = SizeBound.atLeast(16)

  override def encode(v: VPackBinary): Attempt[BitVector] = {
    val length = v.value.size
    val lengthBytes = ulongLength(length)
    (BitVector(0xbf + lengthBytes) ++ ulongBytes(length, lengthBytes) ++ v.value.bits).pure[Attempt]
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackBinary]] = {
    for {
      head <- uint8L.decode(bits).ensure(Err("not a vpack binary"))(h => h.value >= 0xc0 && h.value <= 0xc7)
      lenBytes = head.value - 0xbf
      len  <- ulongLA(lenBytes * 8).decode(head.remainder)
      bin  <- fixedSizeBytes(len.value, bytes).decode(len.remainder)
    } yield bin.map(VPackBinary.apply)
  }
}