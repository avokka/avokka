package avokka.velocypack

import avokka.velocypack.VPackValue.{vpBool, vpString}
import avokka.velocypack.VelocypackArrayEncoder.{vpArray, vpArrayCompact}
import com.arangodb.velocypack.VPackSlice
import org.scalatest._
import scodec._
import scodec.bits._
import shapeless.{::, HNil}

class VelocypackArrayEncoderSpec extends FlatSpec with Matchers {

  val request: Encoder[String :: Boolean :: HNil] = vpArray(vpString :: vpBool :: HNil)
  val requests = vpArray(request :: request :: HNil)
  val compact = vpArrayCompact(vpString :: vpBool :: vpString :: vpBool :: HNil)

  "empty array" should "encode to 0x01" in {
    val encoder = vpArray[HNil, HNil](HNil)
    val result = encoder.encode(HNil)
    assert(result.isSuccessful)
    assertResult(hex"01")(result.require.bytes)
  }

  "array encoder" should "accept only encoders" in {
    assertTypeError("vpArray(String :: HNil)")
    assertCompiles("vpArray(VPackValue.vpBool :: HNil)")
  }

  "simple array encoder" should "return an encoder" in {
    val encoder = vpArray(VPackValue.vpBool :: HNil)
    assert(encoder.isInstanceOf[Encoder[Boolean :: HNil]])
  }

  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  "array encoders" should "conform velocypack" in {
    for {
      e <- compact.encode("a" :: false :: "b" * 10 :: true :: HNil)
      p = new VPackSlice(e.toByteArray)
    } yield println(e, p)

    for {
      e <- request.encode("a" * 200 :: true :: HNil)
      p = new VPackSlice(e.toByteArray)
    } yield println(e, e.take(100), p)

    for {
      e <- requests.encode(("a" :: true :: HNil) :: ("" :: false :: HNil) :: HNil)
      p = new VPackSlice(e.toByteArray)
    } yield println(e, p)

    for {
      e <- requests.encode(("abcdefghijklm" :: true :: HNil) :: ("nopqrstuvwxyz" :: false :: HNil) :: HNil)
      p = new VPackSlice(e.toByteArray)
    } yield println(e, p)
  }

}
