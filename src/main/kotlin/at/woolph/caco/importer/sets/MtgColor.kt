package at.woolph.caco.importer.sets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MtgColor {
    @SerialName("W") White,
    @SerialName("U") Blue,
    @SerialName("B") Black,
    @SerialName("R") Red,
    @SerialName("G") Green,
    @SerialName("C") Colorless,
    @SerialName("T") Tap,
}