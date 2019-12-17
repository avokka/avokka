package avokka.velocypack

import java.time.Instant

import VPack._
import cats.Show
// import cats.syntax.show._
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.ByteVector

class VPackShowSpec extends FlatSpec with Matchers {
  val s: Show[VPack] = implicitly

  "show" should "produce json" in {
    println(s.show(VDate(Instant.now().toEpochMilli)))
    println(s.show(VBinary(ByteVector(50, 10))))
    println(s.show(VString("a")))
    // println(VString("a").show)
    println(s.show(VArray(VString("a"), VTrue, VSmallint(1))))
    println(s.show(VObject(Map("b" -> VTrue, "a" -> VArray(VSmallint(0), VSmallint(1))))))
    println(s.show(VDouble(12.34)))
  }
}
