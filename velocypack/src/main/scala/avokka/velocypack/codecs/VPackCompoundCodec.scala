package avokka.velocypack.codecs

import scodec.Attempt
import scodec.bits.BitVector
import scodec.codecs.vlong

trait VPackCompoundCodec {

  protected def lengthUtils(l: Long): (Int, Int) = {
    ulongLength(l) match {
      case 1     => (1, 0)
      case 2     => (2, 1)
      case 3 | 4 => (4, 2)
      case _     => (8, 3)
    }
  }

  def encodeCompact(head: Int, values: Seq[BitVector]): Attempt[BitVector] = {
    val valuesAll = values.reduce(_ ++ _)
    val valuesBytes = valuesAll.size / 8
    for {
      nr <- vlong.encode(values.length)
      lengthBase = 1 + valuesBytes + nr.size / 8
      lengthBaseL = vlongLength(lengthBase)
      lengthT = lengthBase + lengthBaseL
      lenL = vlongLength(lengthT)
      len <- vlong.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
    } yield BitVector(head) ++ len ++ valuesAll ++ nr.reverseByteOrder
  }


  def offsetsToRanges(offests: Seq[Long], size: Long): Seq[(Long, Long)] = {
    offests.zipWithIndex.sortBy(_._1).foldRight((Vector.empty[(Int, Long, Long)], size))({
      case ((offset, index), (acc, size)) => (acc :+ (index, offset, size), offset)
    })._1.sortBy(_._1).map(r => r._2 -> r._3)
  }
}