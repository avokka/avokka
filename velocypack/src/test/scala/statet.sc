import avokka.velocypack._

case class Test(b: Boolean)
implicit val testEncoder: VPackEncoder[Test] = VPackRecord[Test].encoder
implicit val testDecoder: VPackDecoder[Test] = VPackRecord[Test].decoder

val ok = Test(true)

val bits = ok.toVPackBits.right.get

val testSt = testDecoder.state

val bitss = bits ++ Test(false).toVPackBits.right.get

val doubleDecoder = for {
  un <- testSt
  deux <- testSt
} yield (un, deux)

doubleDecoder.run(bitss)
