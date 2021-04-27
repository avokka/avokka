package avokka.velocypack

import io.circe.Json

package object circe {

  implicit lazy val jsonVPackEncoder: VPackEncoder[Json] = _.fold[VPack](
    jsonNull = VNull,
    jsonBoolean = VPackEncoder.booleanEncoder.encode,
    jsonNumber = n => {
      n.toLong.map(VPackEncoder.longEncoder.encode)
        .getOrElse {
          VPackEncoder.doubleEncoder.encode(n.toDouble)
        }
    },
    jsonString = VString,
    jsonArray = a => VArray(a.map(jsonVPackEncoder.encode)),
    jsonObject = o => VObject(o.toIterable.map {
      case (key, json) => key -> jsonVPackEncoder.encode(json)
    }.toMap)
  )

}
