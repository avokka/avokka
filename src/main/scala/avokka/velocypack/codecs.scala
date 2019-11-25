package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._

import scala.annotation.tailrec

object codecs {

  def between[T : Numeric](codec: Codec[T], min: T, max: T): Codec[T] = {
    import Numeric.Implicits._
    import Ordering.Implicits._
    codec.exmap(
      i => if (min <= i && i <= max) Attempt.Successful(i - min) else Attempt.failure(Err("not in range")),
      d => if (d <= max - min) Attempt.Successful(min + d) else Attempt.failure(Err("not in range"))
    )
  }

  @tailrec def ulongLength(value: Long, acc: Int = 1): Int = {
    if (value > 0Xff) ulongLength(value >> 8, acc + 1)
    else acc
  }

  @tailrec def vlongLength(value: Long, acc: Long = 1): Long = {
    if (value >= 0X80) vlongLength(value >> 7, acc + 1)
    else acc
  }

  def ulongBytes(value: Long, size: Int): BitVector = BitVector.fromLong(value, size * 8, ByteOrdering.LittleEndian)

  def lengthUtils(l: Long): (Int, Int) = {
    ulongLength(l) match {
      case 1     => (1, 0)
      case 2     => (2, 1)
      case 3 | 4 => (4, 2)
      case _     => (8, 3)
    }
  }

  def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

}
