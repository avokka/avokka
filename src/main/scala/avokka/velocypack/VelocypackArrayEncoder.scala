package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless._

trait VelocypackArrayEncoder[E <: HList, A <: HList] {
  def encode(encoders: E, arguments: A, offset: Long): Attempt[(BitVector, Vector[Long])]
}

object VelocypackArrayEncoder {

  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilEncoder extends VelocypackArrayEncoder[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil, offset: Long): Attempt[(BitVector, Vector[Long])] = Attempt.successful((BitVector.empty, Vector.empty))
  }

  implicit def hconsEncoder[T, Enc, E <: HList, A <: HList](implicit ev: VelocypackArrayEncoder[E, A], eve: Enc <:< Encoder[T]): VelocypackArrayEncoder[Enc :: E, T :: A] = new VelocypackArrayEncoder[Enc :: E, T :: A] {
    override def encode(encoders: Enc :: E, arguments: T :: A, offset: Long): Attempt[(BitVector, Vector[Long])] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail, offset + rl.size)
      } yield (rl ++ rr._1, offset +: rr._2)
    }
  }

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayEncoder[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      for {
        values <- ev.encode(encoders, value, 0)
        offsets <- vector(uint8L).encode(values._2.map(off => (off / 8 + 3).toInt))
        nr <- uint8L.encode(values._2.length)
        len <- uint8L.encode((1 + 1 + nr.size / 8 + values._1.size / 8 + offsets.size / 8).toInt)
      } yield hex"06".bits ++ len ++ nr ++ values._1 ++ offsets
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

  case class Dat(i1: Int, i2: Int, i3: Int)

  def main(args: Array[String]): Unit = {
    val codec: Encoder[Dat] = encoder(int16L :: int16L :: int8L :: HNil).as
    val arg = Dat(1,2,3)

    println(codec.encode(arg))
  }
}
