package enumeratum

import avokka.velocypack._
import enumeratum.values.{ValueEnum, ValueEnumEntry}

trait VPackValueEnum[V, T <: ValueEnumEntry[V]] { self: ValueEnum[V, T] =>

  implicit def vpackEncoder(implicit encoder: VPackEncoder[V]): VPackEncoder[T] = velocypack.enumeratumValueVPackEncoder(encoder)
  implicit def vpackDecoder(implicit decoder: VPackDecoder[V]): VPackDecoder[T] = velocypack.enumeratumValueVPackDecoder(decoder, self)

}
