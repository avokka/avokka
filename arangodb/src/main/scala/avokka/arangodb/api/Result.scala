package avokka.arangodb.api

import avokka.velocypack.VPackDecoder

final case class Result[T] ( result: T )

object Result {
  implicit def decoder[T: VPackDecoder]: VPackDecoder[Result[T]] = VPackDecoder.gen
}
