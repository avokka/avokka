package avokka.velocypack.codecs

import avokka.velocypack.VPackIllegal

object VPackIllegalCodec extends VPackFlagCodecTrait[VPackIllegal.type] {
  override val headByte = 0x17
  override val provide = _ => VPackIllegal
  override val errorMessage: String = "not vpack illegal"
}
