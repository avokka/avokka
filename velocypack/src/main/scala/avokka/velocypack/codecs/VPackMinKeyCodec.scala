package avokka.velocypack.codecs

import avokka.velocypack.VPackMinKey

/**
 * Codec of minimum
 *
 * 0x1e : minKey, nonsensical value that compares < than all other values
 */
object VPackMinKeyCodec extends VPackFlagCodecTrait[VPackMinKey.type] {
  override val headByte = 0x1e
  override val provide = _ => VPackMinKey
  override val errorMessage: String = "not vpack min key"
}
