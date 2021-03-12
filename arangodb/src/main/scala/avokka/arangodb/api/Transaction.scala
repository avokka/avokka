package avokka.arangodb
package api

import avokka.velocypack._

/**
  * describing a transaction
  *
  * @param id identifier of the transaction
  * @param status status of the transaction “running”, “committed” or “aborted”
  */
case class Transaction(
    id: String,
    status: String
)

object Transaction {
  implicit val decoder: VPackDecoder[Transaction] = VPackDecoder.gen
}

