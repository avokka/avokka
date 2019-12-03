package avokka.velocypack.codecs

import avokka.velocypack.VPackMaxKey

/**
 * Codec of maximum
 *
 * 0x1f : maxKey, nonsensical value that compares > than all other values
 */
object VPackMaxKeyCodec extends VPackFlagCodecTrait[VPackMaxKey.type] {
  override val headByte = 0x1f
  override val provide = _ => VPackMaxKey
  override val errorMessage: String = "not vpack max key"
}
