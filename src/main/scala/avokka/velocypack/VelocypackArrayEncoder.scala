package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless.{HList, HNil, ::}

trait VelocypackArrayEncoder[E <: HList, A <: HList] {
  def encode(encoders: E, arguments: A): Attempt[(BitVector, Seq[Long])]
  def size(encoders: E): SizeBound
}

object VelocypackArrayEncoder {

  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilEncoder extends VelocypackArrayEncoder[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[(BitVector, Seq[Long])] = Attempt.successful((BitVector.empty, Vector.empty))
    override def size(encoders: HNil): SizeBound = SizeBound.unknown
  }

  implicit def hconsEncoder[T, Enc, E <: HList, A <: HList](implicit ev: VelocypackArrayEncoder[E, A], eve: Enc <:< Encoder[T]): VelocypackArrayEncoder[Enc :: E, T :: A] = new VelocypackArrayEncoder[Enc :: E, T :: A] {
    override def encode(encoders: Enc :: E, arguments: T :: A): Attempt[(BitVector, Seq[Long])] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield (rl ++ rr._1, rl.size +: rr._2)
    }
    override def size(encoders: Enc :: E): SizeBound =  encoders.head.sizeBound + ev.size(encoders.tail)
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

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayEncoder[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.encode(encoders, value).flatMap {
        // empty array
        case (_, Nil) => Attempt.successful(hex"01".bits)

        // all subvalues have the same size
        case (values, AllSame(_)) => for {
          len <- uint8L.encode((1 + 1 + values.size / 8).toInt)
        } yield hex"02".bits ++ len ++ values

        // other cases
        case (values, sizes) => for {
          offs <- Encoder.encodeSeq (uint8L) (offsets (sizes).map (off => (off / 8 + 3).toInt) )
          nr <- uint8L.encode (sizes.length)
          len <- uint8L.encode ((1 + 1 + nr.size / 8 + values.size / 8 + offs.size / 8).toInt)
        } yield hex"06".bits ++ len ++ nr ++ values ++ offs
      }
    }
    override def sizeBound: SizeBound = ev.size(encoders).atLeast
  }

  case class Dat(i1: Int, i2: Int, i3: Int)

  def main(args: Array[String]): Unit = {

    val sizes = Vector(12L,8,120)
    println(offsets(sizes, 1))

    println(AllSame.unapply(Vector(3,3,3)))

    val codec: Encoder[Dat] = encoder(int8L :: int8L :: int8L :: HNil).as
    val arg = Dat(1,2,3)

    println(codec.encode(arg))
  }
}
