package avokka.velocypack

import com.arangodb.velocypack.{VPack, VPackBuilder, VPackSlice}
import scodec._
import scodec.bits._
import scodec.codecs._

object codecs {

  def between[T : Numeric](codec: Codec[T], min: T, max: T): Codec[T] = {
    import Numeric.Implicits._
    import Ordering.Implicits._
    codec.exmap(
      i => if (min <= i && i <= max) Attempt.Successful(i - min) else Attempt.failure(Err("not in range")),
      d => if (d <= max - min) Attempt.Successful(min + d) else Attempt.failure(Err("not in range"))
    )
  }

  def bytesRequire(value: Long): Int = {
    if      (value > 0xffffffffffffffL) 8
    else if (value > 0xffffffffffffL) 7
    else if (value > 0xffffffffffL) 6
    else if (value > 0xffffffffL) 5
    else if (value > 0xffffffL) 4
    else if (value > 0xffffL) 3
    else if (value > 0xffL) 2
    else 1
  }

  def main(args: Array[String]): Unit = {
    val r40 = between(uint8L, 0x40, 0x45)


    println(r40.encode(0))
    println(r40.decode(hex"45".toBitVector))

  }

  def vpackSerialize[T : VelocypackEncoder](t: T): VPackSlice = {
    val builder = new VPackBuilder()
    implicitly[VelocypackEncoder[T]].encode(builder, t).slice()
  }
}
