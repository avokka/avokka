package avokka.velocypack

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.EitherValues._

import java.net._

class VPackURIURLSpec extends AnyFlatSpec with VPackSpecTrait {

  "uri" should "encode to string" in {
    assertEncode(URI.create("mailto:ben"), VString("mailto:ben"))
    assertEncode(URI.create("../../test"), VString("../../test"))

    assertEncode(new URL("mailto:ben"), VString("mailto:ben"))
    assertEncode(new URL("http://test:100/docs/index.html"), VString("http://test:100/docs/index.html"))
  }

  "uri" should "decode from string" in {
    VPackDecoder[URI].decode(VString("mailto:ben")).value should be (a [URI])
    VPackDecoder[URI].decode(VString(":")).left.value should be (a [VPackError.Conversion])

    VPackDecoder[URL].decode(VString("http://test:100/docs/index.html")).value should be (a [URL])
    VPackDecoder[URL].decode(VString(":")).left.value should be (a [VPackError.Conversion])
  }

  "uri/url" should "fail with incoherent type" in {
    VDate(0).as[URI].left.value should be (a [VPackError.WrongType])
    VDate(0).as[URL].left.value should be (a [VPackError.WrongType])
  }
}
