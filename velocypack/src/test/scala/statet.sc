import avokka.velocypack._

case class Test(b: Boolean) derives VPackEncoder, VPackDecoder

val ok = Test(true)

val bits = ok.toVPackBits.right.get

val testSt = VPackDecoder[Test].state

val bitss = bits ++ Test(false).toVPackBits.right.get

val doubleDecoder = for {
  un <- testSt
  deux <- testSt
} yield (un, deux)

doubleDecoder.run(bitss)
