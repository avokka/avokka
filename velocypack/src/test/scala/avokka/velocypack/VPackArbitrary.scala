package avokka.velocypack

import java.util.Date

import cats.data.Chain
import org.scalacheck._
import scodec.bits.ByteVector

trait VPackArbitrary {

  import Arbitrary._
  import VPack._

  val maxDepth = 5
  val maxArray = 10

  val genVNull: Gen[VNull.type] = Gen.const(VNull)
  val genVBoolean: Gen[VBoolean] = arbitrary[Boolean].map(VBoolean)

  val genVDouble: Gen[VDouble] = arbitrary[Double].map(VDouble)
  val genVDate: Gen[VDate] = arbitrary[Date].map { d => VDate(d.toInstant.toEpochMilli) }

  val genVSmallint: Gen[VSmallint] = Gen.choose[Byte](-6, 9).map(VSmallint.apply)
  val genVLong: Gen[VLong] = arbitrary[Long].map(VLong.apply)

  val genVString: Gen[VString] = arbitrary[String].map(VString)
  val genVBinary: Gen[VBinary] = Gen.listOf(arbitrary[Byte]).map { b => VBinary(ByteVector(b)) }

  private val scalar: Gen[VPack] = Gen.oneOf(
    genVNull, genVBoolean, genVDouble, genVDate, genVString, genVSmallint, genVLong, genVString, genVBinary
  )

  private[this] def genAtDepth(depth: Int): Gen[VPack] = {
    if (depth < maxDepth) Gen.frequency((1, genVArray(depth + 1)), (maxDepth - depth, scalar))
    else scalar
  }

  def genVArray(depth: Int = 1): Gen[VArray] = {
    Gen.listOfN(maxArray, genAtDepth(depth)).flatMap { vals =>
      VArray(Chain.fromSeq(vals.toVector))
    }
  }

  val genV: Gen[VPack] = genAtDepth(1)
}