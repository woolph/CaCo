package at.woolph.colly

import at.woolph.colly.Decks.nullable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.net.URI


enum class Rarity {
	OTHER, COMMON, UNCOMMON, RARE, MYTHIC;

	override fun toString() = when(this) {
		COMMON -> "C"
		UNCOMMON -> "U"
		RARE -> "R"
		MYTHIC -> "M"
		else -> "?"
	}
}

fun String.parseRarity() = when(this) {
	"common" -> Rarity.COMMON
	"uncommon" -> Rarity.UNCOMMON
	"rare" -> Rarity.RARE
	"mythic" -> Rarity.MYTHIC
	else -> Rarity.OTHER
}

object CardPossessions : IntIdTable() {
	val card = reference("card", Cards).index()
	val language = varchar("language", length = 2).index()
	val condition = integer("condition").default(1).index()
	val foil = bool("foil").default(false).index()
	val prereleasePromo = bool("prereleasePromo").default(false).index()
}

class CardPossession(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<CardPossession>(CardPossessions)

	var card by Card referencedOn CardPossessions.card
	var language by CardPossessions.language
	var condition by CardPossessions.condition
	var foil by CardPossessions.foil
	var prereleasePromo by CardPossessions.prereleasePromo
}

object Cards : IntIdTable() {
	val set = reference("set", Sets).index()
	val numberInSet = integer("number").index()
	val name = varchar("name", length = 256).index()
	val rarity = enumeration("rarity", Rarity::class).index()
	val promo = bool("promo").default(false).index()
	val image = varchar("imageURI", length = 256).nullable()
	val cardmarketUri = varchar("cardmarketUri", length = 256).nullable()
}

class Card(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<Card>(Cards)

	var set by Set referencedOn Cards.set
	var numberInSet by Cards.numberInSet
	var name by Cards.name
	var rarity by Cards.rarity
	var promo by Cards.promo
	var image by Cards.image.transform({ it?.toString() }, { it?.let { URI(it) } })
	var cardmarketUri by Cards.cardmarketUri.transform({ it?.toString() }, { it?.let { URI(it) } })

	val possessions by CardPossession referrersOn CardPossessions.card
}

object Sets : IntIdTable() {
	val shortName = varchar("shortName", length = 3).index()
	val name = varchar("name", length = 256).index()
	val dateOfRelease = date("dateOfRelease").index()
	val officalCardCount = integer("officalCardCount").default(0)
	val icon = varchar("iconURI", length = 256).nullable()
}

class Set(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<Set>(Sets)

	var shortName by Sets.shortName
	var name by Sets.name
	var dateOfRelease by Sets.dateOfRelease
	var officalCardCount by Sets.officalCardCount
	var icon by Sets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })

	val cards by Card referrersOn Cards.set
}

enum class Place {
	Mainboard, Sideboard, Maybeboard
}

object DeckCards : IntIdTable() {
	val deck = reference("deck", Decks).index()
	val name = varchar("name", length = 256).index()
	val place = enumeration("place", Place::class).index()
	val count = integer("count").index()
	val comment = text("comment").nullable()
}

class DeckCard(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<DeckCard>(DeckCards)

	var deck by Deck referencedOn DeckCards.deck
	var name by DeckCards.name
	var place by DeckCards.place
	var count by DeckCards.count
	var comment by DeckCards.comment
}

enum class Format {
	Unknown, Standard, StandardPlus, Commander, Modern, Pauper, Legacy, Vintage
}

object Decks : IntIdTable() {
	val name = varchar("name", length = 256).index()
	val version = varchar("version", length = 64).index().default("")
	val format = enumeration("format", Format::class).index()
	val priority = integer("priority").index()
	val link = varchar("link", length = 256).nullable()
	val comment = text("comment").nullable()
}

class Deck(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<Deck>(Decks)

	var name by Decks.name
	var version by Decks.version
	var format by Decks.format
	var priority by Decks.priority
	var link by Decks.link.transform({ it?.toString() }, { it?.let { URI(it) } })
	var comment by Decks.comment

	val cards by DeckCard referrersOn DeckCards.deck
}
