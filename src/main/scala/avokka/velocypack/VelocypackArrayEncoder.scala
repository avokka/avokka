package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless.{HList, HNil, ::}

trait VelocypackArrayEncoder[E <: HList, A <: HList] {
  def encode(encoders: E, arguments: A): Attempt[(BitVector, Seq[Long])]
}

object VelocypackArrayEncoder {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilEncoder extends VelocypackArrayEncoder[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[(BitVector, Seq[Long])] = Attempt.successful((BitVector.empty, Vector.empty))
  }

  implicit def hconsEncoder[T, Enc, E <: HList, A <: HList](implicit ev: VelocypackArrayEncoder[E, A], eve: Enc <:< Encoder[T]): VelocypackArrayEncoder[Enc :: E, T :: A] = new VelocypackArrayEncoder[Enc :: E, T :: A] {
    override def encode(encoders: Enc :: E, arguments: T :: A): Attempt[(BitVector, Seq[Long])] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield (rl ++ rr._1, rl.size +: rr._2)
    }
  }

  @scala.annotation.tailrec
  def offsets(sizes: Seq[Long], offset: Long = 0, acc: Vector[Long] = Vector.empty): Vector[Long] = sizes match {
    case head +: tail => offsets(tail, offset + head, acc :+ offset)
    case _ => acc
  }

  object AllSame {
    def unapply(sizes: Seq[Long]): Option[Long] = sizes.headOption.flatMap { l =>
      if (sizes.forall(_ == l)) Some(l) else None
    }
  }

  def lengthUtils(l: Long): (Int, Int, Codec[Long]) = {
    codecs.bytesRequire(l, false, Seq(1,2,4)) match {
      case 1 => (1, 0, ulongL(8))
      case 2 => (2, 1, ulongL(16))
      case 4 => (4, 2, ulongL(32))
      case _ => (8, 3, longL(64))
    }
  }

  private val emptyArrayResult = BitVector(0x01)

  def vpArray[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayEncoder[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.encode(encoders, value).flatMap {
        // empty array
        case (_, Nil) => Attempt.successful(emptyArrayResult)

        // all subvalues have the same size
        case (values, AllSame(_)) => {
          val valuesBytes = values.size / 8
          val lengthMax = 1 + 8 + valuesBytes
          val (lengthBytes, head, lengthCodec) = lengthUtils(lengthMax)
          val arrayBytes = 1 + lengthBytes + valuesBytes
          for {
            len <- lengthCodec.encode(arrayBytes)
          } yield BitVector(0x02 + head) ++ len ++ values
        }

        // other cases
        case (values, sizes) => {
          val valuesBytes = values.size / 8
          val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * sizes.length
          val (lengthBytes, head, lengthCodec) = lengthUtils(lengthMax)
          val headBytes = 1 + lengthBytes + lengthBytes
          val indexTable = offsets(sizes).map(off => headBytes + off / 8)

          for {
            len <- lengthCodec.encode(headBytes + valuesBytes + lengthBytes * sizes.length)
            nr <- lengthCodec.encode(sizes.length)
            index <- Encoder.encodeSeq(lengthCodec)(indexTable)
          } yield if(head == 3) BitVector(0x06 + head) ++ len ++ values ++ index ++ nr
                           else BitVector(0x06 + head) ++ len ++ nr ++ values ++ index
        }
      }
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

  @scala.annotation.tailrec
  def vLength(value: Long, acc: Long = 1): Long = {
    if (value >= 0X80) vLength(value >> 7, acc + 1)
    else acc
  }

  def vpArrayCompact[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayEncoder[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.encode(encoders, value).flatMap {
        // empty array
        case (_, Nil) => Attempt.successful(emptyArrayResult)

        case (values, sizes) => {
          val valuesBytes = values.size / 8
          for {
            nr <- vlong.encode(sizes.length)
            lengthBase = 1 + valuesBytes + nr.size / 8
            lengthBaseL = vLength(lengthBase)
            lengthT = lengthBase + lengthBaseL
            lenL = vLength(lengthT)
            len <- vlong.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
          } yield BitVector(0x13) ++ len ++ values ++ nr.reverseByteOrder
        }
      }
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

}
