/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.decks

enum class Format(
    val shortName: String,
) {
    Unknown("???"),
    Standard("STD"),
    Historic("HSC"),
    Commander("CMD"),
    PauperCommander("PCMD"),
    Oathbreaker("OBR"),
    Modern("MDN"),
    Pauper("PPR"),
    Legacy("LGC"),
    Vintage("VTG"),
    Pioneer("PNR"),
    Brawl("BRL"),
    Cube("CUB"),
}
