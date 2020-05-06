package avokka.velocypack

import java.time.{Instant, LocalDate}
import java.util.UUID

import avokka.velocypack.VPack._
import cats.Contravariant
import cats.syntax.either._
import scodec.bits.{BitVector, ByteVector}
import shapeless.HList

import scala.annotation.implicitNotFound
import scala.collection.compat._

@implicitNotFound("Cannot find a velocypack encoder for ${T}")
trait VPackEncoder[T] { self =>
  def encode(t: T): VPack

  def map(f: VPack => VPack): VPackEncoder[T] = (t: T) => f(self.encode(t))

  def contramap[U](f: U => T): VPackEncoder[U] = (u: U) => self.encode(f(u))

  def mapObject(f: VObject => VObject): VPackEncoder[T] = map {
    case v: VObject => f(v)
    case v => v
  }

  /** encode value to bitvector
    *
    * @param t value
    * @return either codec error or bitvector
    */
  def bits(t: T): Result[BitVector] = {
    codecs.vpackEncoder.encode(encode(t)).toEither.leftMap(VPackError.Codec)
  }
}

object VPackEncoder {
  def apply[T](implicit encoder: VPackEncoder[T]): VPackEncoder[T] = encoder

  implicit val contravariance: Contravariant[VPackEncoder] = new Contravariant[VPackEncoder] {
    override def contramap[A, B](fa: VPackEncoder[A])(f: B => A): VPackEncoder[B] = fa.contramap(f)
  }

  // scala types encoders

  implicit val booleanEncoder: VPackEncoder[Boolean] = VBoolean(_)

  implicit val longEncoder: VPackEncoder[Long] = {
    case VSmallint.From(s) => s
    case l                 => VLong(l)
  }

  implicit val shortEncoder: VPackEncoder[Short] = {
    case VSmallint.From(s) => s
    case i                 => VLong(i)
  }

  implicit val intEncoder: VPackEncoder[Int] = {
    case VSmallint.From(s) => s
    case i                 => VLong(i)
  }

  implicit val doubleEncoder: VPackEncoder[Double] = {
    case VSmallint.From(s) => s
    case VLong.From(l)     => l
    case d                 => VDouble(d)
  }

  implicit val stringEncoder: VPackEncoder[String] = VString(_)

  implicit val instantEncoder: VPackEncoder[Instant] = i => VDate(i.toEpochMilli)

  implicit val byteVectorEncoder: VPackEncoder[ByteVector] = VBinary(_)

  implicit val arrayByteEncoder: VPackEncoder[Array[Byte]] = byteVectorEncoder.contramap(ByteVector.apply)

  implicit val uuidEncoder: VPackEncoder[UUID] = byteVectorEncoder.contramap(ByteVector.fromUUID)

  implicit def optionEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Option[T]] =
    _.fold[VPack](VNull)(e.encode)

  implicit def vectorEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Vector[T]] =
    a => VArray(a.map(e.encode))
  implicit def listEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[List[T]] =
    a => VArray(a.map(e.encode).toVector)
  implicit def seqEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Seq[T]] =
    a => VArray(a.map(e.encode).toVector)
  implicit def setEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Set[T]] =
    a => VArray(a.map(e.encode).toVector)
  implicit def iterableEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Iterable[T]] =
    a => VArray(a.map(e.encode).toVector)

  implicit def genericEncoder[T <: HList](implicit a: VPackGeneric.Encoder[T]): VPackEncoder[T] =
    VPackGeneric.Encoder()(a)

  implicit def tuple1Encoder[T1](implicit e1: VPackEncoder[T1]): VPackEncoder[Tuple1[T1]] =
    a => VArray(Vector(e1.encode(a._1)))
  implicit def tuple2Encoder[T1, T2](implicit e1: VPackEncoder[T1], e2: VPackEncoder[T2]): VPackEncoder[Tuple2[T1, T2]] =
    a => VArray(Vector(e1.encode(a._1), e2.encode(a._2)))

  implicit def mapEncoder[T](implicit e: VPackEncoder[T]): VPackEncoder[Map[String, T]] =
    a => VObject(a.view.mapValues(e.encode).toMap)

  implicit val unitEncoder: VPackEncoder[Unit] = _ => VNone

  implicit val vPackEncoder: VPackEncoder[VPack] = identity(_)
  implicit val vArrayEncoder: VPackEncoder[VArray] = identity(_)
  implicit val vObjectEncoder: VPackEncoder[VObject] = identity(_)

  implicit val localDateEncoder: VPackEncoder[LocalDate] = stringEncoder.contramap(_.toString)

}
