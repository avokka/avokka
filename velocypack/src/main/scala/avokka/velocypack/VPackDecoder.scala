package avokka.velocypack

import java.time.{Instant, LocalDate}
import java.util.UUID

import avokka.velocypack.VPack._
import cats.instances.either._
import cats.instances.vector._
import cats.syntax.either._
import cats.syntax.traverse._
import scodec.DecodeResult
import scodec.bits.{BitVector, ByteVector}
import scodec.interop.cats._
import shapeless.HList

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Cannot find an velocypack decoder for ${T}")
trait VPackDecoder[T] { self =>
  def decode(v: VPack): Result[T]

  def map[U](f: T => U): VPackDecoder[U] = (v: VPack) => decode(v).map(f)

  def emap[U](f: T => Result[U]): VPackDecoder[U] = (v: VPack) => decode(v).flatMap(f)

  /** decodes vpack bitvector to T
    * @return either error or (T value and remainder)
    */
  def decode(bits: BitVector): Result[DecodeResult[T]] = codecs.vpackDecoder
      .decode(bits)
      .toEither
      .leftMap(VPackError.Codec)
      .flatMap(_.traverse(decode))
}

object VPackDecoder {
  def apply[T](implicit decoder: VPackDecoder[T]): VPackDecoder[T] = decoder

  // scala types instances

  implicit val booleanDecoder: VPackDecoder[Boolean] = {
    case VBoolean(b) => b.asRight
    case v           => VPackError.WrongType(v).asLeft
  }

  implicit val byteDecoder: VPackDecoder[Byte] = {
    case VSmallint(s)                => s.asRight
    case VLong(l) if l.isValidByte   => l.toByte.asRight
    case VLong(l)                    => VPackError.Overflow(l).asLeft
    case VDouble(d) if d.isValidByte => d.toByte.asRight
    case v                           => VPackError.WrongType(v).asLeft
  }

  implicit val shortDecoder: VPackDecoder[Short] = {
    case VSmallint(s)                 => s.toShort.asRight
    case VLong(l) if l.isValidShort   => l.toShort.asRight
    case VLong(l)                     => VPackError.Overflow(l).asLeft
    case VDouble(d) if d.isValidShort => d.toShort.asRight
    case v                            => VPackError.WrongType(v).asLeft
  }

  implicit val intDecoder: VPackDecoder[Int] = {
    case VSmallint(s)               => s.toInt.asRight
    case VLong(l) if l.isValidInt   => l.toInt.asRight
    case VLong(l)                   => VPackError.Overflow(l).asLeft
    case VDouble(d) if d.isValidInt => d.toInt.asRight
    case v                          => VPackError.WrongType(v).asLeft
  }

  implicit val longDecoder: VPackDecoder[Long] = {
    case VSmallint(s)            => s.toLong.asRight
    case VLong(l)                => l.asRight
    case VDouble(d) if d.isWhole => d.toLong.asRight
    case v                       => VPackError.WrongType(v).asLeft
  }

  implicit val floatDecoder: VPackDecoder[Float] = {
    case VSmallint(s) => s.toFloat.asRight
    case VLong(l)     => l.toFloat.asRight
    case VDouble(d)   => d.toFloat.asRight
    case v            => VPackError.WrongType(v).asLeft
  }

  implicit val doubleDecoder: VPackDecoder[Double] = {
    case VSmallint(s) => s.toDouble.asRight
    case VLong(l)     => l.toDouble.asRight
    case VDouble(d)   => d.asRight
    case v            => VPackError.WrongType(v).asLeft
  }

  implicit val stringDecoder: VPackDecoder[String] = {
    case VString(s) => s.asRight
    case v          => VPackError.WrongType(v).asLeft
  }

  implicit val instantDecoder: VPackDecoder[Instant] = {
    case VDate(d) => Instant.ofEpochMilli(d).asRight
    case VLong(l) => Instant.ofEpochMilli(l).asRight
    case VString(s) => Either.fromTry(Try(Instant.parse(s))).leftMap(ex => VPackError.Conversion(ex))
    case v => VPackError.WrongType(v).asLeft
  }

  implicit val byteVectorDecoder: VPackDecoder[ByteVector] = {
    case VBinary(b) => b.asRight
    case VString(s) => ByteVector.fromHexDescriptive(s).leftMap(s => VPackError.Conversion(new IllegalArgumentException(s)))
    case v          => VPackError.WrongType(v).asLeft
  }

  implicit val arrayByteDecoder: VPackDecoder[Array[Byte]] = byteVectorDecoder.map(_.toArray)

  implicit val uuidDecoder: VPackDecoder[UUID] = {
    case VBinary(b) => b.toUUID.asRight
    case VString(s) => UUID.fromString(s).asRight
    case v          => VPackError.WrongType(v).asLeft
  }

  implicit def optionDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Option[T]] = {
    case VNull => None.asRight
    case v     => d.decode(v).map(Some(_))
  }

  implicit def vectorDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Vector[T]] = {
    case VArray(a) => a.traverse(d.decode) //.map(_.toVector)
    case v         => VPackError.WrongType(v).asLeft
  }

  implicit def listDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[List[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toList)
    case v         => VPackError.WrongType(v).asLeft
  }

  implicit def seqDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Seq[T]] = {
    case VArray(a) => a.traverse(d.decode) //.map(_.toVector)
    case v         => VPackError.WrongType(v).asLeft
  }

  implicit def setDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Set[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toSet)
    case v         => VPackError.WrongType(v).asLeft
  }

  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] =
    VPackGeneric.Decoder(a)

  implicit def tuple1Decoder[T1](implicit d1: VPackDecoder[T1]): VPackDecoder[Tuple1[T1]] = {
    case VArray(Vector(a1)) => d1.decode(a1).map(Tuple1.apply)
    case v                  => VPackError.WrongType(v).asLeft
  }
  implicit def tuple2Decoder[T1, T2](implicit d1: VPackDecoder[T1],
                                     d2: VPackDecoder[T2]): VPackDecoder[Tuple2[T1, T2]] = {
    case VArray(Vector(a1, a2)) =>
      for {
        r1 <- d1.decode(a1)
        r2 <- d2.decode(a2)
      } yield Tuple2(r1, r2)
    case v => VPackError.WrongType(v).asLeft
  }

  implicit def mapDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Map[String, T]] = {
    case VObject(o) => {
      o.toVector
        .traverse[Result, (String, T)]({
          case (key, v) => d.decode(v).leftMap(_.historyAdd(key)).map(r => key -> r)
        })
        .map(_.toMap)

      // o.values.toList.traverse(d.decode).map { r => (o.keys zip r).toMap }
    }
    case v => VPackError.WrongType(v).asLeft
  }

//  implicit val unitDecoder: VPackDecoder[Unit] = _ => ().asRight

  implicit val vPackDecoder: VPackDecoder[VPack] = _.asRight
  implicit val vArrayDecoder: VPackDecoder[VArray] = {
    case v: VArray => v.asRight
    case v         => VPackError.WrongType(v).asLeft
  }
  implicit val vObjectDecoder: VPackDecoder[VObject] = {
    case v: VObject => v.asRight
    case v          => VPackError.WrongType(v).asLeft
  }

  implicit val localDateDecoder: VPackDecoder[LocalDate] = {
    case VString(value) => LocalDate.parse(value).asRight
    case v => VPackError.WrongType(v).asLeft
  }

}
