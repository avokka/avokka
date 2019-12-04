package avokka.velocypack.codecs

import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.BitVector
import scodec.codecs.vlongL
import scodec.bits.HexStringSyntax

class VPackVLongCodec extends Codec[Long] {
  override def sizeBound: SizeBound = SizeBound.bounded(8, 64)

  private def loopEncode(v: Long, acc: BitVector = BitVector.empty): BitVector = {
    if (v >= 0x80L) loopEncode(v >> 7, acc ++ BitVector(((v & 0x7FL).toByte | 0x80L).toByte))
    else acc ++ BitVector((v & 0x7FL).toByte)
  }

  override def encode(value: Long): Attempt[BitVector] = {
    Attempt.successful(loopEncode(value))
  }

  private def loopDecode(buffer: BitVector, sh: Int, value: Long): Attempt[DecodeResult[Long]] = {
    val byte = buffer.take(8).toByte(false)
    if ((byte & 0x80.toByte) != 0) {
      loopDecode(buffer.drop(8), sh + 7, value + ((byte & 0x7F).toLong << sh))
      /*
      if (buffer.sizeLessThan(8L)) {
        Attempt.failure(Err.InsufficientBits(8L, buffer.size, Nil))
      } else {
        val nextByte = buffer.take(8L).toByte(false)
        val nextValue = (value << 7) + (nextByte & 0x7F).toLong
        loopDecode(buffer.drop(8L), nextByte, nextValue)
      }
       */
    } else {
      Attempt.successful(DecodeResult(value + (byte & 0x07F).toLong << sh, buffer.drop(8)))
    }
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = loopDecode(bits, 0, 0)
}

object VPackVLongCodec {
  private def storeVariableValueLength(value: Long) = {
    var i: Int = 0
    var v: Long = value
    val a = scala.collection.mutable.Buffer.empty[Byte]

      while (v >= 0x80) {
        a += ((v & 0x7f).toByte | 0x80).toByte
        v >>= 7
      }
      a += (v & 0x7f).toByte
    a
  }

  def main(args: Array[String]): Unit = {
    println(vlongL.encode(1000))
    println(vlongL.decodeValue(hex"8768".bits))

    val vl = new VPackVLongCodec
    println(vl.encode(1000))
    println(vl.decode(hex"e807".bits))

    println(BitVector(storeVariableValueLength(1000)))
  }
}