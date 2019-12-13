package avokka.velocypack

import java.time.Instant

import avokka.velocypack.VPack._
import avokka.velocypack.codecs._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.{BitVector, ByteVector}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}
import scodec.codecs.provide

import scala.util.Try

trait CodecImplicits {

  implicit val booleanCodec: Codec[Boolean] = Codec(
    VPackBooleanCodec.encoder.contramap(VBoolean.apply),
    vpackDecoder.emap({
      case VPack.VBoolean(b) => b.pure[Attempt]
      case _ => Err("not a boolean").raiseError
    })
  )
  
  implicit val intCodec: Codec[Int] = Codec(
    vpackEncoder.contramap[Int]({
      case VSmallint.From(s) => s
      case l => VLong(l)
    }),
    vpackDecoder.emap({
      case VSmallint(s) => s.toInt.pure[Attempt]
      case VLong(l) if l.isValidInt => l.toInt.pure[Attempt]
      case VLong(l) => Err(s"vpack long $l overflow").raiseError
      case _ => Err("not an int").raiseError
    })
  )

  implicit val doubleCodec: Codec[Double] = Codec(
    vpackEncoder.contramap[Double]({
      case VSmallint.From(s) => s
      case VLong.From(l) => l
      case d => VDouble(d)
    }),
    vpackDecoder.emap({
      case VSmallint(s) => s.toDouble.pure[Attempt]
      case VLong(l) => l.toDouble.pure[Attempt]
      case VDouble(d) => d.pure[Attempt]
      case _ => Err("not a double").raiseError
    })
  )
  
  implicit val floatCodec: Codec[Float] = doubleCodec.xmap(_.toFloat, _.toDouble)

  implicit val stringCodec: Codec[String] = Codec(
    VPackStringCodec.encoder.contramap(VString.apply),
    vpackDecoder.emap({
      case VString(s) => s.pure[Attempt]
      case _ => Err("not a string").raiseError
    })
  )

  implicit val instantCodec: Codec[Instant] = Codec(
    VPackDateCodec.encoder.contramap[Instant](v => VDate(v.toEpochMilli)),
    vpackDecoder.emap({
      case VDate(d) => Instant.ofEpochMilli(d).pure[Attempt]
      case VLong(l) => Instant.ofEpochMilli(l).pure[Attempt]
      case VString(s) => Attempt.fromTry(Try(Instant.parse(s)))
      case _ => Err("not a date").raiseError
    })
  )

  implicit val binaryCodec: Codec[ByteVector] = Codec(
    VPackBinaryCodec.encoder.contramap(VBinary.apply),
    vpackDecoder.emap({
      case VBinary(b) => b.pure[Attempt]
      case _ => Err("not a binary").raiseError
    })
  )

  implicit def optionCodec[T](implicit codec: Codec[T]): Codec[Option[T]] = new Codec[Option[T]] {
    override def sizeBound: SizeBound = SizeBound.exact(8) | codec.sizeBound
    override def encode(value: Option[T]): Attempt[BitVector] = value match {
      case Some(value) => codec.encode(value)
      case None => VPackType.NullType.bits.pure[Attempt] // VPackNullCodec.encode(VPackNull)
    }
    private val decoder = Decoder.choiceDecoder(
      codec.map(Some(_)),
      VPackType.vpackTypeDecoder.emap({
        case VPackType.NullType => None.pure[Attempt]
        case _ => Err("not null").raiseError
      }),
    )
    override def decode(bits: BitVector): Attempt[DecodeResult[Option[T]]] = decoder.decode(bits)
  }

  /*
  implicit def listCodec[T](implicit codec: Codec[T]): Codec[List[T]] = vpackArrayCodec.exmap(
    _.values.toList.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  implicit def vectorCodec[T](implicit codec: Codec[T]): Codec[Vector[T]] = vpackArrayCodec.exmap(
    _.values.toVector.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  implicit def mapCodec[T](implicit codec: Codec[T]): Codec[Map[String, T]] = VPackObjectCodec.exmap(
    _.values.toList.traverse({ case (k,v) => codec.decodeValue(v).map(r => k -> r) }).map(_.toMap),
    _.toList.traverse({ case (k,v) => codec.encode(v).map(r => k -> r) }).map(l => VPackObject(l.toMap))
  )

  implicit def genericCodec[T <: HList](implicit a: VPackGeneric[T]): Codec[T] = VPackGeneric.codec()(a)
   */

  implicit def unitCodec: Codec[Unit] = provide(())

  implicit val vpackObjectCodec: Codec[VObject] = VPackObjectCodec.codecSorted
  implicit val vpackArrayCodec: Codec[VArray] = VPackArrayCodec.codec
}
