package at.woolph.caco.datamodel.collection

enum class CardLanguage {
	UNKNOWN, ENGLISH, GERMAN,
	RUSSIAN, JAPANESE,
	SPANISH, PORTUGUESE, ITALIAN, FRENCH,
	CHINESE, CHINESE_TRADITIONAL, KOREAN;
	override fun toString() = when(this) {
		ENGLISH -> "en"
		GERMAN -> "de"
		RUSSIAN -> "ru"
		JAPANESE -> "ja"
		SPANISH -> "es"
		PORTUGUESE -> "pt"
		ITALIAN -> "it"
		FRENCH -> "fr"
		CHINESE -> "zhs"
		CHINESE_TRADITIONAL -> "zht"
		KOREAN -> "ko"
		else -> "?"
	}
}
