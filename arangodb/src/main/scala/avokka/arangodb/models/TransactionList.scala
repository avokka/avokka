package avokka.arangodb.models

import avokka.arangodb.types.TransactionId
import avokka.velocypack._

final case class TransactionList(
    transactions: List[TransactionList.Transaction],
)

object TransactionList {

  case class Transaction(
                          id: TransactionId,
                          state: String
                        )

  object Transaction {
    implicit val decoder: VPackDecoder[Transaction] = VPackDecoder.derived
  }

  implicit val decoder: VPackDecoder[TransactionList] = VPackDecoder.derived
}

