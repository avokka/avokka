package avokka.arangodb
package protocol

import avokka.velocypack._

object ProtocolCodec {
  lazy val requestRequestEncoder: VPackEncoder[ArangoRequest.Request] = VPackGeneric.tuple { r =>
    (r.version, r.`type`, r.database, r.requestType, r.request, r.parameters, r.meta)
  }

  lazy val requestAuthenticationEncoder: VPackEncoder[ArangoRequest.Authentication] = VPackGeneric.encoder[ArangoRequest.Authentication]

  lazy val requestHeaderEncoder: VPackEncoder[ArangoRequest.Header] = {
    case r: ArangoRequest.Request => requestRequestEncoder.encode(r)
    case a: ArangoRequest.Authentication => requestAuthenticationEncoder.encode(a)
  }

  lazy val responseHeaderDecoder: VPackDecoder[ArangoResponse.Header] = VPackGeneric.decoder[ArangoResponse.Header]
}
