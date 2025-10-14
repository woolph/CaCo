/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.collection

enum class CardCondition {
    UNKNOWN,
    NEAR_MINT,
    EXCELLENT,
    GOOD,
    PLAYED,
    POOR,
    ;

    override fun toString() =
        when (this) {
            NEAR_MINT -> "NM"
            EXCELLENT -> "EX"
            GOOD -> "GD"
            PLAYED -> "PL"
            POOR -> "PR"
            else -> "?"
        }

    companion object {
        fun parse(languageCode: String) =
            when (languageCode.uppercase()) {
                "NM" -> NEAR_MINT
                "EX" -> EXCELLENT
                "GD" -> GOOD
                "PL" -> PLAYED
                "PR" -> POOR
                else -> UNKNOWN
            }
    }

    fun isBetterThanOrEqual(other: CardCondition) =
        when {
            this == UNKNOWN && other != UNKNOWN -> false
            else -> ordinal <= other.ordinal
        }
}
