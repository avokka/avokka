package avokka.velocypack

import cats.data.{Kleisli, StateT}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.traverse._
import magnolia._
import scodec.bits.BitVector
import scodec.bits.ByteVector
import shapeless.HList

import java.net.{URI, URL}
import scala.annotation.implicitNotFound
import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}

@implicitNotFound("Cannot find an velocypack decoder for ${T}")
trait VPackDecoder[T] {
  def decode(v: VPack): VPackResult[T]

  def map[U](f: T => U): VPackDecoder[U] = (v: VPack) => decode(v).map(f)

  def flatMap[U](f: T => VPackResult[U]): VPackDecoder[U] = (v: VPack) => decode(v).flatMap(f)

  def state: StateT[VPackResult, BitVector, T] = {
    codecs.vpackDecoder.asState.flatMapF(decode)
  }

  def kleisli: Kleisli[VPackResult, VPack, T] = Kleisli(decode)
}


object VPackDecoder extends VPackDecoderDerivation with VPackDecoderLow {
  @inline def apply[T](implicit decoder: VPackDecoder[T]): VPackDecoder[T] = decoder

  // scala types instances

  implicit val booleanDecoder: VPackDecoder[Boolean] = {
    case b: VBoolean => Right(b.value)
    case v           => Left(VPackError.WrongType(v))
  }

  implicit val byteDecoder: VPackDecoder[Byte] = {
    case VSmallint(s)                => Right(s)
    case VLong(l) if l.isValidByte   => Right(l.toByte)
    case VLong(l)                    => Left(VPackError.Overflow(l))
    case VDouble(d) if d.isValidByte => Right(d.toByte)
    case v                           => Left(VPackError.WrongType(v))
  }

  implicit val shortDecoder: VPackDecoder[Short] = {
    case VSmallint(s)                 => Right(s.toShort)
    case VLong(l) if l.isValidShort   => Right(l.toShort)
    case VLong(l)                     => Left(VPackError.Overflow(l))
    case VDouble(d) if d.isValidShort => Right(d.toShort)
    case v                            => Left(VPackError.WrongType(v))
  }

  implicit val intDecoder: VPackDecoder[Int] = {
    case VSmallint(s)               => Right(s.toInt)
    case VLong(l) if l.isValidInt   => Right(l.toInt)
    case VLong(l)                   => Left(VPackError.Overflow(l))
    case VDouble(d) if d.isValidInt => Right(d.toInt)
    case v                          => Left(VPackError.WrongType(v))
  }

  implicit val longDecoder: VPackDecoder[Long] = {
    case VSmallint(s)            => Right(s.toLong)
    case VLong(l)                => Right(l)
    case VDouble(d) if d.isWhole => Right(d.toLong)
    case v                       => Left(VPackError.WrongType(v))
  }

  implicit val bigintDecoder: VPackDecoder[BigInt] = {
    case VSmallint(s)            => Right(BigInt(s.toLong))
    case VLong(l)                => Right(BigInt(l))
    case VDouble(d) if d.isWhole => Right(BigInt(d.toLong))
    case VBinary(b)              => Right(BigInt(b.toArray))
    case v                       => Left(VPackError.WrongType(v))
  }

  implicit val floatDecoder: VPackDecoder[Float] = {
    case VSmallint(s) => Right(s.toFloat)
    case VLong(l)     => Right(l.toFloat)
    case VDouble(d)   => Right(d.toFloat)
    case v            => Left(VPackError.WrongType(v))
  }

  implicit val doubleDecoder: VPackDecoder[Double] = {
    case VSmallint(s) => Right(s.toDouble)
    case VLong(l)     => Right(l.toDouble)
    case VDouble(d)   => Right(d)
    case v            => Left(VPackError.WrongType(v))
  }

  implicit val bigdecimalDecoder: VPackDecoder[BigDecimal] = {
    case VSmallint(s)            => Right(BigDecimal(s.toInt))
    case VLong(l)                => Right(BigDecimal(l))
    case VDouble(d)              => Right(BigDecimal(d))
    case VBinary(b)              => {
      val (scale, bigint) = b.splitAt(4)
      Right(BigDecimal(BigInt(bigint.toArray), scale.toInt()))
    }
    case v                       => Left(VPackError.WrongType(v))
  }

  implicit val stringDecoder: VPackDecoder[String] = {
    case VString(s) => Right(s)
    case v          => Left(VPackError.WrongType(v))
  }

  implicit val instantDecoder: VPackDecoder[Instant] = {
    case VDate(d) => Right(Instant.ofEpochMilli(d))
    case VLong(l) => Right(Instant.ofEpochMilli(l))
    case VString(s) => Either.catchNonFatal(Instant.parse(s))
      .leftMap(VPackError.Conversion(_))
    case v => Left(VPackError.WrongType(v))
  }

  implicit val dateDecoder: VPackDecoder[Date] = {
    case VDate(d) => Right(new Date(d))
    case VLong(l) => Right(new Date(l))
    case VString(s) => Either.catchNonFatal(Instant.parse(s))
      .leftMap(VPackError.Conversion(_))
      .map(i => new Date(i.toEpochMilli))
    case v => Left(VPackError.WrongType(v))
  }

  implicit val byteVectorDecoder: VPackDecoder[ByteVector] = {
    case VBinary(b) => Right(b)
    case VString(s) => ByteVector.fromHexDescriptive(s)
      .leftMap(s => VPackError.Conversion(new IllegalArgumentException(s)))
    case v          => Left(VPackError.WrongType(v))
  }

  implicit val arrayByteDecoder: VPackDecoder[Array[Byte]] = byteVectorDecoder.map(_.toArray)

  implicit val uuidDecoder: VPackDecoder[UUID] = {
    case VBinary(b) => Right(b.toUUID)
    case VString(s) => Either.catchNonFatal(UUID.fromString(s)).leftMap(VPackError.Conversion(_))
    case v          => Left(VPackError.WrongType(v))
  }

  implicit def optionDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Option[T]] = {
    case VNull => Right(None)
    case v     => d.decode(v).map(Some(_))
  }

  implicit def vectorDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Vector[T]] = {
    case VArray(a) => a.traverse(d.decode) //.map(_.toVector)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def listDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[List[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toList)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def seqDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Seq[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toSeq) //.map(_.toVector)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def setDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Set[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toSet)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] =
    VPackGeneric.Decoder(a)

  implicit def mapDecoder[K, T](implicit kd: VPackKeyDecoder[K], td: VPackDecoder[T]): VPackDecoder[Map[K, T]] = {
    case VObject(o) => {
      o.toVector
        .traverse({
          case (key, v) => (kd.decode(key), td.decode(v))
            .tupled
            .leftMap(_.historyAdd(key))
        })
        .map(_.toMap)
    }
    case v => Left(VPackError.WrongType(v))
  }

//  implicit val unitDecoder: VPackDecoderF[Unit] = _ => Right(())

  implicit val vPackDecoder: VPackDecoder[VPack] = Right(_)
  implicit val vArrayDecoder: VPackDecoder[VArray] = {
    case v: VArray => Right(v)
    case v         => Left(VPackError.WrongType(v))
  }
  implicit val vObjectDecoder: VPackDecoder[VObject] = {
    case v: VObject => Right(v)
    case v          => Left(VPackError.WrongType(v))
  }

  implicit val localDateDecoder: VPackDecoder[LocalDate] = {
    case VString(value) => Either.catchNonFatal(LocalDate.parse(value)).leftMap(VPackError.Conversion(_))
    case v => Left(VPackError.WrongType(v))
  }

  implicit val uriDecoder: VPackDecoder[URI] = {
    case VString(value) => Either.catchNonFatal(new URI(value)).leftMap(VPackError.Conversion(_))
    case v => Left(VPackError.WrongType(v))
  }

  implicit val urlDecoder: VPackDecoder[URL] = {
    case VString(value) => Either.catchNonFatal(new URL(value)).leftMap(VPackError.Conversion(_))
    case v => Left(VPackError.WrongType(v))
  }

  // semi auto derivation
  def gen[T]: VPackDecoder[T] = macro Magnolia.gen[T]
}
