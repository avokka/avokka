package avokka.velocypack

import scodec._
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._
import shapeless.PolyDefns.~>>
import shapeless.{::, HList, HNil, Poly2}
import shapeless.UnaryTCConstraint.*->*
import shapeless.ops.hlist.{Mapper, RightFolder}

trait VelocypackArrayEncoder[T] {

}

object VelocypackArrayEncoder {

  val hnilCodec: Codec[HNil] = new Codec[HNil] {
    override def sizeBound = SizeBound.exact(0)
    override def encode(hn: HNil) = Attempt.successful(BitVector.empty)
    override def decode(buffer: BitVector) = Attempt.successful(DecodeResult(HNil, buffer))
    override def toString = s"HNil"
  }

  def prepend[A, L <: HList](a: Codec[A], l: Codec[L]): Codec[A :: L] = new Codec[A :: L] {
    override def sizeBound = a.sizeBound + l.sizeBound
    override def encode(xs: A :: L) = Codec.encodeBoth(a, l)(xs.head, xs.tail)
    override def decode(buffer: BitVector) = Codec.decodeBothCombine(a, l)(buffer) { _ :: _ }
    override def toString = s"$a :: $l"
  }

  object PrependCodec extends Poly2 {
    implicit def caseCodecAndCodecHList[A, L <: HList] = at[Codec[A], Codec[L]](prepend)
  }
/*
  object a extends (Encoder ~>> ByteVector) {
    override def apply[T](f: Encoder[T]): ByteVector = f
  }
*/
  def apply[L <: HList : *->*[Codec]#λ, M <: HList](l: L)(
    implicit folder: RightFolder.Aux[L, Codec[HNil], PrependCodec.type, Codec[M]],
  ): Codec[M] = {
    l.foldRight(hnilCodec)(PrependCodec)
  }

}
