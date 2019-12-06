package avokka.velocystream

import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VStreamMessageSpec extends FlatSpec with Matchers {

  it should "split in stream of chunks" in {
    val m = VStreamMessage(10, hex"aabbccddeeff")
    val chunks = m.chunks(1)
    assertResult(6)(chunks.length)
    assertResult(hex"aa")(chunks.head.data)
    assertResult(true)(chunks.head.x.first)
    assertResult(6)(chunks.head.x.number)

    val chunks2 = m.chunks(4)
    assertResult(2)(chunks2.length)
    val first = chunks2.head
    val second = chunks2.tail.head

    assertResult(hex"aabbccdd")(first.data)
    assertResult(true)(first.x.first)
    assertResult(2)(first.x.number)
    assertResult(10)(first.messageId)
    assertResult(6)(first.messageLength)

    assertResult(hex"eeff")(second.data)
    assertResult(false)(second.x.first)
    assertResult(2)(second.x.number)
    assertResult(10)(second.messageId)
    assertResult(6)(second.messageLength)

    val chunk1 = m.chunks(10)
    assertResult(1)(chunk1.length)
    val one = chunk1.head
    assertResult(m.data)(one.data)
    assertResult(true)(one.x.first)
    assertResult(1)(one.x.number)
    assertResult(m.id)(one.messageId)
    assertResult(m.data.size)(one.messageLength)
  }

}