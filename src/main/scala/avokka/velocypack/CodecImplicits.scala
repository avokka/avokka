package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs._
import cats.implicits._
import scodec.bits.{BitVector, ByteVector}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}
import shapeless.HList

trait CodecImplicits extends CodecImplicitsLowPriority {

  implicit val booleanCodec: Codec[Boolean] = VPackBooleanCodec.as

  implicit val intCodec: Codec[Int] = new Codec[Int] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Int): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case VPackLong(l) => VPackLongCodec.encode(l)
    }

    private val decoder = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toInt),
      VPackLongCodec.map(_.value).emap({
        case l if l.isValidInt => l.toInt.pure[Attempt]
        case _ => Err("vpack long overflow").raiseError
      })
    )

    override def decode(bits: BitVector): Attempt[DecodeResult[Int]] = decoder.decode(bits)
  }

  implicit val longCodec: Codec[Long] = new Codec[Long] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Long): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case l => VPackLongCodec.encode(VPackLong(l))
    }

    private val decoder = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toLong),
      VPackLongCodec.map(_.value)
    )

    override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = decoder.decode(bits)
  }

  implicit val doubleCodec: Codec[Double] = new Codec[Double] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackDoubleCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Double): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case VPackLong(l) => VPackLongCodec.encode(l)
      case d => VPackDoubleCodec.encode(VPackDouble(d))
    }

    private val decoder = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toDouble),
      VPackLongCodec.map(_.value.toDouble),
      VPackDoubleCodec.map(_.value),
    )

    override def decode(bits: BitVector): Attempt[DecodeResult[Double]] = decoder.decode(bits)
  }

  implicit val floatCodec: Codec[Float] = doubleCodec.xmap(_.toFloat, _.toDouble)

  implicit val stringCodec: Codec[String] = VPackStringCodec.as

  implicit val instantCodec: Codec[Instant] = VPackDateCodec.xmap(d => Instant.ofEpochMilli(d.value), t => VPackDate(t.toEpochMilli))

  implicit val binaryCodec: Codec[ByteVector] = VPackBinaryCodec.as

  implicit def optionCodec[T](implicit codec: Codec[T]): Codec[Option[T]] = new Codec[Option[T]] {
    override def sizeBound: SizeBound = VPackNullCodec.sizeBound | codec.sizeBound
    override def encode(value: Option[T]): Attempt[BitVector] = value match {
      case Some(value) => codec.encode(value)
      case None => VPackNullCodec.encode(VPackNull)
    }
    private val decoder = Decoder.choiceDecoder(
      codec.map(Some(_)),
      VPackNullCodec.map(_ => None),
    )
    override def decode(bits: BitVector): Attempt[DecodeResult[Option[T]]] = decoder.decode(bits)
  }

  implicit def listCodec[T](implicit codec: Codec[T]): Codec[List[T]] = VPackArrayCodec.exmap(
    _.values.toList.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  implicit def vectorCodec[T](implicit codec: Codec[T]): Codec[Vector[T]] = VPackArrayCodec.exmap(
    _.values.toVector.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  implicit def mapCodec[T](implicit codec: Codec[T]): Codec[Map[String, T]] = VPackObjectCodec.exmap(
    _.values.toList.traverse({ case (k,v) => codec.decodeValue(v).map(r => k -> r) }).map(_.toMap),
    _.toList.traverse({ case (k,v) => codec.encode(v).map(r => k -> r) }).map(l => VPackObject(l.toMap))
  )

  implicit def hlistCodec[T <: HList](implicit a: VPackHListCodec[T]): Codec[T] = VPackHListCodec.codec(a)
}

trait CodecImplicitsLowPriority {
}
