package avokka.velocypack

import scodec.Attempt.Successful
import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless._
import shapeless.ops.hlist._

trait VelocypackArrayEncoder[T] {

}

object VelocypackArrayEncoder {

  object ApplyEncoder extends Poly2 {
    implicit def valueAndCodec[A] = at[A, Codec[A]]((value: A, encoder: Codec[A]) =>
      encoder.encode(value)
    )
  }

  def apply[L <: HList, M <: HList, Z1 <: HList, Z <: HList, TR](l: L)
  (implicit
   m: Comapped.Aux[L, Codec, M],
   zw: ZipWith.Aux[M, L, ApplyEncoder.type, Z],
   tr: ToTraversable.Aux[Z, Vector, TR]
  ): Encoder[M] = new Encoder[M] {
    override def sizeBound: SizeBound = SizeBound.unknown

    override def encode(value: M): Attempt[BitVector] = {
      val values = value.zipWith(l)(ApplyEncoder).to[Vector]
      println(values)
      val valuesAndOffsets = values.foldLeft(BitVector.empty, 0L, Vector.empty[Long]) {
        case ((accb, accl, acco), Successful(b : BitVector)) => (accb ++ b, accl + b.size / 8, acco :+ accl)
      }
      println(valuesAndOffsets)
      Attempt.successful(hex"10".bits)
    }
  }

  case class Dat(i1: Int, i2: Int, i3: Int)

  def main(args: Array[String]): Unit = {
    val ea = int8 :: int32 :: int8 :: HNil
    val ca = apply(ea).as[Dat]
    ca.encode(Dat(1, 2, 3))



  }
}
