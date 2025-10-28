package at.woolph.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class UriSerializer() : KSerializer<Uri> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("at.woolph.utils.UriSerializer", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Uri) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Uri =
    Uri(decoder.decodeString())
}
