package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._

import scala.annotation.tailrec

package object codecs {

  /**
   * calculates byte length of unsigned long
   *
   * @param value the long
   * @param acc accumulator
   * @return length in bytes
   */
  @tailrec def ulongLength(value: Long, acc: Int = 1): Int = {
    if (value > 0Xff) ulongLength(value >> 8, acc + 1)
    else acc
  }

  /**
   * calculates byte length of variable length long
   * @param value the long
   * @param acc accumulator
   * @return length in bytes
   */
  @tailrec def vlongLength(value: Long, acc: Long = 1): Long = {
    if (value >= 0X80) vlongLength(value >> 7, acc + 1)
    else acc
  }

  /**
   * builds a bit vector of a Long in little endian
   * @param value the long
   * @param size length in bytes
   * @return bit vector
   */
  def ulongBytes(value: Long, size: Int): BitVector = BitVector.fromLong(value, size * 8, ByteOrdering.LittleEndian)

  def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

  private[codecs] object AllSameSize {
    def unapply(s: Iterable[BitVector]): Option[Long] = for {
      size <- s.headOption.map(_.size) if s.forall(_.size == size)
    } yield size
  }

  private[codecs] case class HeadLength(head: Int, length: Long)

  trait VPackCodec[T] extends Codec[T]
}
