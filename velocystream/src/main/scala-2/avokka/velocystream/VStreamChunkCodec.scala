package avokka.velocystream

import scodec.Codec
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import shapeless.{::, HNil}

trait VStreamChunkCodec { self: VStreamChunk.type =>
  val codec: Codec[VStreamChunk] = uint32L.consume { l =>
    VStreamChunkHeader.codec :: fixedSizeBytes(l - dataOffset, bytes)
  } {
    case _ :: bv :: HNil => bv.size + dataOffset
  }.as
}
