/* Copyright 2025 Wolfgang Mayer */
import at.woolph.caco.masterdata.import.ScryfallCard
import at.woolph.caco.masterdata.import.ScryfallSet
import at.woolph.caco.masterdata.import.jsonSerializer
import at.woolph.caco.masterdata.import.paginatedDataRequest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class ScryfallImportTests {
    @Test
    fun test() {
        val decodedScryfallSet = jsonSerializer.decodeFromString<ScryfallSet>("""
            {"object":"set","id":"4e47a6cd-cdeb-4b0f-8f24-cfe1a0127cb3","code":"dmu","mtgo_code":"dmu","arena_code":"dmu","tcgplayer_id":3102,"name":"Dominaria United","uri":"https://api.scryfall.com/sets/4e47a6cd-cdeb-4b0f-8f24-cfe1a0127cb3","scryfall_uri":"https://scryfall.com/sets/dmu","search_uri":"https://api.scryfall.com/cards/search?include_extras=true\u0026include_variations=true\u0026order=set\u0026q=e%3Admu\u0026unique=prints","released_at":"2022-09-09","set_type":"expansion","card_count":436,"digital":false,"nonfoil_only":false,"foil_only":false,"icon_svg_uri":"https://svgs.scryfall.io/sets/dmu.svg?1679889600"}
        """.trimIndent())

        decodedScryfallSet.objectType shouldBe "set"
        decodedScryfallSet.id shouldBe UUID.fromString("4e47a6cd-cdeb-4b0f-8f24-cfe1a0127cb3")
    }

    @Test
    fun cards() = runBlocking {
        val setCode = "stx"
        paginatedDataRequest<ScryfallCard>("https://api.scryfall.com/cards/search?q=set%3A${setCode}&unique=prints&order=set")
            .collect {
                println("${it.name}")
            }
    }

    @Test
    fun sets() = runBlocking {
        paginatedDataRequest<ScryfallSet>("https://api.scryfall.com/sets")
            .collect {
                println("${it.name}")
            }
    }
}
