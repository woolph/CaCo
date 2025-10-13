package at.woolph.caco.datamodel.sets

import at.woolph.caco.utils.compareToNullable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import java.net.URI
import java.util.*
import kotlin.collections.List
import kotlin.collections.asSequence
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.filter
import kotlin.collections.filterNot
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.groupBy
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.partition
import kotlin.collections.sortedDescending
import kotlin.collections.sumOf
import kotlin.sequences.Sequence
import kotlin.sequences.flatMap
import kotlin.sequences.sequence

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

sealed interface Block {
  val blockCode: String
  val blockName: String
}

data class SingleSetBlock(val set: ScryfallCardSet) : Block {
  override val blockCode: String get() = set.code
  override val blockName: String get() = set.name
}

data class MultiSetBlock(
  override val blockCode: String,
  override val blockName: String,
  val sets: List<ScryfallCardSet>,
) : Block

class ScryfallCardSet(id: EntityID<UUID>) : UUIDEntity(id), Comparable<ScryfallCardSet> {

  companion object : UUIDEntityClass<ScryfallCardSet>(ScryfallCardSets) {
    fun findByCode(code: String?) = code?.let { find { ScryfallCardSets.code eq it }.firstOrNull() }
    fun findByParentSetCode(code: String?) = code?.let { find { ScryfallCardSets.parentSetCode eq it } } ?: emptySized()

    fun allRootSets() = all().filter(ScryfallCardSet::isRootSet)

    private fun compareSetCodeNullable(setCode: String?, otherSetcode: String?): Int? =
      setCode?.length?.compareToNullable(otherSetcode?.length) ?: setCode.compareToNullable(otherSetcode)

    val childSetsConsideredToBeRootSets = listOf("j25", "j22")

    val blockNameBlacklist = listOf(
      "Commander",
      "Core Set",
      "Heroes ofthe Realm",
      "Judge Gift Cards",
      "Friday Night Magic",
      "Magic Player Rewards",
      "Arena League",
    )


    private val FILTER_NO_PARENT = Op.build { ScryfallCardSets.parentSetCode eq null }

    fun allGroupedByBlocks() = groupedByBlocks(all())
    fun rootSetsGroupedByBlocks() = groupedByBlocks(find(FILTER_NO_PARENT))
    fun rootSetsGroupedByBlocks(op: SqlExpressionBuilder.() -> Op<Boolean>) =
      groupedByBlocks(find(FILTER_NO_PARENT.and(op)))

    private fun groupedByBlocks(scryfallCardSets: SizedIterable<ScryfallCardSet>): Sequence<Block> = sequence {
      val (nonBlockSets, blockSets) = scryfallCardSets
        .orderBy(ScryfallCardSets.releaseDate to SortOrder.DESC)
        .filter { !it.digitalOnly && it.cardCount > 12 }
        .partition { it.blockName == null || it.blockName in blockNameBlacklist }

      nonBlockSets.map { SingleSetBlock(it) }.forEach { yield(it) }

      blockSets.groupBy { it.blockName!! }.map { (blockName, blockSets) ->
        if (blockSets.size == 1) {
          SingleSetBlock(blockSets.first())
        } else {
          MultiSetBlock(blockSets.first().blockCode!!, "$blockName Block", blockSets.sortedDescending())
        }
      }.forEach { yield(it) }
    }
  }

  var code by ScryfallCardSets.code
  var parentSetCode by ScryfallCardSets.parentSetCode
  var name by ScryfallCardSets.name

  var type by ScryfallCardSets.type
  var digitalOnly by ScryfallCardSets.digitalOnly
  var cardCount by ScryfallCardSets.cardCount
  var printedSize by ScryfallCardSets.printedSize
  var blockCode by ScryfallCardSets.blockCode
  var blockName by ScryfallCardSets.blockName

  var releaseDate by ScryfallCardSets.releaseDate
  var icon by ScryfallCardSets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })

  val cards by Card referrersOn Cards.set

  val totalCardCount: Int get() = cardCount + childSets.sumOf(ScryfallCardSet::cardCount)

  val binderPages: Int get() = (cardCount / 18 + if (cardCount % 18 > 0) 1 else 0)
  val totalBinderPages: Int get() = binderPages + childSets.sumOf(ScryfallCardSet::binderPages)

  val childSets: SizedIterable<ScryfallCardSet> get() = findByParentSetCode(code)
  val selfAndNonRootChildSets: Sequence<ScryfallCardSet>
    get() = sequence {
      yield(this@ScryfallCardSet)
      yieldAll(childSets.filterNot(ScryfallCardSet::isRootSet).flatMap(ScryfallCardSet::selfAndNonRootChildSets))
    }

  val cardsOfSelfAndNonRootChildSets = selfAndNonRootChildSets.flatMap { it.cards.asSequence() }

  val parentSet: ScryfallCardSet? get() = findByCode(parentSetCode)

  val isRootSet: Boolean
    get() = parentSetCode == null
        || (parentSet?.type != SetType.COMMANDER && type == SetType.COMMANDER)
        || code in childSetsConsideredToBeRootSets

  override fun compareTo(other: ScryfallCardSet): Int {
    if (id == other.id) return 0
    return releaseDate.compareToNullable(other.releaseDate)?.let { -it }
      ?: compareSetCodeNullable(code, other.code)
      ?: 0
  }

  override fun toString() = "[$code] $name"
}
