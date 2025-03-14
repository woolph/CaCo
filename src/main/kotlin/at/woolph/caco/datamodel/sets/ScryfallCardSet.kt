package at.woolph.caco.datamodel.sets

import at.woolph.caco.utils.compareToNullable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.javatime.date
import java.net.URI
import java.util.*

object ScryfallCardSets : IdTable<UUID>() {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val code = varchar("setCode", length = 10).uniqueIndex()
    val type = enumeration<SetType>("type").index()
    val name = varchar("name", length = 256).index()
    val digitalOnly = bool("digitalOnly").index()

    val parentSetCode = varchar("parentSetCode", length = 10).index().nullable()

    val blockName = varchar("blockName", length = 256).nullable()
    val blockCode = varchar("blockCode", length = 10).index().nullable()

    val cardCount = integer("cardCount")
    val printedSize = integer("printedSize").nullable()
    val releaseDate = date("releaseDate").index()
    val icon = varchar("iconUri", length = 256).nullable()
}

class ScryfallCardSet(id: EntityID<UUID>) : UUIDEntity(id), Comparable<ScryfallCardSet> {
    companion object : UUIDEntityClass<ScryfallCardSet>(ScryfallCardSets) {
        fun findByCode(code: String?) = code?.let { find { ScryfallCardSets.code eq it }.firstOrNull() }
        fun findByParentSetCode(code: String?) = code?.let { find { ScryfallCardSets.parentSetCode eq it } } ?: emptySized()

        fun allRootSets() = all().filter(ScryfallCardSet::isRootSet)

        private fun compareSetCodeNullable(setCode: String?, otherSetcode: String?): Int? =
            setCode?.length?.compareToNullable(otherSetcode?.length) ?: setCode.compareToNullable(otherSetcode)
    }

    var code by ScryfallCardSets.code
    var parentSetCode by ScryfallCardSets.parentSetCode
    var name by ScryfallCardSets.name

    var type by ScryfallCardSets.type
    var digitalOnly  by ScryfallCardSets.digitalOnly
    var cardCount  by ScryfallCardSets.cardCount
    var printedSize  by ScryfallCardSets.printedSize
    var blockCode  by ScryfallCardSets.blockCode
    var blockName  by ScryfallCardSets.blockName

    var releaseDate by ScryfallCardSets.releaseDate
    var icon by ScryfallCardSets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })

    val cards by Card referrersOn Cards.set

    val childSets: SizedIterable<ScryfallCardSet> get() = findByParentSetCode(code)
    val selfAndNonRootChildSets: Sequence<ScryfallCardSet> get()  = sequence {
        yield(this@ScryfallCardSet)
        yieldAll(childSets.filterNot(ScryfallCardSet::isRootSet).flatMap(ScryfallCardSet::selfAndNonRootChildSets))
    }

    val cardsOfSelfAndNonRootChildSets = selfAndNonRootChildSets.flatMap { it.cards.asSequence() }

    val parentSet: ScryfallCardSet? get() = findByCode(parentSetCode)

    val isRootSet: Boolean get() = parentSetCode == null || (parentSet?.type != SetType.COMMANDER && type == SetType.COMMANDER)

    override fun compareTo(other: ScryfallCardSet): Int {
        if (id == other.id) return 0
        return releaseDate.compareToNullable(other.releaseDate)?.let { -it }
            ?: compareSetCodeNullable(code, other.code)
            ?: 0
    }

    override fun toString() = "[$code] $name"
}
