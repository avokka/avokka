package avokka.velocypack

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

  def main(args: Array[String]): Unit = {
    val r40 = {
      between(uint8L, 0x40, 0x45)
    }

    println(r40.encode(0))
    println(r40.decode(hex"45".toBitVector))

  }
}
