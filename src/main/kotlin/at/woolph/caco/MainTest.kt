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
			val commander = deckDoc.selectFirst("div.deck_info_widget").let {
				val regexCommander = Regex("<div class=\"half_min_page indented_content\"> <span class=\"dt note\">Commander:</span>\\s*<div class=\"card inline_block\">\\s*<a class=\"simple\" href=\"[^\"]*\" target=\"_blank\">(.*)</a>")
				regexCommander.find(it.html())?.groups?.get(1)?.value // TODO commander eleganter herauslesen
			}
			println("Commander:")
			println("1 $commander")

			println("Mainboard:")
			deckDoc.selectFirst("table.main").select("tr[id]").forEach {
				val count = it.selectFirst("td.card_count").text()
				val cardName = it.selectFirst("td.card_name").selectFirst("a").text()

				if(cardName != commander)
					println("$count $cardName")
			}

			println(if(commander != null) "Maybeboard:" else "Sideboard:")
			deckDoc.selectFirst("table.sideboard").select("tr[id]").forEach {
				val count = it.selectFirst("td.card_count").text()
				val cardName = it.selectFirst("td.card_name").selectFirst("a").text()

				println("$count $cardName")
			}

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
