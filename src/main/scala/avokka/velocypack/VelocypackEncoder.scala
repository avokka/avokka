package avokka.velocypack

import com.arangodb.velocypack.{VPackBuilder, VPackSlice}

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find VelocypackEncoder for ${T}")
trait VelocypackEncoder[T] {
  def encode(builder: VPackBuilder, t: T): VPackBuilder
}
