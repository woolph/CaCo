/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import at.woolph.caco.datamodel.Color
import at.woolph.caco.datamodel.ColorIdentity
import at.woolph.caco.datamodel.decks.Format
import at.woolph.utils.currency.CurrencyValue
import at.woolph.utils.ktor.jsonSerializer
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.json.json
import kotlin.uuid.Uuid

object Cards : IdTable<Uuid>() {
  override val id = uuid("oracleId").entityId()
  override val primaryKey = PrimaryKey(id)

  val name = varchar("name", length = 256).index() // name is not unique for tokens
  val layout = enumeration<LayoutType>("layout")
  val nameDE = varchar("nameDE", length = 256).index().nullable()
  val token = bool("token").default(false).index()

  val specialDeckRestrictions = integer("specialDeckRestrictions").nullable()

  val manaCost = varchar("manaCost", length = 256).nullable()
  val manaValue = float("manaValue").index()
  val oracleText = varchar("oracleText", length = 4096)
  val type = varchar("type", length = 256).nullable()
  val colorIdentity = integer("colorIdentity")
  val producedMana = integer("producedMana").nullable()

  val gameChanger = bool("gameChanger").index()
  val edhrecRank = integer("edhrecRank").nullable()

  val legalities = json<Map<Format, Legality>>("legality", jsonSerializer).nullable()
}

class Card(id: EntityID<Uuid>) : UuidEntity(id), Comparable<Card> {
  companion object : UuidEntityClass<Card>(Cards) {
    val CARD_DRAW_PATTERN = Regex("draws? (|a |two |three )cards?", RegexOption.IGNORE_CASE)
  }

  val lowestPrice: CurrencyValue? get() = prints.mapNotNull { it.lowestPrice }.minOrNull()
  val prints by CardPrint referrersOn CardPrints

  var name by Cards.name
  var nameDE by Cards.nameDE
  var token by Cards.token
  var layout by Cards.layout

  var specialDeckRestrictions by Cards.specialDeckRestrictions

  var manaCost by Cards.manaCost
  var manaValue by Cards.manaValue
  var type by Cards.type
  var oracleText by Cards.oracleText
  var legalities by Cards.legalities

  fun isLegalIn(format: Format): Boolean =
      legalities?.get(format)?.isAllowedToBePlayed == true

  var gameChanger by Cards.gameChanger
  var edhrecRank by Cards.edhrecRank

  var colorIdentity by
      Cards.colorIdentity.transform(ColorIdentity::encodeAsInteger, ColorIdentity::decodeFromInteger)

  var producedMana: Color? by
    Cards.producedMana.transform(
      { it?.encodeAsInteger() },
      { it?.let(Color::decodeFromInteger) },
    )

  val isCreature: Boolean
    get() = type?.contains("Creature") == true

  val isLand: Boolean
    get() = type?.contains("Land") == true

  val isCheapNonland: Boolean
    get() = manaValue <= 2.0 && !isLand

  val isCheapCardDraw: Boolean by lazy {
    (isCheapNonland &&
        (oracleText.contains(CARD_DRAW_PATTERN) &&
            oracleTextNone("{4}", "blood token", "investigate") &&
            ((!isCreature || oracleTextAll("when", "enters")))) ||
        (!isCreature &&
            !oracleText.contains(Regex("pays?", RegexOption.IGNORE_CASE)) &&
            oracleTextAll("look", "library", "put", "your hand"))) ||
        !isLand &&
            oracleText.contains(
                Regex("cycling( \\{([012])}|—pay \\d+ life)", RegexOption.IGNORE_CASE)
            )
  }

  val blacklistCheapRamp =
      listOf(
          "Dreamscape Artist",
          "Crop Rotation",
          "Ordeal of Nylea",
          "Khalni Heart Expedition",
          "Oashra Cultivator",
          "Elvish Reclaimer",
      )
  val isCheapRamp: Boolean by lazy {
    isCheapNonland &&
        !isCheapCardDraw &&
        (oracleText.contains(Regex("\\badd\\b", RegexOption.IGNORE_CASE)) &&
            oracleTextNone("add its ability", "add a lore counter", "dies") ||
            oracleTextAll("search", "your library", "put", "onto the battlefield") &&
                oracleTextAny("land", "basic") &&
                name !in blacklistCheapRamp ||
            oracleTextAll("enchanted land is tapped", "adds an additional") ||
            oracleTextAll("put a creature card with", "from your hand onto the battlefield"))
  }

  val isMDFCLand: Boolean
    get() = (type?.contains("// Land") == true) xor (type?.contains("Land // ") == true)

  val isMDFCLandTapped: Boolean by lazy {
    isMDFCLand && oracleTextAll("enters", "tapped") && oracleTextNone("you may pay", "unless")
  }
  val isMDFCLandUntapped: Boolean by lazy {
    isMDFCLand && (!oracleTextAll("enters", "tapped") || oracleTextAny("you may pay", "unless"))
  }

  private fun oracleTextAll(vararg keywords: String) =
      keywords.all { oracleText.contains(it, ignoreCase = true) }

  private fun oracleTextAny(vararg keywords: String) =
      keywords.any { oracleText.contains(it, ignoreCase = true) }

  private fun oracleTextNone(vararg keywords: String) =
      keywords.none { oracleText.contains(it, ignoreCase = true) }

  override fun compareTo(other: Card): Int =
    name.compareTo(other.name)

  override fun toString(): String = name
}
