package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.net.URI

object Variants : IntIdTable() {
    val deck = reference("deck", Decks).index()

    val parentVariant = reference("parentVariant", Variants).nullable()

    val originator = varchar("originator", length = 256).index().default("")
	val priority = enumeration("priority", Priority::class).index()
    val link = varchar("link", length = 256).nullable()
    val comment = text("comment").nullable()

    val archived = bool("archived").default(false)
}

class Variant(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Variant>(Variants)

    var deck by Deck referencedOn Variants.deck
    var parentVariant by Variant optionalReferencedOn Variants.parentVariant

    var originator by Variants.originator
    var priority by Variants.priority
    var link by Variants.link.transform({ it?.toString() }, { it?.let { URI(it) } })
    var comment by Variants.comment

    var archived by Variants.archived

    val builds by Build referrersOn Builds.variant
}
