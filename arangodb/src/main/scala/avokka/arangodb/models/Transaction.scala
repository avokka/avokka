package avokka.arangodb
package models

import avokka.arangodb.types.TransactionId
import avokka.velocypack._

/**
  * describing a transaction
  *
  * @param id identifier of the transaction
  * @param status status of the transaction “running”, “committed” or “aborted”
  */
case class Transaction(
    id: TransactionId,
    status: String
)

object Transaction {
  implicit val decoder: VPackDecoder[Transaction] = VPackDecoder.gen

  val KEY: String = "x-arango-trx-id"
}

