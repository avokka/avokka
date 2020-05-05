package avokka.velocypack
package codecs

import cats.data.Chain
import cats.syntax.foldable._
import scodec.Attempt
import scodec.bits.BitVector
import scodec.interop.cats._

private trait VPackCompoundCodec {

  protected def lengthUtils(l: Long): (Int, Int) = {
    ulongLength(l) match {
      case 1     => (1, 0)
      case 2     => (2, 1)
      case 3 | 4 => (4, 2)
      case _     => (8, 3)
    }
  }

  protected def encodeCompact(head: Int, values: Chain[BitVector]): Attempt[BitVector] = {
    val valuesAll = values.fold //.reduce(_ ++ _)(BitVectorMonoidInstance)
    val valuesBytes = valuesAll.size / 8
    for {
      nr <- VPackVLongCodec.encode(values.length)
      lengthBase = 1 + valuesBytes + nr.size / 8
      lengthBaseL = vlongLength(lengthBase)
      lengthT = lengthBase + lengthBaseL
      lenL = vlongLength(lengthT)
      len <- VPackVLongCodec.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
    } yield BitVector(head) ++ len ++ valuesAll ++ nr.reverseByteOrder
  }

  protected def offsetsToRanges(offests: Seq[Long], size: Long): Vector[(Long, Long)] = {
    offests.zipWithIndex
      .sortBy(_._1)
      .foldRight((Vector.empty[(Int, Long, Long)], size))({
        case ((offset, index), (acc, size)) => (acc :+ ((index, offset, size)), offset)
      })
      ._1
      .sortBy(_._1)
      .map(r => r._2 -> r._3)
  }
}
