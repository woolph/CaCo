package at.woolph.caco.datamodel.collection

enum class CardLanguage {
	UNKNOWN, ENGLISH, GERMAN, SPANISH, JAPANESE, RUSSIAN, CHINESE, CHINESE_TRADITIONL, KOREAN, ITALIAN, FRENCH, PORTUGUESE;
	override fun toString() = when(this) {
		ENGLISH -> "en"
		GERMAN -> "de"
		SPANISH -> "es"
		KOREAN -> "ko"
		ITALIAN -> "it"
		FRENCH -> "fr"
		PORTUGUESE -> "pt"
		JAPANESE -> "ja"
		RUSSIAN -> "ru"
		CHINESE -> "zhs"
		CHINESE_TRADITIONL -> "zht"
		else -> "?"
	}
}
