/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import at.woolph.utils.Uri
import at.woolph.utils.exposed.UuidEntity
import at.woolph.utils.exposed.UuidEntityClass
import at.woolph.utils.exposed.ktUuid
import at.woolph.utils.compareToNullable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.javatime.date
import kotlin.uuid.Uuid

object ScryfallCardSets : IdTable<Uuid>() {
  override val id = ktUuid("id").entityId()
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

class ScryfallCardSet(id: EntityID<Uuid>) : IScryfallCardSet, UuidEntity(id) {
  companion object : UuidEntityClass<ScryfallCardSet>(ScryfallCardSets) {
    fun findByCode(code: String?) = code?.let { find { ScryfallCardSets.code eq it }.firstOrNull() }

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

    private val FILTER_NO_PARENT = Op.build { ScryfallCardSets.parentSetCode eq null }

    fun allGroupedByBlocks() = groupedByBlocks(all())

    fun rootSetsGroupedByBlocks() = groupedByBlocks(find(FILTER_NO_PARENT))

    fun rootSetsGroupedByBlocks(op: SqlExpressionBuilder.() -> Op<Boolean>) =
        groupedByBlocks(find(FILTER_NO_PARENT.and(op)))

    private fun groupedByBlocks(scryfallCardSets: SizedIterable<ScryfallCardSet>): Sequence<Block> =
        sequence {
          val (nonBlockSets, blockSets) =
              scryfallCardSets
                  .orderBy(ScryfallCardSets.releaseDate to SortOrder.DESC)
                  .filter { !it.digitalOnly && it.cardCount > 12 }
                  .partition { it.blockName == null || it.blockName in blockNameBlacklist }

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

  override val uuid: Uuid = id.value
  override var code by ScryfallCardSets.code
  override var parentSetCode by ScryfallCardSets.parentSetCode
  override var name by ScryfallCardSets.name

  override var type by ScryfallCardSets.type
  override var digitalOnly by ScryfallCardSets.digitalOnly
  override var cardCount by ScryfallCardSets.cardCount
  override var printedSize by ScryfallCardSets.printedSize
  override var blockCode by ScryfallCardSets.blockCode
  override var blockName by ScryfallCardSets.blockName

  override var releaseDate by ScryfallCardSets.releaseDate
  override var icon by ScryfallCardSets.icon.transform({ it?.toString() }, { it?.let { Uri(it) } })

  override val cards by Card referrersOn Cards.set

  override val childSets: SizedIterable<ScryfallCardSet>
    get() = findByParentSetCode(code)

  override val parentSet: ScryfallCardSet?
    get() = findByCode(parentSetCode)

  override fun toString() = "[$code] $name"
}
