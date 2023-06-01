package avokka.arangodb
package models

import avokka.velocypack._

/**
  * the role of a server in a cluster
  *
  * @param role role of server
  */
case class ServerRole(
    role: String,
)

object ServerRole {

  implicit val decoder: VPackDecoder[ServerRole] = VPackDecoder.derived

}
