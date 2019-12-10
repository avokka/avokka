package avokka.velocypack

import avokka.velocypack.codecs.VPackType
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}
import scodec.bits._
import scodec.codecs._

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
      if (value.isEmpty) VPackType.codec.encode(VPackType.ArrayEmpty)
      else {
        for {
          size <- uint8L.encode(value.length)
          data <- Encoder.encodeSeq(vpackEncoder)(value)
          head <- VPackType.codec.encode(VPackType.ArrayUnindexed(0x02))
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

  val vpackDecoder: Decoder[VPack] = VPackType.codec.flatMap {
    case VPackType.Null => provide(VNull)
    case VPackType.False => provide(False)
    case VPackType.True => provide(True)
    case VPackType.ArrayEmpty => provide(EmptyArray)
    case VPackType.ArrayUnindexed(b) => arrayCodec.map(VArray.apply)
  }

  //implicit val vpackDecoder: Decoder[VPack] = vpackDiscriminated.asDecoder

  val vpackEncoder: Encoder[VPack] = Encoder(_ match {
      // variable discriminator (the codec produce the head byte)
    case VArray(value) => arrayCodec.encode(value)
    case VNull => VPackType.codec.encode(VPackType.Null)
    case VBoolean(value) => VPackType.codec.encode(if (value) VPackType.True else VPackType.False)
      // fixed discriminator
//    case v => vpackDiscriminated.encode(v)
  })


  def main(args: Array[String]): Unit = {
    println(8 << 0)
    println(8 << 1)
    println(8 << 2)
    println(8 << 3)
    println(vpackEncoder.encode(VArray(Vector.empty)))
    println(vpackEncoder.encode(VArray(Vector(True, VArray(Vector(False, VNull))))))
    println(vpackDecoder.decode(hex"02021a02021918".bits))
  }
}