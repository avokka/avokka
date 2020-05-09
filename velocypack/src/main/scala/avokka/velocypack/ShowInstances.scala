package avokka.velocypack

import java.time.Instant

import cats.Show
import cats.instances.double._
import cats.syntax.contravariant._
import VPack._

trait ShowInstances {

  implicit val vpackShow: Show[VPack] = Show.show {
    case v : VNone.type    => vpackNoneShow.show(v)
    case v : VIllegal.type => vpackIllegalShow.show(v)
    case v : VNull.type    => vpackNullShow.show(v)
    case v : VBoolean      => vpackBooleanShow.show(v)
    case v : VDouble       => vpackDoubleShow.show(v)
    case v : VDate         => vpackDateShow.show(v)
    case v : VMinKey.type  => vpackMinKeyShow.show(v)
    case v : VMaxKey.type  => vpackMaxKeyShow.show(v)
    case v : VSmallint     => vpackSmallIntShow.show(v)
    case v : VLong         => vpackLongShow.show(v)
    case v : VString       => vpackStringShow.show(v)
    case v : VBinary       => vpackBinaryShow.show(v)
    case v : VArray        => vpackArrayShow.show(v)
    case v : VObject       => vpackObjectShow.show(v)
  }

  implicit val vpackNoneShow: Show[VNone.type] = Show.show { _ => "undefined" }
  implicit val vpackIllegalShow: Show[VIllegal.type] = Show.show { _ => "undefined" }
  implicit val vpackNullShow: Show[VNull.type] = Show.show { _ => "null" }

  implicit val vpackBooleanShow: Show[VBoolean] = Show.show {
    case VFalse => "false"
    case VTrue  => "true"
  }

  implicit val vpackDoubleShow: Show[VDouble] = Show[Double].contramap(_.value)

  implicit val vpackDateShow: Show[VDate] = Show.show { v => s""""${Instant.ofEpochMilli(v.value)}"""" }

  implicit val vpackMinKeyShow: Show[VMinKey.type] = Show.show { _ => "-Infinity" }
  implicit val vpackMaxKeyShow: Show[VMaxKey.type] = Show.show { _ => "Infinity" }

  implicit val vpackSmallIntShow: Show[VSmallint] = Show.show { v => v.value.toString }
  implicit val vpackLongShow: Show[VLong] = Show.show { v => v.value.toString }

  implicit val vpackStringShow: Show[VString] = Show.show { v => s""""${v.value}"""" }
  implicit val vpackBinaryShow: Show[VBinary] = Show.show { v => s""""${v.value.toHex}"""" }

  implicit val vpackArrayShow: Show[VArray] = Show.show { v =>
    v.values.map(vpackShow.show).toList.mkString("[", ",", "]")
  }

  implicit val vpackObjectShow: Show[VObject] = Show.show { v =>
    v.values.map { case (key, value) =>
      """"%s":%s""".format(key, vpackShow.show(value))
    }.mkString("{", ",", "}")
  }

  implicit val vpackErrorShow: Show[VPackError] = Show.fromToString[VPackError]
}
