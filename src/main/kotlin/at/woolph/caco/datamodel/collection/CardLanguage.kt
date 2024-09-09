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

	companion object {
		fun parse(languageCode: String) = when(languageCode.lowercase()) {
			"en" -> ENGLISH
			"de" -> GERMAN
			"ru" -> RUSSIAN
			"ja" -> JAPANESE
			"es" -> SPANISH
			"pt" -> PORTUGUESE
			"it" -> ITALIAN
			"fr" -> FRENCH
			"zhs" -> CHINESE
			"zht" -> CHINESE_TRADITIONAL
			"ko" -> KOREAN
			else -> UNKNOWN
		}
	}
}
