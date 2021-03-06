package at.woolph.caco.datamodel.decks

enum class Format(val shortName: String) {
	Unknown("???"),
	Standard("STD"),
	Historic("HSC"),
	Commander("CMD"),
	Modern("MDN"),
	Pauper("PPR"),
	Legacy("LGC"),
	Vintage("VTG"),
	Pioneer("PNR")
}
