package avokka.arangodb.fs2

import avokka.velocystream._
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.OptionValues._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import scodec.bits.ByteVector

class VChunkAssemblerTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  val singleC: VStreamChunk = VStreamChunk(VStreamChunkHeader(VStreamChunkX(first = true, index = 1), 1, 1), ByteVector(1))
  val singleM: VStreamMessage = VStreamMessage(1, ByteVector(1))

  "single chunk is returned immediately" in {
    (for {
      assembler <- VChunkAssembler[IO]
      msg <- assembler.push(singleC).value
      rs <- assembler.size
    } yield msg -> rs).asserting { r =>
      r._1.value shouldBe singleM
      r._2 shouldBe 0
    }
  }

  val splitC1: VStreamChunk = VStreamChunk(VStreamChunkHeader(VStreamChunkX(first = true, index = 2), 1, 2), ByteVector(1))
  val splitC2: VStreamChunk = VStreamChunk(VStreamChunkHeader(VStreamChunkX(first = false, index = 1), 1, 2), ByteVector(2))
  val splitM: VStreamMessage = VStreamMessage(1, ByteVector(1, 2))

  "chunks are stacked until complete" in {
    (for {
      assembler <- VChunkAssembler[IO]
      push1 <- assembler.push(splitC1).value
      size1 <- assembler.size
      push2 <- assembler.push(splitC2).value
      size2 <- assembler.size
    } yield (push1, size1, push2, size2)).asserting {
      case (p1, s1, p2, s2) =>
        p1 shouldBe empty
        s1 shouldBe 1
        p2.value shouldBe splitM
        s2 shouldBe 0
    }
  }
  "chunks in any order" in {
    (for {
      assembler <- VChunkAssembler[IO]
      push1 <- assembler.push(splitC2).value
      size1 <- assembler.size
      push2 <- assembler.push(splitC1).value
      size2 <- assembler.size
    } yield (push1, size1, push2, size2)).asserting {
      case (p1, s1, p2, s2) =>
        p1 shouldBe empty
        s1 shouldBe 1
        p2.value shouldBe splitM
        s2 shouldBe 0
    }
  }

  val splitC1b: VStreamChunk = VStreamChunk(VStreamChunkHeader(VStreamChunkX(first = true, index = 2), 2, 2), ByteVector(10))
  val splitC2b: VStreamChunk = VStreamChunk(VStreamChunkHeader(VStreamChunkX(first = false, index = 1), 2, 2), ByteVector(20))
  val splitMb: VStreamMessage = VStreamMessage(2, ByteVector(10, 20))

  "multiplex chunks" in {
    (for {
      assembler <- VChunkAssembler[IO]
      push1 <- assembler.push(splitC1).value
      size1 <- assembler.size
      push1b <- assembler.push(splitC1b).value
      size1b <- assembler.size
      push2 <- assembler.push(splitC2).value
      size2 <- assembler.size
      push2b <- assembler.push(splitC2b).value
      size2b <- assembler.size
    } yield (push1, size1, push1b, size1b, push2, size2, push2b, size2b)).asserting {
      case (p1, s1, p1b, s1b, p2, s2, p2b, s2b) =>
        p1 shouldBe empty
        s1 shouldBe 1
        p1b shouldBe empty
        s1b shouldBe 2
        p2.value shouldBe splitM
        s2 shouldBe 1
        p2b.value shouldBe splitMb
        s2b shouldBe 0
    }
  }

}
