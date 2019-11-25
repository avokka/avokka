package avokka.velocypack

import java.time.Instant

import cats.data._
import cats.implicits._
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.cats._

sealed trait VPackValue

case object VPackReserved1 extends VPackValue {
  implicit val codec: Codec[VPackReserved1.type] = constant(0x15) ~> provide(VPackReserved1)
}

case object VPackReserved2 extends VPackValue {
  implicit val codec: Codec[VPackReserved2.type] = constant(0x16) ~> provide(VPackReserved2)
}

case object VPackIllegal extends VPackValue {
  implicit val codec: Codec[VPackIllegal.type] = constant(0x17) ~> provide(VPackIllegal)
}

case object VPackNull extends VPackValue {
  implicit val codec: Codec[VPackNull.type] = constant(0x18) ~> provide(VPackNull)
}

case class VPackBoolean(value: Boolean) extends VPackValue

object VPackBoolean {
  val encoder: Encoder[VPackBoolean] = Encoder { b =>
    Attempt.successful(BitVector(if (b.value) 0x1a else 0x19))
  }

  val decoder: Decoder[VPackBoolean] = for {
    head  <- uint8L
    value <- head match {
      case 0x19 => provide(false)
      case 0x1a => provide(true)
      case _ => fail(Err("not a vpack boolean"))
    }
  } yield VPackBoolean(value)

  implicit val codec: Codec[VPackBoolean] = Codec(encoder, decoder)

  val dCodec: Codec[VPackBoolean] = discriminated[VPackBoolean].by(byte)
    .subcaseP(0x19) { case v @ VPackBoolean(false) => v } (provide(false).as)
    .subcaseP(0x1a) { case v @ VPackBoolean(true) => v } (provide(true).as)
}

case class VPackDouble(value: Double) extends VPackValue

object VPackDouble {
  implicit val codec: Codec[VPackDouble] = { constant(0x1b) ~> doubleL }.as
}

case class VPackDate(value: Long) extends VPackValue

object VPackDate {
  implicit val codec: Codec[VPackDate] = { constant(0x1c) ~> int64L }.as
}

case object VPackExternal extends VPackValue {
  implicit val codec: Codec[VPackExternal.type] = constant(0x1d) ~> provide(VPackExternal)
}

case object VPackMinKey extends VPackValue {
  implicit val codec: Codec[VPackMinKey.type] = constant(0x1e) ~> provide(VPackMinKey)
}

case object VPackMaxKey extends VPackValue {
  implicit val codec: Codec[VPackMaxKey.type] = constant(0x1f) ~> provide(VPackMaxKey)
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {

  val dCodec: Codec[VPackLong] = {
    val base = discriminated[VPackLong].by(uint8)

    val smalls = (-6 to 9).foldLeft(base) { (codec, small) =>
      val tag = small + (if (small > 0) 0x30 else 0x40)
      codec.subcaseP(tag) { case v @ VPackLong(`small`) => v } (provide(small.toLong).as)
    }

    val signeds = (0 to 6).foldLeft(smalls) { (codec, size) =>
      val bits = 8 * (size + 1)
      codec.subcaseP(0x20 + size) {
        case v @ VPackLong(s) if s < 0 && s >= -(1L << (bits - 1)) => v
      } (longL(bits).as)
    }.subcaseP(0x27) { case v @ VPackLong(s) if s < 0 => v } (int64L.as)

    val unsigneds = (0 to 6).foldLeft(signeds) { (codec, size) =>
      val bits = 8 * (size + 1)
      codec.subcaseP(0x28) {
        case v @ VPackLong(u) if u > 0 && u < (1L << bits) => v
      } (ulongL(bits).as)
    }.subcaseP(0x2f) { case v @ VPackLong(u) if u > 0 => v } (int64L.as)

    unsigneds
  }

  val encoder: Encoder[VPackLong] = Encoder { _.value match {
    // small ints
    case 0 => Attempt.successful(BitVector(0x30))
    case 1 => Attempt.successful(BitVector(0x31))
    case 2 => Attempt.successful(BitVector(0x32))
    case 3 => Attempt.successful(BitVector(0x33))
    case 4 => Attempt.successful(BitVector(0x34))
    case 5 => Attempt.successful(BitVector(0x35))
    case 6 => Attempt.successful(BitVector(0x36))
    case 7 => Attempt.successful(BitVector(0x37))
    case 8 => Attempt.successful(BitVector(0x38))
    case 9 => Attempt.successful(BitVector(0x39))
    case -6 => Attempt.successful(BitVector(0x3a))
    case -5 => Attempt.successful(BitVector(0x3b))
    case -4 => Attempt.successful(BitVector(0x3c))
    case -3 => Attempt.successful(BitVector(0x3d))
    case -2 => Attempt.successful(BitVector(0x3e))
    case -1 => Attempt.successful(BitVector(0x3f))
    // negative as signed
    case s if s < 0 && s >= -(1L << 7) => longL(8).encode(s).map { BitVector(0x20) ++ _ }
    case s if s < 0 && s >= -(1L << 15) => longL(16).encode(s).map { BitVector(0x21) ++ _ }
    case s if s < 0 && s >= -(1L << 23) => longL(24).encode(s).map { BitVector(0x22) ++ _ }
    case s if s < 0 && s >= -(1L << 31) => longL(32).encode(s).map { BitVector(0x23) ++ _ }
    case s if s < 0 && s >= -(1L << 39) => longL(40).encode(s).map { BitVector(0x24) ++ _ }
    case s if s < 0 && s >= -(1L << 47) => longL(48).encode(s).map { BitVector(0x25) ++ _ }
    case s if s < 0 && s >= -(1L << 55) => longL(56).encode(s).map { BitVector(0x26) ++ _ }
    case s if s < 0 => longL(64).encode(s).map { BitVector(0x27) ++ _ }
    // positive as unsigned
    case u if u > 0 && u < (1L << 8)  => ulongL(8).encode(u).map { BitVector(0x28) ++ _ }
    case u if u > 0 && u < (1L << 16) => ulongL(16).encode(u).map { BitVector(0x29) ++ _ }
    case u if u > 0 && u < (1L << 24) => ulongL(24).encode(u).map { BitVector(0x2a) ++ _ }
    case u if u > 0 && u < (1L << 32) => ulongL(32).encode(u).map { BitVector(0x2b) ++ _ }
    case u if u > 0 && u < (1L << 40) => ulongL(40).encode(u).map { BitVector(0x2c) ++ _ }
    case u if u > 0 && u < (1L << 48) => ulongL(48).encode(u).map { BitVector(0x2d) ++ _ }
    case u if u > 0 && u < (1L << 56) => ulongL(56).encode(u).map { BitVector(0x2e) ++ _ }
    case u if u > 0 => longL(64).encode(u).map { BitVector(0x2f) ++ _ }
  } }

  val smallDecoder: PartialFunction[Int, Byte] = {
    case 0x30 => 0
  }

  val decoder: Decoder[VPackLong] = for {
    head  <- uint8L
    value <- head match {
        // signed
      case 0x20 => longL(8)
      case 0x21 => longL(16)
      case 0x22 => longL(24)
      case 0x23 => longL(32)
      case 0x24 => longL(40)
      case 0x25 => longL(48)
      case 0x26 => longL(56)
      case 0x27 => longL(64)
        // unsigned
      case 0x28 => ulongL(8)
      case 0x29 => ulongL(16)
      case 0x2a => ulongL(24)
      case 0x2b => ulongL(32)
      case 0x2c => ulongL(40)
      case 0x2d => ulongL(48)
      case 0x2e => ulongL(56)
      case 0x2f => longL(64)
        // small ints
      case 0x30 => provide(0L)
      case 0x31 => provide(1L)
      case 0x32 => provide(2L)
      case 0x33 => provide(3L)
      case 0x34 => provide(4L)
      case 0x35 => provide(5L)
      case 0x36 => provide(6L)
      case 0x37 => provide(7L)
      case 0x38 => provide(8L)
      case 0x39 => provide(9L)
      case 0x3a => provide(-6L)
      case 0x3b => provide(-5L)
      case 0x3c => provide(-4L)
      case 0x3d => provide(-3L)
      case 0x3e => provide(-2L)
      case 0x3f => provide(-1L)
      case _ => fail(Err("not a vpack number"))
    }
  } yield VPackLong(value)

  val fCodec: Codec[VPackLong] = Codec(encoder, decoder)

  implicit val codec = fCodec
}

case class VPackString(value: String) extends VPackValue

object VPackString {

  val encoder: Encoder[VPackString] = Encoder { s =>
    for {
      bs    <- utf8.encode(s.value)
      bytes = bs.bytes
      len   = bytes.size
      head  = if (len > 126) BitVector(0xbf) ++ codecs.ulongBytes(len, 8)
              else BitVector(0x40 + len)
    } yield head ++ bs
  }

  val decoder: Decoder[VPackString] = for {
    head  <- uint8L
    len   <- if (head == 0xbf) int64L
             else if (head >= 0x40 && head < 0xbf) provide[Long](head - 0x40)
             else fail(Err("not a vpack string"))
    str   <- fixedSizeBytes(len, utf8)
  } yield VPackString(str)

  implicit val codec: Codec[VPackString] = Codec(encoder, decoder)

}

case class VPackBinary(value: ByteVector) extends VPackValue

object VPackBinary {

  val encoder: Encoder[VPackBinary] = Encoder { bin =>
    val length = bin.value.size
    val lengthBytes = codecs.ulongLength(length)
    Attempt.successful(
      BitVector(0xbf + lengthBytes) ++
      codecs.ulongBytes(length, lengthBytes) ++
      bin.value.bits
    )
  }

  val decoder: Decoder[VPackBinary] = for {
    head      <- uint8L
    lenBytes  <- if (head >= 0xc0 && head <= 0xc7) provide(head - 0xbf)
                 else fail(Err("not a vpack binary"))
    len       <- codecs.ulongLA(lenBytes * 8)
    bin       <- fixedSizeBytes(len, bytes)
  } yield VPackBinary(bin)

  implicit val codec: Codec[VPackBinary] = Codec(encoder, decoder)
}

case class VPackArray(values: Seq[BitVector]) // extends VPackValue

object VPackArray {

  private val emptyArrayResult = BitVector(0x01)

  object AllSameSize {
    def unapply(s: Iterable[BitVector]): Option[Long] = for {
      size <- s.headOption.map(_.size) if s.forall(_.size == size)
    } yield size
  }

  val encoder: Encoder[VPackArray] = Encoder(_ match {

    case VPackArray(Nil) => Attempt.successful(emptyArrayResult)

    case VPackArray(values @ AllSameSize(size)) => {
      val valuesBytes = values.length * size / 8
      val lengthMax = 1 + 8 + valuesBytes
      val (lengthBytes, head) = codecs.lengthUtils(lengthMax)
      val arrayBytes = 1 + lengthBytes + valuesBytes
      val len = codecs.ulongBytes(arrayBytes, lengthBytes)

      Attempt.successful(BitVector(0x02 + head) ++ len ++ values.reduce(_ ++ _))
    }

    case VPackArray(values) => {
      val (valuesAll, valuesBytes, offsets) = values.foldLeft((BitVector.empty, 0L, Vector.empty[Long])) {
        case ((bytes, offset, offsets), element) => (bytes ++ element, offset + element.size / 8, offsets :+ offset)
      }
      val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.length
      val (lengthBytes, head) = codecs.lengthUtils(lengthMax)
      val headBytes = 1 + lengthBytes + lengthBytes
      val indexTable = offsets.map(off => headBytes + off)

      val len = codecs.ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.length, lengthBytes)
      val nr = codecs.ulongBytes(offsets.length, lengthBytes)
      val index = indexTable.foldLeft(BitVector.empty)((b, l) => b ++ codecs.ulongBytes(l, lengthBytes))

      Attempt.successful(
      if (head == 3) BitVector(0x06 + head) ++ len ++ valuesAll ++ index ++ nr
                else BitVector(0x06 + head) ++ len ++ nr ++ valuesAll ++ index
      )
    }
  })

  val compactEncoder: Encoder[VPackArray] = Encoder(_ match {
    case VPackArray(Nil) => Attempt.successful(emptyArrayResult)
    case VPackArray(values) => {
      val valuesAll = values.reduce(_ ++ _)
      val valuesBytes = valuesAll.size / 8
      for {
        nr <- vlong.encode(values.length)
        lengthBase = 1 + valuesBytes + nr.size / 8
        lengthBaseL = codecs.vlongLength(lengthBase)
        lengthT = lengthBase + lengthBaseL
        lenL = codecs.vlongLength(lengthT)
        len <- vlong.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
      } yield BitVector(0x13) ++ len ++ valuesAll ++ nr.reverseByteOrder
    }
  })

  def decoderLinear(lenLength: Int): Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length  <- codecs.ulongLA(8 * lenLength).decode(b)
      bodyLen = length.value - 1 - lenLength
      body    <- scodec.codecs.bits(8 * bodyLen).decode(length.remainder)
      values  = body.value.bytes.dropWhile(_ == 0)
      valueLen <- VPackValue.vpLengthDecoder.decode(values.bits)
      nr = (values.size / valueLen.value).toInt
      result = Seq.range(0, nr).map(n => values.slice(n * valueLen.value, (n + 1) * valueLen.value).bits)
    } yield DecodeResult(result, body.remainder)
  )

  val decoder: Decoder[VPackArray] = {
    for {
      head     <- uint8L
      decs     <- head match {
        case 0x01 => provide(Seq.empty[BitVector])
        case 0x02 => decoderLinear(1)
        case 0x03 => decoderLinear(2)
        case 0x04 => decoderLinear(4)
        case 0x05 => decoderLinear(8)
          /*
        case 0x06 => decodeOffsets(1, head.remainder)
        case 0x07 => decodeOffsets(2, head.remainder)
        case 0x08 => decodeOffsets(4, head.remainder)
        case 0x09 => decodeOffsets64(8, head.remainder)
        case 0x13 => decodeCompact(head.remainder)
           */
        case _ => fail(Err("not a vpack array"))
      }
    } yield VPackArray(decs)
  }

  def main(args: Array[String]): Unit = {
    println(decoder.decode(hex"02 05 31 32 33".bits))
    println(decoder.decode(hex"03 07 00 00 31 32 33".bits))
  }

}

object VPackValue {
  
  lazy val vpLengthCodecs: Map[Int, Decoder[Long]] = {
    Map(
      0x00 -> provide(1L),
      0x01 -> provide(1L),
      0x02 -> ulongL(8),
      0x03 -> ulongL(16),
      0x04 -> ulongL(32),
      0x05 -> longL(64),
      0x06 -> ulongL(8),
      0x07 -> ulongL(16),
      0x08 -> ulongL(32),
      0x09 -> longL(64),
      0x0a -> provide(1L),
      0x0b -> ulongL(8),
      0x0c -> ulongL(16),
      0x0d -> ulongL(32),
      0x0e -> longL(64),
      0x0f -> ulongL(8),
      0x10 -> ulongL(16),
      0x11 -> ulongL(32),
      0x12 -> longL(64),
      0x13 -> vlongL,
      0x14 -> vlongL,
      0x18 -> provide(1L),
      0x19 -> provide(1L),
      0x1a -> provide(1L),
      0x1b -> provide(8L + 1),
      0x1c -> provide(8L + 1),
    ) ++
      (for { x <- 0x20 to 0x27 } yield x -> provide(x.toLong - 0x1f + 1)) ++
      (for { x <- 0x28 to 0x2f } yield x -> provide(x.toLong - 0x27 + 1)) ++
      (for { x <- 0x30 to 0x3f } yield x -> provide(1L)) ++
      (for { x <- 0x40 to 0xbe } yield x -> provide(x.toLong - 0x40 + 1)) ++
    Map(
      0xbf -> longL(64).map(_ + 8 + 1),
    ) ++
      (for { h <- 0xc0 to 0xc7 } yield h -> codecs.ulongLA(8 * (h - 0xbf)).map(_ + 2))
  }

  val vpLengthDecoder: Decoder[Long] = for {
    head <- uint8L
    len  <- vpLengthCodecs.getOrElse(head, fail(Err("unknown vpack header")))
  } yield len

  implicit val codec: Codec[VPackValue] = lazily { Codec.coproduct[VPackValue].choice }

  val vpBool: Codec[Boolean] = VPackBoolean.codec.as
  val vpString: Codec[String] = VPackString.codec.as //.xmap(_.value, VPackString.apply)

  val vpDouble: Codec[Double] = VPackDouble.codec.as // xmap(_.value, VPackDouble.apply)
  val vpFloat: Codec[Float] = vpDouble.xmap(_.toFloat, _.toDouble)

  val vpInstant: Codec[Instant] = VPackDate.codec.xmap(d => Instant.ofEpochMilli(d.value), t => VPackDate(t.toEpochMilli))

  val vpInt: Codec[Int] = VPackLong.codec.narrow({
    case VPackLong(value) if value.isValidInt => Attempt.successful(value.toInt)
    case VPackLong(value) => Attempt.failure(Err(s"Long to Int failure for value $value"))
  }, v => VPackLong(v.toLong))
  val vpLong: Codec[Long] = VPackLong.codec.as

  val vpBin: Codec[ByteVector] = VPackBinary.codec.as

}
