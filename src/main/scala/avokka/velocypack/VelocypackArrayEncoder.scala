package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless._

trait IsHlistOfEncoders[E <: HList, A <: HList] {
  def aEncode(e: E, a: A, offset: Long): Attempt[(BitVector, Vector[Long])]
}

object IsHlistOfEncoders {

  def apply[E <: HList, A <: HList](implicit isListOfEncoders: IsHlistOfEncoders[E, A]): IsHlistOfEncoders[E, A] = isListOfEncoders

  implicit object HNilIsHListOfEncoders extends IsHlistOfEncoders[HNil, HNil] {
    override def aEncode(i: HNil, a: HNil, offset: Long): Attempt[(BitVector, Vector[Long])] = Attempt.successful((BitVector.empty, Vector.empty))
  }

  implicit def hconsIsHListOfEncoders[T, E, I <: HList, A <: HList]
  (implicit
    ev: IsHlistOfEncoders[I, A],
    eve: E <:< Encoder[T]
  ): IsHlistOfEncoders[E :: I, T :: A] = new IsHlistOfEncoders[E :: I, T :: A] {
    override def aEncode(i: E :: I, a: T :: A, offset: Long): Attempt[(BitVector, Vector[Long])] = {
      for {
        rl <- i.head.encode(a.head)
        rr <- ev.aEncode(i.tail, a.tail, offset + rl.size / 8)
      } yield (rl ++ rr._1, offset +: rr._2)
    }
  }

  def encoder[I <: HList, A <: HList](i: I)(implicit
                                            ev: IsHlistOfEncoders[I, A]
  ): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.aEncode(i, value, 0).flatMap {
        case (bv, offs) => vector(uint8L).encode(offs.map(_.toInt)).map { offsets =>
          bv ++ offsets
        }
      }
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

  def main(args: Array[String]): Unit = {
    val codecs = encoder(int16L :: int8L :: int16L :: HNil)
    val args = 1 :: 2 :: 3 :: HNil

    println(codecs.encode(args))
  }
}

object VelocypackArrayEncoder {

  case class Dat(i1: Int, i2: Int, i3: Int)

  def main(args: Array[String]): Unit = {
    val ea = int8 :: int32 :: int8 :: HNil
    val ca = IsHlistOfEncoders.encoder(ea).as[Dat]
    println(ca.encode(Dat(1, 2, 3)))

  }
}
