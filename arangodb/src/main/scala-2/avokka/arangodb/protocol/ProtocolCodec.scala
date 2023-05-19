package avokka.arangodb
package protocol

import avokka.velocypack._
import shapeless.{HNil, ::}

object ProtocolCodec {
  val requestRequestEncoder: VPackEncoder[ArangoRequest.Request] = VPackGeneric[ArangoRequest.Request].cmap { r =>
    r.version :: r.`type` :: r.database :: r.requestType :: r.request :: r.parameters :: r.meta :: shapeless.HNil
  }

  val requestAuthenticationEncoder: VPackEncoder[ArangoRequest.Authentication] = VPackGeneric[ArangoRequest.Authentication].encoder

  val requestHeaderEncoder: VPackEncoder[ArangoRequest.Header] = {
    case r: ArangoRequest.Request => requestRequestEncoder.encode(r)
    case a: ArangoRequest.Authentication => requestAuthenticationEncoder.encode(a)
  }

  val responseHeaderDecoder: VPackDecoder[ArangoResponse.Header] = VPackGeneric[ArangoResponse.Header].decoder
}
