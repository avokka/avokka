package avokka.velocypack.codecs

import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

object VPackVLongCodec extends Codec[Long] {
  override def sizeBound: SizeBound = SizeBound.bounded(8, 64)

  private def loopEncode(v: Long, acc: BitVector = BitVector.empty): BitVector = {
    if (v >= 0x80L) loopEncode(v >> 7, acc ++ BitVector(((v & 0x7FL).toByte | 0x80L).toByte))
    else acc ++ BitVector((v & 0x7FL).toByte)
  }

  override def encode(value: Long): Attempt[BitVector] = {
    Attempt.successful(loopEncode(value))
  }

  private def loopDecode(buffer: BitVector, sh: Int, value: Long): Attempt[DecodeResult[Long]] = {
    val (head, tail) = buffer.splitAt(8)
    val byte = head.toByte(false)
    val v = value | ((byte & 0x7F).toLong << sh)
    if ((byte & 0x80.toByte) != 0) {
      loopDecode(tail, sh + 7, v)
    } else {
      Attempt.successful(DecodeResult(v, tail))
    }
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = loopDecode(bits, 0, 0)
}
