package at.woolph.caco.datamodel.sets

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import java.util.UUID

object ScryfallCardSets : IdTable<UUID>() {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val setCode = varchar("setCode", length = 10).index()
    val set = reference("set", CardSets).index()
    val name = varchar("name", length = 256).index()
}

class ScryfallCardSet(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ScryfallCardSet>(ScryfallCardSets)

    var setCode by ScryfallCardSets.setCode
    var name by ScryfallCardSets.name

    var set by CardSet referencedOn ScryfallCardSets.set

    val cards by Card referrersOn Cards.set
}