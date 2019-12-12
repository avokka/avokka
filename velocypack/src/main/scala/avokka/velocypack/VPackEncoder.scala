package avokka.velocypack

import java.time.Instant

import avokka.velocypack.VPack._
import scodec.bits.ByteVector

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a velocypack encoder for ${T}")
trait VPackEncoder[T] {
  def encode(t: T): VPack
}

object VPackEncoder {
  def apply[T](implicit encoder: VPackEncoder[T]): VPackEncoder[T] = encoder

  def apply[T](f: T => VPack): VPackEncoder[T] = (t: T) => f(t)

  implicit val booleanEncoder: VPackEncoder[Boolean] = VBoolean.apply

  implicit val longEncoder: VPackEncoder[Long] = {
    case VSmallint.From(s) => s
    case l => VLong(l)
  }

  implicit val shortEncoder: VPackEncoder[Short] = {
    case VSmallint.From(s) => s
    case i => VLong(i)
  }

  implicit val intEncoder: VPackEncoder[Int] = {
    case VSmallint.From(s) => s
    case i => VLong(i)
  }

  implicit val doubleEncoder: VPackEncoder[Double] = {
    case VSmallint.From(s) => s
    case VLong.From(l) => l
    case d => VDouble(d)
  }

  implicit val stringEncoder: VPackEncoder[String] = VString.apply

  implicit val instantEncoder: VPackEncoder[Instant] = i => VDate(i.toEpochMilli)

  implicit val byteVectorEncoder: VPackEncoder[ByteVector] = VBinary.apply

  implicit def optionEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Option[T]] = _.fold[VPack](VNull)(e.encode)

  implicit def vectorEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Vector[T]] = a => VArray(a.map(e.encode))
  implicit def listEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[List[T]] = a => VArray(a.map(e.encode))
  implicit def mapEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Map[String, T]] = a => VObject(a.mapValues(e.encode))

}
