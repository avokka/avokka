package avokka.velocypack

import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}
import cats.Contravariant
import cats.syntax.either._
import scodec.bits.{BitVector, ByteVector}

import java.net.{URI, URL}
import scala.annotation.implicitNotFound

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
  def bits(t: T): VPackResult[BitVector] = {
    codecs.vpackEncoder.encode(encode(t)).toEither.leftMap(VPackError.Codec.apply)
  }
}

object VPackEncoder extends VPackEncoderGeneric with VPackEncoderDerivation with VPackEncoderLow {
  @inline def apply[T](implicit encoder: VPackEncoder[T]): VPackEncoder[T] = encoder

  implicit val contravariance: Contravariant[VPackEncoder] = new Contravariant[VPackEncoder] {
    override def contramap[A, B](fa: VPackEncoder[A])(f: B => A): VPackEncoder[B] = fa.contramap(f)
  }

  // scala types encoders

  implicit val booleanEncoder: VPackEncoder[Boolean] = if(_) VTrue else VFalse

  implicit val byteEncoder: VPackEncoder[Byte] = {
    case b if VSmallint.isValid(b) => VSmallint(b)
    case b => VLong(b.toLong)
  }

  implicit val shortEncoder: VPackEncoder[Short] = {
    case i if VSmallint.isValid(i) => VSmallint(i.toByte)
    case i => VLong(i.toLong)
  }

  implicit val intEncoder: VPackEncoder[Int] = {
    case i if VSmallint.isValid(i) => VSmallint(i.toByte)
    case i => VLong(i.toLong)
  }

  implicit val longEncoder: VPackEncoder[Long] = {
    case l if VSmallint.isValid(l) => VSmallint(l.toByte)
    case l => VLong(l)
  }

  implicit val bigintEncoder: VPackEncoder[BigInt] = {
    case i if VSmallint.isValidByte(i) => VSmallint(i.toByte)
    case i if i.isValidLong            => VLong(i.toLong)
    case i => VBinary(ByteVector(i.toByteArray))
  }

  implicit val floatEncoder: VPackEncoder[Float] = {
    case f if VSmallint.isValidByte(f) => VSmallint(f.toByte)
    case f if f.toLong.toFloat == f    => VLong(f.toLong)
    case f => VDouble(f.toDouble)
  }

  implicit val doubleEncoder: VPackEncoder[Double] = {
    case d if VSmallint.isValidByte(d) => VSmallint(d.toByte)
    case d if d.toLong.toDouble == d   => VLong(d.toLong)
    case d => VDouble(d)
  }

  implicit val bigdecimalEncoder: VPackEncoder[BigDecimal] = {
    case d if VSmallint.isValidByte(d) => VSmallint(d.toByte)
    case d if d.isValidLong            => VLong(d.toLongExact)
    case d if d.isDecimalDouble        => VDouble(d.toDouble)
    case d => VBinary(ByteVector.fromInt(d.scale) ++ ByteVector(d.underlying().unscaledValue().toByteArray))
  }

  implicit val stringEncoder: VPackEncoder[String] = VString(_)

  implicit val instantEncoder: VPackEncoder[Instant] = i => VDate(i.toEpochMilli)

  implicit val dateEncoder: VPackEncoder[Date] = d => VDate(d.getTime)

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

  implicit def mapEncoder[K, T](implicit ke: VPackKeyEncoder[K], te: VPackEncoder[T]): VPackEncoder[Map[K, T]] =
    a => VObject(a.map { case (k, t) => (ke.encode(k), te.encode(t)) })

  implicit val unitEncoder: VPackEncoder[Unit] = _ => VNone

  implicit val vPackEncoder: VPackEncoder[VPack] = identity(_)
  implicit val vArrayEncoder: VPackEncoder[VArray] = identity(_)
  implicit val vObjectEncoder: VPackEncoder[VObject] = identity(_)

  implicit val localDateEncoder: VPackEncoder[LocalDate] = stringEncoder.contramap(_.toString)

  implicit val uriEncoder: VPackEncoder[URI] = stringEncoder.contramap(_.toString)
  implicit val urlEncoder: VPackEncoder[URL] = stringEncoder.contramap(_.toString)

}
