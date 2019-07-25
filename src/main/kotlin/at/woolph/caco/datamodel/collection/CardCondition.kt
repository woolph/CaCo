package at.woolph.caco.datamodel.collection

enum class CardCondition {
    UNKNOWN, NEAR_MINT, EXCELLENT, GOOD, PLAYED, POOR;
    override fun toString() = when(this) {
        NEAR_MINT -> "NM"
        EXCELLENT -> "EX"
        GOOD -> "GD"
        PLAYED -> "PL"
        POOR -> "PR"
        else -> "?"
    }
}
