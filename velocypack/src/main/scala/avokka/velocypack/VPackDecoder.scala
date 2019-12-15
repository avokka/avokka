package avokka.velocypack

import java.time.Instant

import avokka.velocypack.VPack._
import cats.{ApplicativeError, Monad, MonadError}
import cats.syntax.either._
import cats.syntax.traverse._
import cats.syntax.monad._
import cats.syntax.monadError._
import cats.syntax.applicativeError._
import cats.instances.either._
import cats.instances.list._
import scodec.{Attempt, DecodeResult, Decoder, Err}
import scodec.bits.{BitVector, ByteVector}
import shapeless.HList

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Cannot find an velocypack decoder for ${T}")
trait VPackDecoder[T] { self =>
  def decode(v: VPack): VPackDecoder.Result[T]

  def map[U](f: T => U): VPackDecoder[U] = (v: VPack) => self.decode(v).map(f)

  def flatMap[U](f: T => VPackDecoder.Result[U]): VPackDecoder[U] = (v: VPack) => self.decode(v).flatMap(f)

  lazy val deserializer: Decoder[T] = new Decoder[T] {
    override def decode(bits: BitVector): Attempt[DecodeResult[T]] = codecs.vpackDecoder.decode(bits).flatMap { r =>
      self.decode(r.value).fold(
        e => Attempt.failure(Err(e.getMessage)),
        t => Attempt.successful(DecodeResult(t, r.remainder))
      )
    }
  }
}

object VPackDecoder {
  type Result[T] = Either[VPackError, T]

  def apply[T](implicit decoder: VPackDecoder[T]): VPackDecoder[T] = decoder

  def apply[T](f: VPack => Result[T]): VPackDecoder[T] = (v: VPack) => f(v)

  implicit val applicativeError: ApplicativeError[VPackDecoder, VPackError] = new ApplicativeError[VPackDecoder, VPackError] {
    override def raiseError[A](e: VPackError): VPackDecoder[A] = (v: VPack) => e.asLeft
    override def handleErrorWith[A](fa: VPackDecoder[A])(f: VPackError => VPackDecoder[A]): VPackDecoder[A] = new VPackDecoder[A] {
      override def decode(v: VPack): Result[A] = fa.decode(v).handleErrorWith(e => f(e).decode(v))
    }
    override def pure[A](x: A): VPackDecoder[A] = (v: VPack) => x.asRight
    override def ap[A, B](ff: VPackDecoder[A => B])(fa: VPackDecoder[A]): VPackDecoder[B] = new VPackDecoder[B] {
      override def decode(v: VPack): Result[B] = fa.decode(v) match {
        case Left(l) => Left(l)
        case Right(r) => ff.decode(v) match {
          case Left(l) => Left(l)
          case Right(ffv) => ffv(r).asRight
        }
      }
    }
  }

  /*
  implicit val monad = new MonadError[VPackDecoder, VPackError] {
    override def flatMap[A, B](fa: VPackDecoder[A])(f: A => VPackDecoder[B]): VPackDecoder[B] = new VPackDecoder[B] {
      override def decode(v: VPack): Result[B] = fa.decode(v) match {
        case Left(value) => Left(value)
        case Right(value) => f(value).decode(v)
      }
    }
    override def tailRecM[A, B](a: A)(f: A => VPackDecoder[Either[A, B]]): VPackDecoder[B] = ???
    override def pure[A](x: A): VPackDecoder[A] = new VPackDecoder[A] {
      override def decode(v: VPack): Result[A] = x.asRight
    }
    override def raiseError[A](e: VPackError): VPackDecoder[A] = new VPackDecoder[A] {
      override def decode(v: VPack): Result[A] = e.asLeft
    }
    override def handleErrorWith[A](fa: VPackDecoder[A])(f: VPackError => VPackDecoder[A]): VPackDecoder[A] =
      new VPackDecoder[A] {
        override def decode(v: VPack): Result[A] = fa.decode(v).leftFlatMap(f)
      }
  }
  */

  // scala types instances

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
    case VArray(a) => a.traverse(d.decode).map(_.toVector)
    case _ => VPackError.WrongType.asLeft
  }

  implicit def listDecoder[T](implicit d: VPackDecoder[T]): VPackDecoder[List[T]] = {
    case VArray(a) => a.traverse(d.decode).map(_.toList)
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

  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] = VPackGeneric.Decoder(a)

}