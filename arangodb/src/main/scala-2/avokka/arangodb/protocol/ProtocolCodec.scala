package avokka.arangodb
package protocol

import avokka.velocypack._
import shapeless.{HNil, ::}

object ProtocolCodec {
  lazy val requestRequestEncoder: VPackEncoder[ArangoRequest.Request] = VPackGeneric[ArangoRequest.Request].cmap { r =>
    r.version :: r.`type` :: r.database :: r.requestType :: r.request :: r.parameters :: r.meta :: HNil
  }

  lazy val requestAuthenticationEncoder: VPackEncoder[ArangoRequest.Authentication] = VPackGeneric[ArangoRequest.Authentication].encoder

  lazy val requestHeaderEncoder: VPackEncoder[ArangoRequest.Header] = {
    case r: ArangoRequest.Request => requestRequestEncoder.encode(r)
    case a: ArangoRequest.Authentication => requestAuthenticationEncoder.encode(a)
  }

  lazy val responseHeaderDecoder: VPackDecoder[ArangoResponse.Header] = VPackGeneric[ArangoResponse.Header].decoder
}
