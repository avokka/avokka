package avokka.velocypack

import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}
import scodec.bits._
import scodec.codecs._

sealed abstract class VPackHead(val i: Int)

object VPackHead {
  case object Null extends VPackHead(0x18)
  case object False extends VPackHead(0x19)
  case object True extends VPackHead(0x1a)
  case object EmptyArray extends VPackHead(0x01)
  case class Array(override val i: Int) extends VPackHead(i)

  implicit val codec: Codec[VPackHead] = uint8L.exmap({
    case Null.`i` => Attempt.successful(Null)
    case False.`i` => Attempt.successful(False)
    case True.`i` => Attempt.successful(True)
    case EmptyArray.`i` => Attempt.successful(EmptyArray)
    case i if i == 0x02 => Attempt.successful(Array(i))
    case u => Attempt.failure(Err(s"unknown head byte ${u.toHexString}"))
  }
  , h => Attempt.successful(h.i))
}

sealed trait VPack {

}

object VPack {
  case object VNull extends VPack

  case class VBoolean(value: Boolean) extends VPack

  case class VArray(value: Vector[VPack]) extends VPack

  val False: VPack = VBoolean(false)
  val True: VPack = VBoolean(true)
  val EmptyArray: VPack = VArray(Vector.empty)

  val arrayCodec: Codec[Vector[VPack]] = new Codec[Vector[VPack]] {
    override def sizeBound: SizeBound = SizeBound.unknown

    override def encode(value: Vector[VPack]): Attempt[BitVector] = {
      if (value.isEmpty) VPackHead.codec.encode(VPackHead.EmptyArray)
      else {
        for {
          size <- uint8L.encode(value.length)
          data <- Encoder.encodeSeq(vpackEncoder)(value)
          head <- VPackHead.codec.encode(VPackHead.Array(0x02))
        } yield head ++ size ++ data
      }
    }

    override def decode(bits: BitVector): Attempt[DecodeResult[Vector[VPack]]] = {
      for {
        size <- uint8L.decode(bits)
        vec  <- Decoder.decodeCollect(vpackDecoder, Some(size.value))(size.remainder)
      } yield vec.map(_.toVector)
    }
  }

  /*
  val vpackDiscriminated: Codec[VPack] = discriminated[VPack].by(uint8L)
    .subcaseP(0x01) { case v @ EmptyArray => v } (provide(EmptyArray))
    .caseP(0x02) { case VArray(value) => value } (VArray.apply) (arrayCodec)
    .subcaseP(0x18) { case v @ VNull => v } (provide(VNull))
    .subcaseP(0x19) { case v @ False => v } (provide(False))
    .subcaseP(0x1a) { case v @ True => v } (provide(True))
*/

  val vpackDecoder: Decoder[VPack] = VPackHead.codec.flatMap {
    case VPackHead.Null => provide(VNull)
    case VPackHead.False => provide(False)
    case VPackHead.True => provide(True)
    case VPackHead.EmptyArray => provide(EmptyArray)
    case VPackHead.Array(b) => arrayCodec.map(VArray.apply)
  }

  //implicit val vpackDecoder: Decoder[VPack] = vpackDiscriminated.asDecoder

  val vpackEncoder: Encoder[VPack] = Encoder(_ match {
      // variable discriminator (the codec produce the head byte)
    case VArray(value) => arrayCodec.encode(value)
    case VNull => VPackHead.codec.encode(VPackHead.Null)
    case VBoolean(value) => VPackHead.codec.encode(if (value) VPackHead.True else VPackHead.False)
      // fixed discriminator
//    case v => vpackDiscriminated.encode(v)
  })


  def main(args: Array[String]): Unit = {
    println(vpackEncoder.encode(VArray(Vector.empty)))
    println(vpackEncoder.encode(VArray(Vector(True, VArray(Vector(False, VNull))))))
    println(vpackDecoder.decode(hex"02021a02021918".bits))
  }
}