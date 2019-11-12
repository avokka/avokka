package avokka

import com.arangodb.velocypack.VPack
import scodec.bits.ByteVector

object VPackPlay {
  def main(args: Array[String]): Unit = {
    val vpack = new VPack.Builder().build()

    val t = VResponse(1, 1, 1, Map("a" -> "b", "c" -> "d"))

    val gen = vpack.serialize(t)

    println(ByteVector.view(gen.getBuffer, gen.getStart, gen.getByteSize))

    println(gen.toString)

    val o = vpack.deserialize(gen, classOf[VResponse])

    println(o)

  }

}
