package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless.ops.hlist.{Comapped, ToTraversable, ZipWith}
import shapeless.{HList, HNil, Poly2}

trait VelocypackArrayEncoder[T] {

}

object VelocypackArrayEncoder {

  object ApplyEncoder extends Poly2 {
    implicit def valueAndCodec[A] = at[A, Codec[A]]((v: A, encoder: Codec[A]) =>
      encoder.encode(v)
    )
  }

  def apply[L <: HList, M <: HList, Z <: HList, TR](l: L)(
    implicit m: Comapped.Aux[L, Codec, M],
    zipW: ZipWith.Aux[M, L, ApplyEncoder.type, Z],
    tr: ToTraversable.Aux[Z, List, TR]
  ): Encoder[M] = new Encoder[M] {
    override def sizeBound: SizeBound = SizeBound.unknown

    override def encode(value: M): Attempt[BitVector] = {
      println(value.zipWith(l)(ApplyEncoder).toList)
      Attempt.successful(hex"10".bits)
    }
  }

  case class Dat(i1: Int, i2: Int)

  def main(args: Array[String]): Unit = {
    val ca = apply(int8 :: int32 :: HNil).as[Dat]

    ca.encode(Dat(1, 2))
  }
}
