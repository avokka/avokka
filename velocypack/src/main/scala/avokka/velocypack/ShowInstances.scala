package avokka.velocypack

import java.time.Instant

import cats.Show
// import cats.syntax.contravariant._

trait ShowInstances {
  import VPack._

  implicit val vpackShow: Show[VPack] = Show.show {
    case VNone            => "undefined"
    case VIllegal         => "undefined"
    case VNull            => "null"
    case VBoolean(value)  => if (value) "true" else "false"
    case VDouble(value)   => value.toString
    case VDate(value)     => s""""${Instant.ofEpochMilli(value)}""""
    case VMinKey          => "-Infinity"
    case VMaxKey          => "Infinity"
    case VSmallint(value) => value.toString
    case VLong(value)     => value.toString
    case VString(value)   => s""""$value""""
    case VBinary(value)   => s""""${value.toHex}""""
    case VArray(values)   => values.map(vpackShow.show).toList.mkString("[", ",", "]")
    case VObject(values) =>
      values.mapValues(vpackShow.show).map { case (k, v) => s""""$k":$v""" }.mkString("{", ",", "}")
  }

//  implicit val vpackStringShow: Show[VString] = vpackShow.narrow
}
