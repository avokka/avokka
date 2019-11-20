package avokka.velocypack

import com.arangodb.velocypack.{VPackBuilder, VPackSlice, ValueType}
import scodec._

object codecs {

  def between[T : Numeric](codec: Codec[T], min: T, max: T): Codec[T] = {
    import Numeric.Implicits._
    import Ordering.Implicits._
    codec.exmap(
      i => if (min <= i && i <= max) Attempt.Successful(i - min) else Attempt.failure(Err("not in range")),
      d => if (d <= max - min) Attempt.Successful(min + d) else Attempt.failure(Err("not in range"))
    )
  }

  def bytesRequire(value: Long, signed: Boolean, range: Seq[Int] = 1 to 8): Int = {
    range.find { bytes =>
      val bits = bytes * 8
      val max = (1L << (if (signed) bits - 1 else bits)) - 1
      val min = if (signed) -(1L << (bits - 1)) else 0
      min <= value && value <= max
    }.getOrElse(0)
  }

  def vpackSerialize[T : VelocypackEncoder](t: T): VPackSlice = {
    val builder = new VPackBuilder()
    implicitly[VelocypackEncoder[T]].encode(builder, t).slice()
  }

  implicit object VelocypackStringEncoder extends VelocypackEncoder[String] {
    override def encode(builder: VPackBuilder, t: String): VPackBuilder = builder.add(t)
  }
  implicit object VelocypackBooleanEncoder extends VelocypackEncoder[Boolean] {
    override def encode(builder: VPackBuilder, t: Boolean): VPackBuilder = builder.add(t)
  }
  implicit object VelocypackIntEncoder extends VelocypackEncoder[Int] {
    override def encode(builder: VPackBuilder, t: Int): VPackBuilder = builder.add(t: java.lang.Integer)
  }
  implicit object VelocypackLongEncoder extends VelocypackEncoder[Long] {
    override def encode(builder: VPackBuilder, t: Long): VPackBuilder = builder.add(t: java.lang.Long)
  }
  implicit object VelocypackShortEncoder extends VelocypackEncoder[Short] {
    override def encode(builder: VPackBuilder, t: Short): VPackBuilder = builder.add(t: java.lang.Short)
  }
  implicit object VelocypackDoubleEncoder extends VelocypackEncoder[Double] {
    override def encode(builder: VPackBuilder, t: Double): VPackBuilder = builder.add(t: java.lang.Double)
  }
  implicit object VelocypackFloatEncoder extends VelocypackEncoder[Float] {
    override def encode(builder: VPackBuilder, t: Float): VPackBuilder = builder.add(t: java.lang.Float)
  }
  implicit object VelocypackCharEncoder extends VelocypackEncoder[Char] {
    override def encode(builder: VPackBuilder, t: Char): VPackBuilder = builder.add(t: java.lang.Character)
  }

  implicit def velocypackSeqEncoder[T](implicit encoder: VelocypackEncoder[T]): VelocypackEncoder[Seq[T]] = (builder: VPackBuilder, t: Seq[T]) => {
    t.foldLeft(builder.add(ValueType.ARRAY)) {
      (builder, element) => encoder.encode(builder, element)
    }.close()
  }

  implicit def velocypackMapEncoder[T](implicit encoder: VelocypackEncoder[T]): VelocypackEncoder[Map[String, T]] = (builder: VPackBuilder, t: Map[String, T]) => {
    t.foldLeft(builder.add(ValueType.OBJECT)) {
      case (builder, (key, value)) => builder.add(key, vpackSerialize(value)(encoder))
    }.close()
  }
}
