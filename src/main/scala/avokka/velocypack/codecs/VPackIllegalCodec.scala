package avokka.velocypack.codecs

import avokka.velocypack.VPackIllegal

/**
 * Codec of illegal flag
 *
 * 0x17 : illegal - this type can be used to indicate a value that is illegal in the embedding application
 */
object VPackIllegalCodec extends VPackFlagCodecTrait[VPackIllegal.type] {
  override val headByte = 0x17
  override val provide = _ => VPackIllegal
  override val errorMessage: String = "not vpack illegal"
}
