import avokka.velocypack._

import scala.util.Try

case class Test(b: Boolean)
implicit val testEncoder: VPackEncoder[Test] = VPackRecord[Try, Test].encoder
implicit val testDecoder: VPackDecoder[Try, Test] = VPackRecord[Try, Test].decoder

val ok = Test(true)

val bits = ok.toVPackBits.right.get

val testSt = testDecoder.state

val bitss = bits ++ Test(false).toVPackBits.right.get

val doubleDecoder = for {
  un <- testSt
  deux <- testSt
} yield (un, deux)

doubleDecoder.run(bitss)
