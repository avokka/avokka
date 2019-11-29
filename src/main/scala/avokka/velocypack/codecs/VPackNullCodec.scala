package avokka.velocypack.codecs

import avokka.velocypack.VPackNull

/**
 * Codec of null
 *
 * 0x18 : null
 */
object VPackNullCodec extends VPackFlagCodecTrait[VPackNull.type] {
  override val headByte = 0x18
  override val provide = _ => VPackNull
  override val errorMessage: String = "not vpack null"
}
