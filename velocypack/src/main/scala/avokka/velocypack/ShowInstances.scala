package avokka.velocypack

import java.time.Instant

import cats.Show
import VPack._
// import cats.syntax.contravariant._

trait ShowInstances {

  implicit val vpackShow: Show[VPack] = Show.show {
    case v @ VNone        => vpackNoneShow.show(v)
    case v @ VIllegal     => vpackIllegalShow.show(v)
    case v @ VNull        => vpackNullShow.show(v)
    case v : VBoolean     => vpackBooleanShow.show(v)
    case v : VDouble      => vpackDoubleShow.show(v)
    case VDate(value)     => s""""${Instant.ofEpochMilli(value)}""""
    case VMinKey          => "-Infinity"
    case VMaxKey          => "Infinity"
    case VSmallint(value) => value.toString
    case VLong(value)     => value.toString
    case VString(value)   => s""""$value""""
    case VBinary(value)   => s""""${value.toHex}""""
    case v: VArray        => vpackArrayShow.show(v)
    case v: VObject       => vpackObjectShow.show(v)
  }

  implicit val vpackNoneShow: Show[VNone.type] = Show.show { _ => "undefined" }
  implicit val vpackIllegalShow: Show[VIllegal.type] = Show.show { _ => "undefined" }
  implicit val vpackNullShow: Show[VNull.type] = Show.show { _ => "null" }

  implicit val vpackBooleanShow: Show[VBoolean] = Show.show { v => if (v.value) "true" else "false" }

  implicit val vpackDoubleShow: Show[VDouble] = Show.show { v => v.value.toString }

  implicit val vpackArrayShow: Show[VArray] = Show.show { v =>
    v.values.map(vpackShow.show).toList.mkString("[", ",", "]")
  }

  implicit val vpackObjectShow: Show[VObject] = Show.show { v =>
    v.values.mapValues(vpackShow.show).map { case (key, value) => s""""$key":$value""" }.mkString("{", ",", "}")
  }

  implicit val vpackErrorShow: Show[VPackError] = Show.fromToString[VPackError]
}
