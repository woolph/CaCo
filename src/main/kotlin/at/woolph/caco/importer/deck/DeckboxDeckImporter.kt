package at.woolph.caco.importer.deck

import at.woolph.caco.cli.DeckList
import at.woolph.caco.datamodel.decks.Format
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL

class DeckboxDeckImporter(
    val terminal: Terminal,
    val progress: CoroutineProgressTaskAnimator<String>? = null,
) {
    suspend fun importDeckboxDecks(userName: String) =
        getListOfDeckLinks(userName).map { link ->
            importDeck(link)
        }

    suspend fun getListOfDeckLinks(userName: String): Flow<URL> = flow {
        val baseUrl = "https://deckbox.org"
        val userUrl = "$baseUrl/users/$userName"
        progress?.update { context = "fetching list of decks from $userUrl" }

        val doc = withContext(Dispatchers.IO) { Jsoup.connect(userUrl).get() }

        val result = doc.select("li.deck")

        progress?.update { total = result.size.toLong() }

        emitAll(doc.select("li.deck").asFlow().mapNotNull {
            "$baseUrl${it.selectFirst("a[href]")?.attr("href")}"
        }.map(URI::create).map(URI::toURL).onEach {
            progress?.advance(1)
        })
    }

    suspend fun importDeck(deckUrl: URL): DeckList {
        progress?.update { context = "fetching deck from $deckUrl" }
        val deckDoc = withContext(Dispatchers.IO) { Jsoup.connect(deckUrl.toString()).get() }

        val deckName = deckDoc.selectFirst("div.section_title")?.selectFirst("span")!!.text().replace(Regex("^\\d+\\s*"), "")
        progress?.update { context = "processing $deckName" }
        val format = when(deckDoc.selectFirst("div.deck_info_widget span.variant")?.text()) {
            "com" -> Format.Commander
            "sta" -> Format.Standard
            "pio" -> Format.Pioneer
            "his" -> Format.Historic
            "mod" -> Format.Modern
            "leg" -> Format.Legacy
            "vin" -> Format.Vintage
            "bra" -> Format.Brawl
            "pau" -> Format.Pauper
            "oat" -> Format.Oathbreaker
            "cub" -> Format.Cube
            else -> Format.Unknown
        }

        val commanders = sequenceOf(
            deckDoc.selectFirst("div#commander_info span:matches(Commander:) ~ div > a")?.text(),
            deckDoc.selectFirst("div#commander_info span:matches(Partner:) ~ div > a")?.text(),
        ).filterNotNull().associateWith { 1 }

        val mainboard = deckDoc.selectFirst("table.main")!!.select("tr[id]").map {
            val count = it.selectFirst("td.card_count")!!.text()
            val cardName = it.selectFirst("td.card_name")!!.text()
            cardName to count.toInt()
        }.filter { !commanders.contains(it.first) }.toMap()
        val sideboard = deckDoc.selectFirst("table.sideboard")!!.select("tr[id]").map {
            val count = it.selectFirst("td.card_count")!!.text()
            val cardName = it.selectFirst("td.card_name a")!!.text()
            cardName to count.toInt()
        }.toMap()

        return when(format) {
            Format.Commander, Format.Oathbreaker, Format.Brawl ->
                DeckList(
                    deckName,
                    format,
                    mainboard,
                    commandZone = commanders,
                    maybeboard = sideboard,
                )
            else ->
                DeckList(
                    deckName,
                    format,
                    mainboard,
                    sideboard = sideboard,
                )
        }
    }
}
