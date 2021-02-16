package avokka.velocypack

import cats.data.{Kleisli, StateT}
import cats.syntax.all._
import magnolia._
import scodec.DecodeResult
import scodec.bits.BitVector
import scodec.bits.ByteVector
import scodec.interop.cats._
import shapeless.HList

import scala.annotation.implicitNotFound
import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}

@implicitNotFound("Cannot find an velocypack decoder for ${T}")
trait VPackDecoder[T] {
  def decode(v: VPack): VPackResult[T]

  def map[U](f: T => U): VPackDecoder[U] = (v: VPack) => decode(v).map(f)

  def flatMap[U](f: T => VPackResult[U]): VPackDecoder[U] = (v: VPack) => decode(v).flatMap(f)

  /** decodes vpack bitvector to T
    * @return either error or (T value and remainder)
    */
  def decodeBits(bits: BitVector): VPackResult[DecodeResult[T]] = codecs.vpackDecoder
      .decode(bits)
      .toEither
      .leftMap(VPackError.Codec)
      .flatMap(_.traverse(decode))

  def state: StateT[VPackResult, BitVector, T] = {
    codecs.vpackDecoder.asState.flatMapF(decode)
  }

  def kleisli: Kleisli[VPackResult, VPack, T] = Kleisli(decode)
}


object VPackDecoder {
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

  implicit def listDecoder[F[_], T](implicit d: VPackDecoder[T]): VPackDecoder[List[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toList)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def seqDecoder[F[_], T](implicit d: VPackDecoder[T]): VPackDecoder[Seq[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toSeq) //.map(_.toVector)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def setDecoder[F[_], T](implicit d: VPackDecoder[T]): VPackDecoder[Set[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toSet)
    case v         => Left(VPackError.WrongType(v))
  }

  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] =
    VPackGeneric.Decoder(a)

  /*
  implicit def tuple1Decoder[T1](implicit d1: VPackDecoderF[T1]): VPackDecoderF[Tuple1[T1]] = {
    case VArray(Vector(a1)) => d1.decode(a1).map(Tuple1.apply)
    case v                  => VPackError.WrongType(v).raiseError
  }
  implicit def tuple2Decoder[T1, T2](implicit d1: VPackDecoderF[T1],
                                     d2: VPackDecoderF[T2]): VPackDecoderF[Tuple2[T1, T2]] = {
    case VArray(Vector(a1, a2)) =>
      for {
        r1 <- d1.decode(a1)
        r2 <- d2.decode(a2)
      } yield Tuple2(r1, r2)
    case v => VPackError.WrongType(v).raiseError
  }
*/

  implicit def mapDecoder[F[_], T](implicit d: VPackDecoder[T]): VPackDecoder[Map[String, T]] = {
    case VObject(o) => {
      o.toVector
        .traverse({
          case (key, v) => d.decode(v)
            .leftMap(_.historyAdd(key))
            .map(r => key -> r)
        })
        .map(_.toMap)

      // o.values.toList.traverse(d.decode).map { r => (o.keys zip r).toMap }
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

  type Typeclass[T] = VPackDecoder[T]

  def combine[T](ctx: CaseClass[VPackDecoder, T]): VPackDecoder[T] = new VPackDecoder[T] {
    override def decode(v: VPack): VPackResult[T] = v match {
      case VObject(values) => ctx.constructMonadic { p =>
        values.get(p.label) match {
          case Some(value) => p.typeclass.decode(value).leftMap(_.historyAdd(p.label))
          case None => p.default.toRight(VPackError.ObjectFieldAbsent(p.label))
        }
      }
      case _ => Left(VPackError.WrongType(v))
    }
  }

  /*
  def dispatch[T](ctx: SealedTrait[VPackDecoder, T]): VPackDecoder[T] =
    new VPackDecoder[T] {
      override def decode(v: VPack): VPackResult[T] = ctx.dispatch(v) { sub =>
        sub.typeclass.decode(sub.cast(v))
      }
    }
*/
  def gen[T]: VPackDecoder[T] = macro Magnolia.gen[T]
}
