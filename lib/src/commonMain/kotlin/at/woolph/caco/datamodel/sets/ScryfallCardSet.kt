/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import at.woolph.utils.Uri
import at.woolph.utils.compareToNullable
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized

object ScryfallCardSets : IdTable<Uuid>() {
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

class ScryfallCardSet(id: EntityID<Uuid>) : UuidEntity(id), Comparable<ScryfallCardSet> {
  companion object : UuidEntityClass<ScryfallCardSet>(ScryfallCardSets) {
    fun findByCode(code: String?): ScryfallCardSet? =
        code?.let { find { ScryfallCardSets.code eq it }.firstOrNull() }

    fun findByParentSetCode(code: String?) =
        code?.let { find { ScryfallCardSets.parentSetCode eq it } } ?: emptySized()

    fun allRootSets() = all().filter(ScryfallCardSet::isRootSet)

    private fun compareSetCodeNullable(setCode: String?, otherSetcode: String?): Int? =
        setCode?.length?.compareToNullable(otherSetcode?.length)
            ?: setCode.compareToNullable(otherSetcode)

    val childSetsConsideredToBeRootSets = listOf("j25", "j22")

    val blockNameBlacklist =
        listOf(
            "Commander",
            "Core Set",
            "Heroes ofthe Realm",
            "Judge Gift Cards",
            "Friday Night Magic",
            "Magic Player Rewards",
            "Arena League",
        )

    private val FILTER_NO_PARENT = ScryfallCardSets.parentSetCode eq null

    fun allGroupedByBlocks() = groupedByBlocks(all())

    fun rootSetsGroupedByBlocks() = groupedByBlocks(find(FILTER_NO_PARENT))

    fun rootSetsGroupedByBlocks(op: () -> Op<Boolean>) =
        groupedByBlocks(find(FILTER_NO_PARENT.and(op)))

    private fun groupedByBlocks(scryfallCardSets: SizedIterable<ScryfallCardSet>): Sequence<Block> =
        sequence {
          val (nonBlockSets, blockSets) =
              scryfallCardSets
                  .orderBy(ScryfallCardSets.releaseDate to SortOrder.DESC)
                  .filter { !it.digitalOnly && it.cardCount > 12 }
                  .partition { it.blockName == null || blockNameBlacklist.contains(it.blockName) }

          nonBlockSets.map { SingleSetBlock(it) }.forEach { yield(it) }

          blockSets
              .groupBy { it.blockName!! }
              .map { (blockName, blockSets) ->
                if (blockSets.size == 1) {
                  SingleSetBlock(blockSets.first())
                } else {
                  MultiSetBlock(
                      blockSets.first().blockCode!!,
                      "$blockName Block",
                      blockSets.sortedDescending(),
                  )
                }
              }
              .forEach { yield(it) }
        }
  }

  val uuid: Uuid = id.value
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
  var icon by ScryfallCardSets.icon.transform({ it?.toString() }, { it?.let { Uri(it) } })

  val cardPrints by CardPrint referrersOn CardPrints.set

  val childSets: SizedIterable<ScryfallCardSet>
    get() = findByParentSetCode(code)

  val parentSet: ScryfallCardSet?
    get() = findByCode(parentSetCode)

  override fun toString() = "[$code] $name"

  val totalCardCount: Int
    get() = cardCount + childSets.sumOf(ScryfallCardSet::cardCount)

  val binderPages: Int
    get() = (cardCount / 18 + if (cardCount % 18 > 0) 1 else 0)

  val totalBinderPages: Int
    get() = binderPages + childSets.sumOf(ScryfallCardSet::binderPages)

  val selfAndNonRootChildSets: Sequence<ScryfallCardSet>
    get() = sequence {
      yield(this@ScryfallCardSet)
      yieldAll(
          childSets
              .filterNot(ScryfallCardSet::isRootSet)
              .flatMap(ScryfallCardSet::selfAndNonRootChildSets)
      )
    }

  val cardsOfSelfAndNonRootChildSets: Sequence<CardPrint>
    get() = selfAndNonRootChildSets.flatMap { it.cardPrints.asSequence() }

  val isRootSet: Boolean
    get() =
        parentSetCode == null ||
            (parentSet?.type != SetType.COMMANDER && type == SetType.COMMANDER) ||
            code in childSetsConsideredToBeRootSets

  override fun compareTo(other: ScryfallCardSet): Int {
    if (uuid == other.uuid) return 0
    return releaseDate.compareToNullable(other.releaseDate)?.let { -it }
        ?: compareSetCodeNullable(code, other.code)
        ?: 0
  }
}
