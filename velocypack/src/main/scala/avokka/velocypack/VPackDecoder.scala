package avokka.velocypack

import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}
import avokka.velocypack.VPack._
import cats.{ApplicativeThrow, FlatMap, Functor, MonadThrow}
import cats.data.{Kleisli, StateT}
import cats.syntax.all._
import scodec.DecodeResult
import scodec.bits.{BitVector, ByteVector}
import scodec.interop.cats._
import shapeless.HList

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Cannot find an velocypack decoder for ${F}[${T}]")
trait VPackDecoderF[F[_], T] { self =>
  def decode(v: VPack): F[T]

  def map[U](f: T => U)(implicit F: Functor[F]): VPackDecoderF[F, U] = (v: VPack) => decode(v).map(f)

  def flatMap[U](f: T => F[U])(implicit F: FlatMap[F]): VPackDecoderF[F, U] = (v: VPack) => decode(v).flatMap(f)

  /** decodes vpack bitvector to T
    * @return either error or (T value and remainder)
    */
  def decodeBits(bits: BitVector)(implicit F: MonadThrow[F]): F[DecodeResult[T]] = codecs.vpackDecoder
      .decode(bits)
      .toEither
      .leftMap(VPackError.Codec)
      .liftTo
      .flatMap(_.traverse(decode))

  def state(implicit F: MonadThrow[F]): StateT[F, BitVector, T] = {
    codecs.vpackDecoder.asState.flatMapF(decode)
  }
}

object VPackDecoder {
  def apply[T](implicit decoder: VPackDecoder[Result, T]): VPackDecoder[Result, T] = decoder
}

trait VPackDecoderInstances {

  // scala types instances

  implicit def booleanDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Boolean] = Kleisli {
    case b: VBoolean => b.value.pure[F]
    case v           => VPackError.WrongType(v).raiseError
  }

  implicit def byteDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Byte] = Kleisli {
    case VSmallint(s)                => s.pure[F]
    case VLong(l) if l.isValidByte   => l.toByte.pure[F]
    case VLong(l)                    => VPackError.Overflow(l).raiseError
    case VDouble(d) if d.isValidByte => d.toByte.pure[F]
    case v                           => VPackError.WrongType(v).raiseError
  }

  implicit def shortDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Short] = Kleisli {
    case VSmallint(s)                 => s.toShort.pure[F]
    case VLong(l) if l.isValidShort   => l.toShort.pure[F]
    case VLong(l)                     => VPackError.Overflow(l).raiseError
    case VDouble(d) if d.isValidShort => d.toShort.pure[F]
    case v                            => VPackError.WrongType(v).raiseError
  }

  implicit def intDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Int] = Kleisli {
    case VSmallint(s)               => s.toInt.pure[F]
    case VLong(l) if l.isValidInt   => l.toInt.pure[F]
    case VLong(l)                   => VPackError.Overflow(l).raiseError
    case VDouble(d) if d.isValidInt => d.toInt.pure[F]
    case v                          => VPackError.WrongType(v).raiseError
  }

  implicit def longDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Long] = Kleisli {
    case VSmallint(s)            => s.toLong.pure[F]
    case VLong(l)                => l.pure[F]
    case VDouble(d) if d.isWhole => d.toLong.pure[F]
    case v                       => VPackError.WrongType(v).raiseError
  }

  implicit def bigintDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, BigInt] = Kleisli {
    case VSmallint(s)            => BigInt(s).pure[F]
    case VLong(l)                => BigInt(l).pure[F]
    case VDouble(d) if d.isWhole => BigInt(d.toLong).pure[F]
    case VBinary(b)              => BigInt(b.toArray).pure[F]
    case v                       => VPackError.WrongType(v).raiseError
  }

  implicit def floatDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Float] = Kleisli {
    case VSmallint(s) => s.toFloat.pure[F]
    case VLong(l)     => l.toFloat.pure[F]
    case VDouble(d)   => d.toFloat.pure[F]
    case v            => VPackError.WrongType(v).raiseError
  }

  implicit def doubleDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Double] = Kleisli {
    case VSmallint(s) => s.toDouble.pure[F]
    case VLong(l)     => l.toDouble.pure[F]
    case VDouble(d)   => d.pure[F]
    case v            => VPackError.WrongType(v).raiseError
  }

  implicit def bigdecimalDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, BigDecimal] = Kleisli {
    case VSmallint(s)            => BigDecimal(s).pure[F]
    case VLong(l)                => BigDecimal(l).pure[F]
    case VDouble(d)              => BigDecimal(d).pure[F]
    case VBinary(b)              => {
      val (scale, bigint) = b.splitAt(4)
      BigDecimal(BigInt(bigint.toArray), scale.toInt()).pure[F]
    }
    case v                       => VPackError.WrongType(v).raiseError
  }

  implicit def stringDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, String] = Kleisli {
    case VString(s) => s.pure[F]
    case v          => VPackError.WrongType(v).raiseError
  }

  implicit def instantDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoder[F, Instant] = Kleisli {
    case VDate(d) => Instant.ofEpochMilli(d).pure[F]
    case VLong(l) => Instant.ofEpochMilli(l).pure[F]
    case VString(s) => Try(Instant.parse(s)).liftTo.adaptErr(ex => VPackError.Conversion(ex))
    case v => VPackError.WrongType(v).raiseError
  }

  implicit def dateDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoder[F, Date] = Kleisli {
    case VDate(d) => (new Date(d)).pure[F]
    case VLong(l) => (new Date(l)).pure[F]
    case VString(s) => Try(Instant.parse(s)).liftTo.adaptErr(ex => VPackError.Conversion(ex)).map(i => new Date(i.toEpochMilli))
    case v => VPackError.WrongType(v).raiseError
  }

  implicit def byteVectorDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, ByteVector] = Kleisli {
    case VBinary(b) => b.pure[F]
    case VString(s) => ByteVector.fromHexDescriptive(s).leftMap(s => VPackError.Conversion(new IllegalArgumentException(s))).liftTo
    case v          => VPackError.WrongType(v).raiseError
  }

  implicit def arrayByteDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, Array[Byte]] = byteVectorDecoder[F].map(_.toArray)

  implicit def uuidDecoder[F[_]](implicit F: ApplicativeThrow[F]): VPackDecoder[F, UUID] = Kleisli {
    case VBinary(b) => b.toUUID.pure[F]
    case VString(s) => Try(UUID.fromString(s)).liftTo.adaptErr(ex => VPackError.Conversion(ex))
    case v          => VPackError.WrongType(v).raiseError
  }

  implicit def optionDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, Option[T]] = Kleisli {
    case VNull => none[T].pure[F]
    case v     => d(v).map(Some(_))
  }

  implicit def vectorDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, Vector[T]] = Kleisli {
    case VArray(a) => a.traverse(d.run) //.map(_.toVector)
    case v         => VPackError.WrongType(v).raiseError
  }

  implicit def listDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, List[T]] = Kleisli {
    case VArray(a) => a.traverse(d.run).map(_.toList)
    case v         => VPackError.WrongType(v).raiseError
  }

  implicit def seqDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, Seq[T]] = Kleisli {
    case VArray(a) => a.traverse(d.run).map(_.toSeq) //.map(_.toVector)
    case v         => VPackError.WrongType(v).raiseError
  }

  implicit def setDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, Set[T]] = Kleisli {
    case VArray(a) => a.traverse(d.run).map(_.toSet)
    case v         => VPackError.WrongType(v).raiseError
  }

  implicit def genericDecoder[F[_], T <: HList](implicit a: VPackGeneric.Decoder[F, T], F: MonadThrow[F]): VPackDecoder[F, T] =
    VPackGeneric.Decoder(a, F)

  /*
  implicit def tuple1Decoder[T1](implicit d1: VPackDecoder[T1]): VPackDecoder[Tuple1[T1]] = {
    case VArray(Vector(a1)) => d1.decode(a1).map(Tuple1.apply)
    case v                  => VPackError.WrongType(v).raiseError
  }
  implicit def tuple2Decoder[T1, T2](implicit d1: VPackDecoder[T1],
                                     d2: VPackDecoder[T2]): VPackDecoder[Tuple2[T1, T2]] = {
    case VArray(Vector(a1, a2)) =>
      for {
        r1 <- d1.decode(a1)
        r2 <- d2.decode(a2)
      } yield Tuple2(r1, r2)
    case v => VPackError.WrongType(v).raiseError
  }
*/

  implicit def mapDecoder[F[_], T](implicit d: VPackDecoder[F, T], F: ApplicativeThrow[F]): VPackDecoder[F, Map[String, T]] = Kleisli {
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
    case v => VPackError.WrongType(v).raiseError
  }

//  implicit val unitDecoder: VPackDecoder[Unit] = _ => ().pure[F]

  implicit def vPackDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, VPack] = Kleisli(_.pure[F])
  implicit def vArrayDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, VArray] = Kleisli {
    case v: VArray => v.pure[F]
    case v         => VPackError.WrongType(v).raiseError
  }
  implicit def vObjectDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, VObject] = Kleisli {
    case v: VObject => v.pure[F]
    case v          => VPackError.WrongType(v).raiseError
  }

  implicit def localDateDecoder[F[_]: ApplicativeThrow]: VPackDecoder[F, LocalDate] = Kleisli {
    case VString(value) => Try { LocalDate.parse(value) }.liftTo.adaptErr(VPackError.Conversion(_))
    case v => VPackError.WrongType(v).raiseError
  }

}
