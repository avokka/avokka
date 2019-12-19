package avokka.arangodb

import avokka.velocypack.VPackError

trait ArangoError

object ArangoError {
  case class VPack(error: VPackError) extends ArangoError
  case class Head(header: Response.Header) extends ArangoError
  case class Resp(header: Response.Header, error: ResponseError) extends ArangoError
}
