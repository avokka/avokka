package avokka.arangodb.api

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
    implicit val decoder: VPackDecoder[Transaction] = VPackDecoder.gen
  }

  implicit val decoder: VPackDecoder[TransactionList] = VPackDecoder.gen
}

