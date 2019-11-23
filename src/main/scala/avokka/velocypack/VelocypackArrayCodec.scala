package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless.{::, HList, HNil}

trait VelocypackArrayCodec[C <: HList, A <: HList] {
  def encode(encoders: C, arguments: A): Attempt[(BitVector, Seq[Long])]
  def decodeLinear(decoders: C, values: BitVector): Attempt[A]
  def decodeOffsets(decoders: C, values: BitVector, offsets: Seq[Long]): Attempt[A]
}

object VelocypackArrayCodec {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilCodec extends VelocypackArrayCodec[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[(BitVector, Seq[Long])] = Attempt.successful((BitVector.empty, Vector.empty))
    override def decodeLinear(decoders: HNil, values: BitVector): Attempt[HNil] = Attempt.successful(HNil)
    override def decodeOffsets(decoders: HNil, values: BitVector, offsets: Seq[Long]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[T, Cod, C <: HList, A <: HList](implicit ev: VelocypackArrayCodec[C, A], eve: Cod <:< Codec[T]): VelocypackArrayCodec[Cod :: C, T :: A] = new VelocypackArrayCodec[Cod :: C, T :: A] {
    override def encode(encoders: Cod :: C, arguments: T :: A): Attempt[(BitVector, Seq[Long])] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield (rl ++ rr._1, rl.size +: rr._2)
    }
    override def decodeLinear(decoders: Cod :: C, values: BitVector): Attempt[T :: A] = {
      for {
        rl <- decoders.head.decode(values)
        rr <- ev.decodeLinear(decoders.tail, rl.remainder)
      } yield rl.value :: rr
    }
    override def decodeOffsets(decoders: Cod :: C, values: BitVector, offsets: Seq[Long]): Attempt[T :: A] = {
      val offset = offsets.head
      val value = values.drop(offset * 8)
      for {
        rl <- decoders.head.decode(value).map(_.value)
        rr <- ev.decodeOffsets(decoders.tail, values, offsets.tail)
      } yield rl :: rr
    }
  }

  def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

  @scala.annotation.tailrec
  def offsets(sizes: Seq[Long], offset: Long = 0, acc: Vector[Long] = Vector.empty): Vector[Long] = sizes match {
    case head +: tail => offsets(tail, offset + head, acc :+ offset)
    case _ => acc
  }

  object AllSame {
    def unapply(sizes: Seq[Long]): Option[Long] = for { head <- sizes.headOption if sizes.forall(_ == head) } yield head
  }

  private val emptyArrayResult = BitVector(0x01)

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayCodec[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.encode(encoders, value).flatMap {
        // empty array
        case (_, Nil) => Attempt.successful(emptyArrayResult)

        // all subvalues have the same size
        case (values, AllSame(_)) => {
          val valuesBytes = values.size / 8
          val lengthMax = 1 + 8 + valuesBytes
          val (lengthBytes, head, lengthCodec) = codecs.lengthUtils(lengthMax)
          val arrayBytes = 1 + lengthBytes + valuesBytes
          for {
            len <- lengthCodec.encode(arrayBytes)
          } yield BitVector(0x02 + head) ++ len ++ values
        }

        // other cases
        case (values, sizes) => {
          val valuesBytes = values.size / 8
          val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * sizes.length
          val (lengthBytes, head, lengthCodec) = codecs.lengthUtils(lengthMax)
          val headBytes = 1 + lengthBytes + lengthBytes
          val indexTable = offsets(sizes).map(off => headBytes + off / 8)

          for {
            len <- lengthCodec.encode(headBytes + valuesBytes + lengthBytes * sizes.length)
            nr <- lengthCodec.encode(sizes.length)
            index <- Encoder.encodeSeq(lengthCodec)(indexTable)
          } yield if(head == 3) BitVector(0x06 + head) ++ len ++ values ++ index ++ nr
                           else BitVector(0x06 + head) ++ len ++ nr ++ values ++ index
        }
      }
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

  def encoderCompact[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayCodec[E, A]): Encoder[A] = new Encoder[A] {
    override def encode(value: A): Attempt[BitVector] = {
      ev.encode(encoders, value).flatMap {
        // empty array
        case (_, Nil) => Attempt.successful(emptyArrayResult)

        case (values, sizes) => {
          val valuesBytes = values.size / 8
          for {
            nr <- vlong.encode(sizes.length)
            lengthBase = 1 + valuesBytes + nr.size / 8
            lengthBaseL = codecs.vlongLength(lengthBase)
            lengthT = lengthBase + lengthBaseL
            lenL = codecs.vlongLength(lengthT)
            len <- vlong.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
          } yield BitVector(0x13) ++ len ++ values ++ nr.reverseByteOrder
        }
      }
    }
    override def sizeBound: SizeBound = SizeBound.unknown
  }

  def decoder[D <: HList, A <: HList](decoders: D)(implicit ev: VelocypackArrayCodec[D, A]): Decoder[A] = new Decoder[A] {

    def decodeLinear(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- ulongLA(8 * lenLength).decode(b)
      bodyLen = length.value - 1 - lenLength
      body    <- scodec.codecs.bits(8 * bodyLen).decode(length.remainder)
      values  = body.value.bytes.dropWhile(_ == 0).bits
      result  <- ev.decodeLinear(decoders, values)
    } yield DecodeResult(result, body.remainder)

    def decodeOffsets(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- ulongLA(8 * lenLength).decode(b)
      nr      <- ulongLA(8 * lenLength).decode(length.remainder)
      bodyOffset = 1 + lenLength + lenLength
      bodyLen = length.value - bodyOffset
      body    <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
      values  <- scodec.codecs.bits(8 * (bodyLen - nr.value * lenLength)).decode(body.value)
      offsets <- Decoder.decodeCollect(ulongLA(8 * lenLength), Some(nr.value.toInt))(values.remainder)
      result  <- ev.decodeOffsets(decoders, values.value, offsets.value.map(_ - bodyOffset))
    } yield DecodeResult(result, body.remainder)

    def decodeOffsets64(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length    <- ulongLA(8 * lenLength).decode(b)
      bodyOffset = 1 + lenLength
      bodyLen    = length.value - bodyOffset
      (body, remainder) = length.remainder.splitAt(8 * bodyLen)
      (valuesIndex, number) = body.splitAt(8 * (bodyLen - lenLength))
      nr        <- ulongLA(8 * lenLength).decode(number)
      (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * lenLength - lenLength))
      offsets   <- Decoder.decodeCollect(ulongLA(8 * lenLength), Some(nr.value.toInt))(index)
      result    <- ev.decodeOffsets(decoders, values, offsets.value.map(_ - bodyOffset))
    } yield DecodeResult(result, remainder)

    def decodeCompact(b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- vlongL.decode(b)
      bodyLen = 8 * (length.value - 1 - codecs.vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      result  <- ev.decodeLinear(decoders, body.value)
    } yield DecodeResult(result, body.remainder)

    override def decode(bits: BitVector): Attempt[DecodeResult[A]] = {
      for {
        head <- uint8L.decode(bits)
        res  <- head.value match {
          case 0x01 if ev == hnilCodec => ev.decodeLinear(decoders, head.remainder).map(n => DecodeResult(n, head.remainder))
          case 0x01 => Attempt.failure(Err("empty array for non empty decoders"))
          case 0x02 => decodeLinear(1, head.remainder)
          case 0x03 => decodeLinear(2, head.remainder)
          case 0x04 => decodeLinear(4, head.remainder)
          case 0x05 => decodeLinear(8, head.remainder)
          case 0x06 => decodeOffsets(1, head.remainder)
          case 0x07 => decodeOffsets(2, head.remainder)
          case 0x08 => decodeOffsets(4, head.remainder)
          case 0x09 => decodeOffsets64(8, head.remainder)
          case 0x13 => decodeCompact(head.remainder)
          case _ => Attempt.failure(Err("not a vpack array"))
        }
      } yield res
    }
  }

  def codec[C <: HList, A <: HList](codecs: C)(implicit ev: VelocypackArrayCodec[C, A]): Codec[A] = Codec(encoder(codecs), decoder(codecs))
  def codecCompact[C <: HList, A <: HList](codecs: C)(implicit ev: VelocypackArrayCodec[C, A]): Codec[A] = Codec(encoderCompact(codecs), decoder(codecs))

}
