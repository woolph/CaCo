package at.woolph.caco.datamodel.decks

import at.woolph.caco.datamodel.sets.CardSets
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Builds : IntIdTable() {
    val variant = reference("variant", Variants).index()

    val version = varchar("version", length = 64).index().default("")
    val dateOfCreation = date("dateOfCreation").index()
    val dateOfLastModification = date("dateOfLastModification").index()
	val latestSetConsidered = reference("latestSetConsidered", CardSets).index().nullable()
    val comment = text("comment").nullable()
}

class Build(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Build>(Builds)

    var variant by Variant referencedOn Builds.variant

    var version by Builds.version
    var dateOfCreation by Builds.dateOfCreation
    var dateOfLastModification by Builds.dateOfLastModification
    var latestSetConsidered by Builds.latestSetConsidered
    var comment by Builds.comment

    val cards by DeckCard referrersOn DeckCards.build

    val mainboard get() = cards.filter { it.place == Place.Mainboard }
    val sideboard get() = cards.filter { it.place == Place.Sideboard }
    val maybeboard get() = cards.filter { it.place == Place.Maybeboard }

	// TODO state isReady (indicates that all cards needed are in the collection) => probably differentiate between paper and arena?!
}
