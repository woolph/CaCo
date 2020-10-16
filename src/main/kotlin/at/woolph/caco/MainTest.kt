package at.woolph.caco

import org.jsoup.Jsoup
import kotlin.system.exitProcess


/**
 * @see https://github.com/JetBrains/Exposed
 */
fun main(args: Array<String>) {
	val baseUrl = "https://deckbox.org"
	val userName = "woolph"

	val doc = Jsoup.connect("$baseUrl/users/$userName").get()

	doc.select("li.deck").forEach {
		it.select("a[data-title]").forEach {
			val link = "$baseUrl${it.attr("href")}"
			val deckName = it.attr("data-title")
			println(deckName)
			println(link)

			val deckDoc = Jsoup.connect(link).get()
			val (format, commander) = deckDoc.selectFirst("div.deck_info_widget").let {
				it.selectFirst("span:matches(Format:) ~ span.variant")?.text() to it.selectFirst("span:matches(Commander:) ~ div > a")?.text()
			}
			val mainboard = deckDoc.selectFirst("table.main").select("tr[id]").mapNotNull {
				val count = it.selectFirst("td.card_count")?.text()
				val cardName = it.selectFirst("td.card_name")?.text()

				if(cardName != commander) cardName to count else null
			}.associate { it }
			val sideboard = deckDoc.selectFirst("table.sideboard").select("tr[id]").mapNotNull {
				val count = it.selectFirst("td.card_count").text()
				val cardName = it.selectFirst("td.card_name a").text()

				if(cardName != commander) cardName to count else null
			}.associate { it }

			println("Format: $format")
			if (format == "com" || format == "bra" || format == "oat") {
				println("Commander:")
				println("1 $commander")
			}

			println("Mainboard:")
			mainboard.forEach { (cardName, count) -> println("$count $cardName") }

			println(if (format == "com" || format == "bra" || format == "oat") "Maybeboard:" else "Sideboard:")
			sideboard.forEach { (cardName, count) -> println("$count $cardName") }

			print("Press Enter to continue: ")
			readLine()
		}
	}

//	//val regex = Regex("<li id=\"deck_(\\d*)\" class=\"submenu_entry deck \"><a href=\"/sets/(?:1)\" data-title=\"([^\"]*)\" ")
//	val regexDeck = Regex("<li id=\"deck_(\\d*)\" class=\"submenu_entry deck \"><a href=\"/sets/(\\d*)\" data-title=\"([^\"]*)\" ")
//
//	val regexCard = Regex("<td class=\"card_count\">(\\d)*</td> \n          <td class=\"card_name\"> <a class=\"simple\" href=\"[^\"]*\" target=\"_blank\">(.*)</a>")
//	html.lineSequence().forEach {
//		val result = regexDeck.find(it)
//		result?.groups?.let {
//			val deckId = it[2]?.value
//			val deckTitle = it[3]?.value
//			println("$deckId $deckTitle")
//
//
//			println("https://deckbox.org/sets/$deckId")
//			val deckDoc = Jsoup.connect("https://deckbox.org/sets/$deckId").get()
//			val deckHtml = deckDoc.outerHtml()
//			println(deckHtml)
//
//			val commander = regexCommander.find(deckHtml)?.groups?.get(1)?.value
//			println("Commander:")
//			println("1 $commander")
//
//			val cards = regexCard.findAll(deckHtml).mapNotNull {
//				val count = it.groups?.get(1)?.value?.toInt() ?: 0
//				val cardName = it.groups?.get(2)?.value
//
//				if(cardName != commander) cardName?.let { it to count } else null
//			}.associate { it }
//
//			println("Main deck:")
//			cards.forEach { (name, count) ->
//				println("$count $name")
//			}
//			println(if(commander != null) "Maybeboard:" else "Sideboard:")
//
//			print("Press Enter to continue: ")
//			readLine()
//		}
//	}
}
