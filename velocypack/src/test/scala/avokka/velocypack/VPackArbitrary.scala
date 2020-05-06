package avokka.velocypack

import java.util.Date

import cats.data.Chain
import org.scalacheck._
import scodec.bits.ByteVector

trait VPackArbitrary {

  import Arbitrary._
  import VPack._

  val maxDepth = 4
  val maxArray = 10
  val maxObject = 10

  val genVNull: Gen[VNull.type] = Gen.const(VNull)
  val genVBoolean: Gen[VBoolean] = arbitrary[Boolean].map(VBoolean)

  val genVDouble: Gen[VDouble] = arbitrary[Double].map(VDouble)
  val genVDate: Gen[VDate] = arbitrary[Date].map { d => VDate(d.toInstant.toEpochMilli) }

  val genVSmallint: Gen[VSmallint] = Gen.choose[Byte](-6, 9).map(VSmallint.apply)
  val genVLong: Gen[VLong] = arbitrary[Long].map(VLong.apply)

  val genVString: Gen[VString] = arbitrary[String].map(VString)
  val genVBinary: Gen[VBinary] = Gen.listOf(arbitrary[Byte]).map { b => VBinary(ByteVector(b)) }

  val genVScalar: Gen[VPack] = Gen.oneOf(
    genVNull, genVBoolean, genVDouble, genVDate, genVSmallint, genVLong, genVString, genVBinary
  )

  private[this] def genAtDepth(depth: Int): Gen[VPack] = {
    if (depth > 0) Gen.frequency(
      (depth, genVScalar),
      (1, genVArray(depth)),
      (1, genVObject(depth)),
    ) else genVScalar
  }

  def genVArray(depth: Int = maxDepth): Gen[VArray] = {
    Gen.listOfN(maxArray, genAtDepth(depth - 1)).flatMap { vals =>
      VArray(vals.toVector)
    }
  }

  def genVObject(depth: Int = maxDepth): Gen[VObject] = {
    Gen.mapOfN(maxObject, Gen.zip(Gen.identifier, genAtDepth(depth - 1))).flatMap { vals =>
      VObject(vals)
    }
  }

  val genV: Gen[VPack] = genAtDepth(1)
}