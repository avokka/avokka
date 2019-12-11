package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs._
import cats.implicits._
import scodec.bits.{BitVector, ByteVector}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}
import scodec.codecs.provide
import shapeless.HList

import scala.util.Try

trait CodecImplicits {

  implicit val booleanCodec: Codec[Boolean] = Codec(
    VPackBooleanCodec.encoder.contramap(VPackBoolean.apply),
    VPackValue.vpackDecoder.emap({
      case VPackBoolean(b) => b.pure[Attempt]
      case _ => Err("not a boolean").raiseError
    })
  )
  
  implicit val intCodec: Codec[Int] = Codec(
    VPackValue.vpackEncoder.contramap[Int]({
      case VPackSmallint.From(s) => s
      case l => VPackLong(l) 
    }),
    VPackValue.vpackDecoder.emap({
      case v : VPackSmallint => v.value.toInt.pure[Attempt]
      case v : VPackLong if v.value.isValidInt => v.value.toInt.pure[Attempt]
      case v : VPackLong => Err("vpack long overflow").raiseError
      case _ => Err("not an int").raiseError
    })
  )

  implicit val doubleCodec: Codec[Double] = Codec(
    VPackValue.vpackEncoder.contramap[Double]({
      case VPackSmallint.From(s) => s
      case VPackLong.From(l) => l
      case d => VPackDouble(d)
    }),
    VPackValue.vpackDecoder.emap({
      case v : VPackSmallint => v.value.toDouble.pure[Attempt]
      case v : VPackLong => v.value.toDouble.pure[Attempt]
      case v : VPackDouble => v.value.pure[Attempt]
      case _ => Err("not a double").raiseError
    })
  )
  
  implicit val floatCodec: Codec[Float] = doubleCodec.xmap(_.toFloat, _.toDouble)

  implicit val stringCodec: Codec[String] = Codec(
    VPackStringCodec.encoder.contramap(VPackString.apply),
    VPackValue.vpackDecoder.emap({
      case VPackString(s) => s.pure[Attempt]
      case _ => Err("not a string").raiseError
    })
  )

  implicit val instantCodec: Codec[Instant] = Codec(
    VPackDateCodec.encoder.contramap[Instant](v => VPackDate(v.toEpochMilli)),
    VPackValue.vpackDecoder.emap({
      case v : VPackDate => Instant.ofEpochMilli(v.value).pure[Attempt]
      case v : VPackLong => Instant.ofEpochMilli(v.value).pure[Attempt]
      case v : VPackString => Attempt.fromTry(Try(Instant.parse(v.value)))
      case _ => Err("not a date").raiseError
    })
  )

  implicit val binaryCodec: Codec[ByteVector] = Codec(
    VPackBinaryCodec.encoder.contramap(VPackBinary.apply),
    VPackValue.vpackDecoder.emap({
      case VPackBinary(s) => s.pure[Attempt]
      case _ => Err("not a binary").raiseError
    })
  )

  implicit def optionCodec[T](implicit codec: Codec[T]): Codec[Option[T]] = new Codec[Option[T]] {
    override def sizeBound: SizeBound = VPackNullCodec.sizeBound | codec.sizeBound
    override def encode(value: Option[T]): Attempt[BitVector] = value match {
      case Some(value) => codec.encode(value)
      case None => VPackType.Null.pureBits // VPackNullCodec.encode(VPackNull)
    }
    private val decoder = Decoder.choiceDecoder(
      codec.map(Some(_)),
      VPackType.typeDecoder.emap({
        case VPackType.Null => None.pure[Attempt]
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

  implicit val vpackObjectCodec: Codec[VPackObject] = VPackObjectCodec.codecSorted
  implicit val vpackArrayCodec: Codec[VPackArray] = VPackArrayCodec.codec
}
