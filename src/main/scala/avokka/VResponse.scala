package avokka

case class VResponse
(
  version: Int,
  `type`: Int,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)
