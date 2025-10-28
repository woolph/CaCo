package at.woolph.caco.datamodel.sets

import at.woolph.utils.Uri
import at.woolph.utils.compareToNullable
import java.time.LocalDate
import kotlin.uuid.Uuid

interface IScryfallCardSet: Comparable<IScryfallCardSet> {
  companion object {
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
  }

  val uuid: Uuid
  val code: String
  val parentSetCode: String?
  val name: String

  val type: SetType
  val digitalOnly: Boolean
  val cardCount: Int
  val printedSize: Int?
  val blockCode: String?
  val blockName: String?

  val releaseDate: LocalDate
  val icon: Uri?

  val cards: Iterable<ICard>

  val totalCardCount: Int
    get() = cardCount + childSets.sumOf(IScryfallCardSet::cardCount)

  val binderPages: Int
    get() = (cardCount / 18 + if (cardCount % 18 > 0) 1 else 0)

  val totalBinderPages: Int
    get() = binderPages + childSets.sumOf(IScryfallCardSet::binderPages)

  val childSets: Iterable<IScryfallCardSet>

  val selfAndNonRootChildSets: Sequence<IScryfallCardSet>
    get() = sequence {
      yield(this@IScryfallCardSet)
      yieldAll(
          childSets
              .filterNot(IScryfallCardSet::isRootSet)
              .flatMap(IScryfallCardSet::selfAndNonRootChildSets)
      )
    }

  val cardsOfSelfAndNonRootChildSets: Sequence<ICard>
    get() = selfAndNonRootChildSets.flatMap { it.cards.asSequence() }

  val parentSet: IScryfallCardSet?

  val isRootSet: Boolean
    get() =
        parentSetCode == null ||
            (parentSet?.type != SetType.COMMANDER && type == SetType.COMMANDER) ||
            code in childSetsConsideredToBeRootSets

  override fun compareTo(other: IScryfallCardSet): Int {
    if (uuid == other.uuid) return 0
    return releaseDate.compareToNullable(other.releaseDate)?.let { -it }
        ?: compareSetCodeNullable(code, other.code)
        ?: 0
  }
}
