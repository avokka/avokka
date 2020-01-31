import avokka.velocypack._

val b: Boolean = true

b.toVPack
b.toVPackBits

val a: Seq[Int] = List(1,2)

a.toVPack
a.toVPackBits

import scodec.bits._

val bits = BitVector(hex"02043334")
bits.asVPack[Vector[Long]]
