package avokka.velocystream

import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VStreamMessageSpec extends AnyFlatSpec {

  it should "split in stream of chunks" in {
    val m = VStreamMessage(10, hex"aabbccddeeff")
    val chunks = m.chunks(1).toList
    assertResult(6)(chunks.length)
    assertResult(hex"aa")(chunks.head.data)
    assertResult(true)(chunks.head.header.x.first)
    assertResult(6)(chunks.head.header.x.index)

    val chunks2 = m.chunks(4).toList
    assertResult(2)(chunks2.length)
    val first = chunks2.head
    val second = chunks2.tail.head

    assertResult(hex"aabbccdd")(first.data)
    assertResult(true)(first.header.x.first)
    assertResult(2)(first.header.x.index)
    assertResult(10)(first.header.id)
    assertResult(6)(first.header.length)

    assertResult(hex"eeff")(second.data)
    assertResult(false)(second.header.x.first)
    assertResult(1)(second.header.x.index)
    assertResult(10)(second.header.id)
    assertResult(6)(second.header.length)

    val chunk1 = m.chunks(10).toVector
    assertResult(1)(chunk1.length)
    val one = chunk1.head
    assertResult(m.data)(one.data)
    assertResult(true)(one.header.x.first)
    assertResult(1)(one.header.x.index)
    assertResult(m.id)(one.header.id)
    assertResult(m.data.size)(one.header.length)
  }

}
