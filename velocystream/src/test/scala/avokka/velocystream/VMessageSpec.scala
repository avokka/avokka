package avokka.velocystream

import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VMessageSpec extends FlatSpec with Matchers {

  it should "split in stream of chunks" in {
    val m = VMessage(10, hex"aabbccddeeff")
    val chunks = m.chunks(1)
    assertResult(6)(chunks.length)
    assertResult(hex"aa")(chunks.head.data)
    assertResult(true)(chunks.head.isFirst)
    assertResult(6)(chunks.head.chunk)

    val chunks2 = m.chunks(4)
    assertResult(2)(chunks2.length)
    val first = chunks2.head
    val second = chunks2.tail.head

    assertResult(hex"aabbccdd")(first.data)
    assertResult(true)(first.isFirst)
    assertResult(2)(first.chunk)
    assertResult(10)(first.messageId)
    assertResult(6)(first.messageLength)

    assertResult(hex"eeff")(second.data)
    assertResult(false)(second.isFirst)
    assertResult(2)(second.chunk)
    assertResult(10)(second.messageId)
    assertResult(6)(second.messageLength)

    val chunk1 = m.chunks(10)
    assertResult(1)(chunk1.length)
    val one = chunk1.head
    assertResult(m.data)(one.data)
    assertResult(true)(one.isFirst)
    assertResult(1)(one.chunk)
    assertResult(m.id)(one.messageId)
    assertResult(m.data.size)(one.messageLength)
  }

}
