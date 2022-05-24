package at.woolph.caco.cli

import org.jsoup.Jsoup
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@ShellComponent
class DeckboxDeckImporter(
    val inputReader: InputReader,
    val output: Output,
) {
    @ShellMethod("Tries to import all the decklist of the specified user.")
    fun importDeckboxDecks(userName: String) {
        val baseUrl = "https://deckbox.org"
        val doc = Jsoup.connect("$baseUrl/users/$userName").get()

        doc.select("li.deck").forEach {
            it.select("a[data-title]").forEach {
                val link = "$baseUrl${it.attr("href")}"
                val deckName = it.attr("data-title")
                output.println(deckName)
                output.println(link)

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

                output.println("Format: $format")
                if (format == "com" || format == "bra" || format == "oat") {
                    output.println("Commander:")
                    output.println("1 $commander")
                }

                output.println("Mainboard:")
                mainboard.forEach { (cardName, count) -> println("$count $cardName") }

                output.println(if (format == "com" || format == "bra" || format == "oat") "Maybeboard:" else "Sideboard:")
                sideboard.forEach { (cardName, count) -> println("$count $cardName") }

//                print("Press Enter to continue: ")
//                readLine()
                inputReader.prompt("Press Enter to continue: ")
            }
        }
    }
}
