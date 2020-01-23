package avokka.velocypack.codecs

import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

import scala.annotation.tailrec

/**
  * variable length long codec used in compact array and object
  */
private object VPackVLongCodec extends Codec[Long] {
  override def sizeBound: SizeBound = SizeBound.bounded(8, 64)

  @tailrec
  private def loopEncode(value: Long, acc: BitVector = BitVector.empty): BitVector = {
    val low = (value & 0x7FL).toByte
    if (value >= 0x80L) loopEncode(value >> 7, acc ++ BitVector(low | 0x80))
    else acc ++ BitVector(low)
  }

  override def encode(value: Long): Attempt[BitVector] = {
    Attempt.successful(loopEncode(value))
  }

  @tailrec
  private def loopDecode(buffer: BitVector, shift: Int = 0, acc: Long = 0): Attempt[DecodeResult[Long]] = {
    val (head, tail) = buffer.splitAt(8)
    val byte = head.toByte(false)
    val value = acc | ((byte & 0x7F).toLong << shift)
    if ((byte & 0x80) != 0) {
      loopDecode(tail, shift + 7, value)
    } else {
      Attempt.successful(DecodeResult(value, tail))
    }
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = loopDecode(bits)
}
