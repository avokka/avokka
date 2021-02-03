package avokka.velocypack

import avokka.velocypack.VPack._
import cats.data.Kleisli
import cats.syntax.applicativeError._
import cats.syntax.bifunctor._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{ApplicativeThrow, MonadThrow}
import scodec.bits.ByteVector
import shapeless.HList

import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}

/*
@implicitNotFound("Cannot find an velocypack decoder for ${F}[${T}]")
trait VPackDecoderF[F[_], T] { self =>
  def decode(v: VPack): F[T]

  def map[U](f: T => U)(implicit F: Functor[F]): VPackDecoderF[F, U] = (v: VPack) => decode(v).map(f)

  def flatMap[U](f: T => F[U])(implicit F: FlatMap[F]): VPackDecoderF[F, U] = (v: VPack) => decode(v).flatMap(f)

  /** decodes vpack bitvector to T
    * @return either error or (T value and remainder)
    */
  def decodeBits(bits: BitVector)(implicit F: MonadVPackError[F]): F[DecodeResult[T]] = codecs.vpackDecoder
      .decode(bits)
      .toEither
      .leftMap(VPackError.Codec)
      .liftTo
      .flatMap(_.traverse(decode))

  def state(implicit F: MonadVPackError[F]): StateT[F, BitVector, T] = {
    codecs.vpackDecoder.asState.flatMapF(decode)
  }
}
*/

object VPackDecoder {
  def apply[T](implicit decoder: VPackDecoder[T]): VPackDecoder[T] = decoder
}

trait VPackDecoderInstances {

  // scala types instances

  implicit def booleanDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Boolean] = Kleisli {
    case b: VBoolean => F.pure(b.value)
    case v           => F.raiseError(VPackError.WrongType(v))
  }

  implicit def byteDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Byte] = Kleisli {
    case VSmallint(s)                => F.pure(s)
    case VLong(l) if l.isValidByte   => F.pure(l.toByte)
    case VLong(l)                    => F.raiseError(VPackError.Overflow(l))
    case VDouble(d) if d.isValidByte => F.pure(d.toByte)
    case v                           => F.raiseError(VPackError.WrongType(v))
  }

  implicit def shortDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Short] = Kleisli {
    case VSmallint(s)                 => F.pure(s.toShort)
    case VLong(l) if l.isValidShort   => F.pure(l.toShort)
    case VLong(l)                     => F.raiseError(VPackError.Overflow(l))
    case VDouble(d) if d.isValidShort => F.pure(d.toShort)
    case v                            => F.raiseError(VPackError.WrongType(v))
  }

  implicit def intDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Int] = Kleisli {
    case VSmallint(s)               => F.pure(s.toInt)
    case VLong(l) if l.isValidInt   => F.pure(l.toInt)
    case VLong(l)                   => F.raiseError(VPackError.Overflow(l))
    case VDouble(d) if d.isValidInt => F.pure(d.toInt)
    case v                          => F.raiseError(VPackError.WrongType(v))
  }

  implicit def longDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Long] = Kleisli {
    case VSmallint(s)            => F.pure(s.toLong)
    case VLong(l)                => F.pure(l)
    case VDouble(d) if d.isWhole => F.pure(d.toLong)
    case v                       => F.raiseError(VPackError.WrongType(v))
  }

  implicit def bigintDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, BigInt] = Kleisli {
    case VSmallint(s)            => F.pure(BigInt(s))
    case VLong(l)                => F.pure(BigInt(l))
    case VDouble(d) if d.isWhole => F.pure(BigInt(d.toLong))
    case VBinary(b)              => F.pure(BigInt(b.toArray))
    case v                       => F.raiseError(VPackError.WrongType(v))
  }

  implicit def floatDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Float] = Kleisli {
    case VSmallint(s) => F.pure(s.toFloat)
    case VLong(l)     => F.pure(l.toFloat)
    case VDouble(d)   => F.pure(d.toFloat)
    case v            => F.raiseError(VPackError.WrongType(v))
  }

  implicit def doubleDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Double] = Kleisli {
    case VSmallint(s) => F.pure(s.toDouble)
    case VLong(l)     => F.pure(l.toDouble)
    case VDouble(d)   => F.pure(d)
    case v            => F.raiseError(VPackError.WrongType(v))
  }

  implicit def bigdecimalDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, BigDecimal] = Kleisli {
    case VSmallint(s)            => F.pure(BigDecimal(s))
    case VLong(l)                => F.pure(BigDecimal(l))
    case VDouble(d)              => F.pure(BigDecimal(d))
    case VBinary(b)              => {
      val (scale, bigint) = b.splitAt(4)
      F.pure(BigDecimal(BigInt(bigint.toArray), scale.toInt()))
    }
    case v                       => F.raiseError(VPackError.WrongType(v))
  }

  implicit def stringDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, String] = Kleisli({
    case VString(s) => F.pure(s)
    case v          => F.raiseError(VPackError.WrongType(v))
  })

  implicit def instantDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Instant] = Kleisli({
    case VDate(d) => F.pure(Instant.ofEpochMilli(d))
    case VLong(l) => F.pure(Instant.ofEpochMilli(l))
    case VString(s) => F.catchNonFatal(Instant.parse(s)).adaptErr { case e => VPackError.Conversion(e) }
    case v => F.raiseError(VPackError.WrongType(v))
  })

  implicit def dateDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Date] = Kleisli({
    case VDate(d) => F.pure(new Date(d))
    case VLong(l) => F.pure(new Date(l))
    case VString(s) => F.catchNonFatal(Instant.parse(s)).adaptErr { case e => VPackError.Conversion(e) }.map(i => new Date(i.toEpochMilli))
    case v => F.raiseError(VPackError.WrongType(v))
  })

  implicit def byteVectorDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, ByteVector] = Kleisli({
    case VBinary(b) => F.pure(b)
    case VString(s) => F.fromEither(ByteVector.fromHexDescriptive(s).leftMap(s => VPackError.Conversion(new IllegalArgumentException(s))))
    case v          => F.raiseError(VPackError.WrongType(v))
  })

  implicit def arrayByteDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, Array[Byte]] = byteVectorDecoder[F].map(_.toArray)

  implicit def uuidDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, UUID] = Kleisli({
    case VBinary(b) => F.pure(b.toUUID)
    case VString(s) => F.catchNonFatal(UUID.fromString(s)).adaptErr { case e => VPackError.Conversion(e) }
    case v          => F.raiseError(VPackError.WrongType(v))
  })

  implicit def optionDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, Option[T]] = Kleisli({
    case VNull => F.pure(None)
    case v     => d(v).map(Some(_))
  })

  implicit def vectorDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, Vector[T]] = Kleisli({
    case VArray(a) => a.traverse(d.run) //.map(_.toVector)
    case v         => F.raiseError(VPackError.WrongType(v))
  })

  implicit def listDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, List[T]] = Kleisli({
    case VArray(a) => a.traverse(d.run).map(_.toList)
    case v         => F.raiseError(VPackError.WrongType(v))
  })

  implicit def seqDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, Seq[T]] = Kleisli({
    case VArray(a) => a.traverse(d.run).map(_.toSeq) //.map(_.toVector)
    case v         => F.raiseError(VPackError.WrongType(v))
  })

  implicit def setDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, Set[T]] = Kleisli({
    case VArray(a) => a.traverse(d.run).map(_.toSet)
    case v         => F.raiseError(VPackError.WrongType(v))
  })

  implicit def genericDecoder[F[_], T <: HList](implicit a: VPackGeneric.Decoder[F, T], F: MonadThrow[F]): VPackDecoderF[F, T] =
    VPackGeneric.Decoder(a, F)

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

  implicit def mapDecoder[F[_], T](implicit d: VPackDecoderF[F, T], F: ApplicativeThrow[F]): VPackDecoderF[F, Map[String, T]] = Kleisli({
    case VObject(o) => {
      o.toVector
        .traverse({
          case (key, v) => d(v).adaptErr {
            case e: VPackError => e.historyAdd(key)
          }.map(r => key -> r)
        })
        .map(_.toMap)

      // o.values.toList.traverse(d.decode).map { r => (o.keys zip r).toMap }
    }
    case v => F.raiseError(VPackError.WrongType(v))
  })

//  implicit val unitDecoder: VPackDecoderF[Unit] = _ => F.pure(())

  implicit def vPackDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, VPack] = Kleisli(F.pure)
  implicit def vArrayDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, VArray] = Kleisli({
    case v: VArray => F.pure(v)
    case v         => F.raiseError(VPackError.WrongType(v))
  })
  implicit def vObjectDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, VObject] = Kleisli({
    case v: VObject => F.pure(v)
    case v          => F.raiseError(VPackError.WrongType(v))
  })

  implicit def localDateDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoderF[F, LocalDate] = Kleisli({
    case VString(value) => F.catchNonFatal(LocalDate.parse(value)).adaptErr { case e => VPackError.Conversion(e) }
    case v => F.raiseError(VPackError.WrongType(v))
  })

}
