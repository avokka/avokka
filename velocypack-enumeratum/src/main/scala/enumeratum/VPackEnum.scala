package enumeratum

import avokka.velocypack._

trait VPackEnum[T <: EnumEntry] { self: Enum[T] =>

  implicit val vpackEncoder: VPackEncoder[T] = velocypack.enumeratumVPackEncoder
  implicit val vpackDecoder: VPackDecoder[T] = velocypack.enumeratumVPackDecoder(self)

  implicit val vpackKeyEncoder: VPackKeyEncoder[T] = velocypack.enumeratumVPackKeyEncoder
  implicit val vpackKeyDecoder: VPackKeyDecoder[T] = velocypack.enumeratumVPackKeyDecoder(self)

}
