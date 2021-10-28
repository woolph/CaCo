package at.woolph.caco.datamodel.sets

enum class Foil {
    NONFOIL, FOIL, PRERELASE_STAMPED_FOIL, STAMPED_NONFOIL, STAMPED_FOIL;

    val isFoil: Boolean
        get() = this == FOIL || this == PRERELASE_STAMPED_FOIL || this == STAMPED_FOIL

    val isStamped: Boolean
        get() = this == PRERELASE_STAMPED_FOIL || this == STAMPED_NONFOIL || this == STAMPED_FOIL
}
