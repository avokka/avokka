package avokka.velocypack

import scala.compiletime.*
import scala.deriving.*
import cats.implicits.*
import cats.syntax.*

object VPackGeneric { c =>

  inline def summonAllEncoders[A <: Tuple]: List[VPackEncoder[?]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[VPackEncoder[t]] :: summonAllEncoders[ts]
    }

  inline def encoder[P <: Product](using m: Mirror.ProductOf[P]): VPackEncoder[P] = {
    lazy val insts = summonAllEncoders[m.MirroredElemTypes]
    new VPackEncoder[P] {
      override def encode(t: P): VPack = VArray(
        insts.asInstanceOf[List[VPackEncoder[Any]]].zip(t.productIterator).map {
          _.encode(_)
        }.toVector
      )
    }
  }

  inline def tuple[E, T <: Tuple](f: E => T): VPackEncoder[E] = {
    lazy val insts = summonAllEncoders[T]
    new VPackEncoder[E]:
      override def encode(t: E): VPack = VArray(
        insts.asInstanceOf[List[VPackEncoder[Any]]].zip(f(t).productIterator).map {
          _.encode(_)
        }.toVector
      )
  }

  trait Decoder[A <: Tuple] {
    def decode(v: Vector[VPack]): VPackResult[A]
  }

  inline def summonAllDecoders[A <: Tuple]: List[VPackDecoder[?]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[VPackDecoder[t]] :: summonAllDecoders[ts]
    }

  inline def decoder[P <: Product](using m: Mirror.ProductOf[P]): VPackDecoder[P] = {
    lazy val insts = summonAllDecoders[m.MirroredElemTypes]
    new VPackDecoder[P] {
      override def decode(v: VPack): VPackResult[P] = v match {
        case VArray(values) => if (values.length != insts.length) {
          Left(VPackError.NotEnoughElements())
        } else {
          insts.asInstanceOf[List[VPackDecoder[Any]]].zip(values).map {
            _.decode(_)
          }.sequence.map(r =>
            val product: Product = new Product {
              override def productArity: Int = r.size
              override def productElement(n: Int): Any = r(n)
              override def canEqual(that: Any): Boolean = false
            }
            m.fromProduct(product)
          )
        }
        case v => Left(VPackError.WrongType(v))
      }
    }
  }
}
