package avokka.velocypack

import java.time.Instant

import avokka.velocypack.VPack._
import avokka.velocypack.VPackDecoder.Result
import scodec.bits.ByteVector
import cats.implicits._
import shapeless.HList

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Cannot find an velocypack decoder for ${T}")
trait VPackDecoder[T] { self =>
  def decode(v: VPack): VPackDecoder.Result[T]

  def map[U](f: T => U): VPackDecoder[U] = (v: VPack) => self.decode(v).map(f)
}

object VPackDecoder {
  final type Result[T] = Either[VPackError, T]

  def apply[T](implicit decoder: VPackDecoder[T]): VPackDecoder[T] = decoder

  def apply[T](f: VPack => Result[T]): VPackDecoder[T] = (v: VPack) => f(v)

  implicit val booleanDecoder: VPackDecoder[Boolean] = {
    case VBoolean(b) => b.asRight
    case _ => VPackError.WrongType.asLeft
  }

  implicit val longDecoder: VPackDecoder[Long] = {
    case VSmallint(s) => s.toLong.asRight
    case VLong(l) => l.asRight
    case _ => VPackError.WrongType.asLeft
  }

  implicit val shortDecoder: VPackDecoder[Short] = {
    case VSmallint(s) => s.toShort.asRight
    case VLong(l) if l.isValidShort => l.toShort.asRight
    case VLong(l) => VPackError.Overflow.asLeft
    case _ => VPackError.WrongType.asLeft
  }

  implicit val intDecoder: VPackDecoder[Int] = {
    case VSmallint(s) => s.toInt.asRight
    case VLong(l) if l.isValidInt => l.toInt.asRight
    case VLong(l) => VPackError.Overflow.asLeft
    case _ => VPackError.WrongType.asLeft
  }

  implicit val doubleDecoder: VPackDecoder[Double] = {
    case VSmallint(s) => s.toDouble.asRight
    case VLong(l) => l.toDouble.asRight
    case VDouble(d) => d.asRight
    case _ => VPackError.WrongType.asLeft
  }

  implicit val stringDecoder: VPackDecoder[String] = {
    case VString(s) => s.asRight
    case _ => VPackError.WrongType.asLeft
  }

  implicit val instantDecoder: VPackDecoder[Instant] = {
    case VDate(d) => Instant.ofEpochMilli(d).asRight
    case VLong(l) => Instant.ofEpochMilli(l).asRight
    case VString(s) => Either.fromTry(Try(Instant.parse(s))).leftMap(VPackError.Conversion.apply)
    case _ => VPackError.WrongType.asLeft
  }

  implicit val byteVectorDecoder: VPackDecoder[ByteVector] = {
    case VBinary(b) => b.asRight
    case _ => VPackError.WrongType.asLeft
  }

  implicit def optionDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Option[T]] = {
    case VNull => None.asRight
    case v => d.decode(v).map(Some(_))
  }

  implicit def vectorDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Vector[T]] = {
    case VArray(a) => a.toVector.traverse(d.decode)
    case _ => VPackError.WrongType.asLeft
  }

  implicit def listDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[List[T]] = {
    case VArray(a) => a.toList.traverse(d.decode)
    case _ => VPackError.WrongType.asLeft
  }

  implicit def mapDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Map[String, T]] = {
    case VObject(o) => {
      o.values.toList.traverse(d.decode).map { r =>
        (o.keys zip r).toMap
      }
    }
    case _ => VPackError.WrongType.asLeft
  }

  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric[T]): VPackDecoder[T] = VPackGeneric.decoder(a)

}