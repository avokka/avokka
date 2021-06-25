package avokka.arangodb.models

import enumeratum._
import enumeratum.values._

sealed abstract class WalType(val value: Int) extends IntEnumEntry

object WalType extends IntEnum[WalType] with VPackValueEnum[Int, WalType] {

  case object CreateDatabase extends WalType(1100)
  case object DropDatabase extends WalType(1101)
  case object CreateCollection extends WalType(2000)
  case object DropCollection extends WalType(2001)
  case object RenameCollection extends WalType(2002)
  case object ChangeCollection extends WalType(2003)
  case object TruncateCollection extends WalType(2004)

  case object CreateIndex extends WalType(2100)
  case object DropIndex extends WalType(2101)
  case object CreateView extends WalType(2110)
  case object DropView extends WalType(2111)
  case object ChangeView extends WalType(2112)
  case object StartTransaction extends WalType(2200)
  case object CommitTransaction extends WalType(2201)
  case object AbortTransaction extends WalType(2202)
  case object InsertReplaceDocument extends WalType(2300)
  case object RemoveDocument extends WalType(2302)

  override val values = findValues
}
