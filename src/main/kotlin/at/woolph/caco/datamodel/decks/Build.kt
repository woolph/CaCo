package at.woolph.caco.datamodel.decks

import at.woolph.caco.datamodel.sets.CardSets
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date

object Builds : IntIdTable() {
    val archetype = reference("archetype", DeckArchetypes).index()

	val parentBuild = reference("parentBuild", Builds).nullable()

    val version = varchar("version", length = 64).index().default("")
    val dateOfCreation = date("dateOfCreation").index()
    val dateOfLastModification = date("dateOfLastModification").index()
	val latestSetConsidered = reference("latestSetConsidered", CardSets).index().nullable()
    val comment = text("comment").nullable()

    val currentlyBuilt = bool("currentlyBuilt").default(false)
	val archived = bool("archived").default(false)
}

class Build(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Build>(Builds)

    var archetype by DeckArchetype referencedOn Builds.archetype
	var parentBuild by Build optionalReferencedOn Builds.parentBuild

    var version by Builds.version
    var dateOfCreation by Builds.dateOfCreation
    var dateOfLastModification by Builds.dateOfLastModification
    var latestSetConsidered by Builds.latestSetConsidered
    var comment by Builds.comment

	val currentlyBuilt by Builds.currentlyBuilt
	val archived by Builds.archived

    val cards by DeckCard referrersOn DeckCards.build

    val mainboard get() = cards.filter { it.place == Place.Mainboard }
    val sideboard get() = cards.filter { it.place == Place.Sideboard }
    val maybeboard get() = cards.filter { it.place == Place.Maybeboard }

    // TODO implement feature to show diff between two builds (to be able to compare)
	// TODO state isReady (indicates that all cards needed are in the collection) => probably differentiate between paper and arena?!
}
