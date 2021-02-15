import avokka.velocypack._

import scala.util.Try

val b: Boolean = true

b.toVPack
b.toVPackBits.right.get

val a: Seq[Int] = List(1,2)

a.toVPack
a.toVPackBits.right.get

import scodec.bits._

val bits = hex"02043334".bits
bits.asVPack[Vector[Long]].right.get.value

case class Test(b: Boolean)
implicit val testEncoder: VPackEncoder[Test] = VPackRecord[Test].encoder
implicit val testDecoder: VPackDecoder[Test] = VPackRecord[Test].decoder

val t = Test(true)

t.toVPack
t.toVPackBits.right.get

hex"0b070141621903".bits.asVPack[Test].right.get.value
hex"0a".bits.asVPack[Test]

case class TestTrue(b: Boolean = true)
implicit val testTrueDecoder: VPackDecoder[TestTrue] = VPackRecord[TestTrue].decoderWithDefaults

hex"0b070141621903".bits.asVPack[TestTrue].right.get.value
hex"0a".bits.asVPack[TestTrue].right.get.value
