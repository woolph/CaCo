/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.cli.SuspendingTransactionCliktCommand
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.prompt
import org.jetbrains.exposed.sql.and
import java.io.File
import kotlin.io.path.Path

class EnterCards : SuspendingTransactionCliktCommand() {
  val format by option(help = "The format to export the entered cards to")
    .enum<CollectionFileFormat>()
    .default(CollectionFileFormat.ARCHIDEKT)

  val condition by option(help = "The language of the cards")
    .convert { CardCondition.Companion.parse(it) }
    .default(CardCondition.NEAR_MINT)
    .validate { it != CardCondition.UNKNOWN }

  val language by option(help = "The language of the cards")
    .convert { CardLanguage.Companion.parse(it) }
    .prompt("Select the language of the cards")
    .validate { it != CardLanguage.UNKNOWN }

  val languageRankList by option(help = "The language rank list defines which language print you prefer for your collection.",
    valueSourceKey = "languageRankList")
    .convert { it.split(",").map(CardLanguage.Companion::parse) }
    .default(listOf(CardLanguage.ENGLISH))

  override suspend fun runTransaction() {
    /** this is a list of languages to be looked for while checking, if I need the entered card for collection or not
     * (picture this: I'm entering cards for a set in German, but I'm preferring collecting English cards, so when I have the card in English already, I don't need the German one;
     * on the other hand, if I don't have the card in English, I need the German one; now let's say I'm entering cards in English, then I don't need to check for my German stock,
     * because I want to replace them anyway if this is the only printing I have)
     */
    val languagesToBeChecked =
      languageRankList.takeWhile { it != language }.toMutableList().apply { add(language) }

    echo("language: $language (languages to be considered when checking whether card is needed for collection: $languagesToBeChecked)")
    echo("condition: $condition")

    File("./import.stdin").printWriter().use { stdinPrint ->
      data class PossessionUpdate2(
        val count: Int = 0,
        val alreadyCollected: Int,
      ) {
        fun increment() = PossessionUpdate2(count + 1, alreadyCollected)
        fun decrement() = if (count > 0) PossessionUpdate2(count - 1, alreadyCollected) else this
        fun isNeeded() = (count + alreadyCollected) == 0
      }

      fun newPossessionUpdate2(card: Card, finish: Finish) =
        PossessionUpdate2(
          0,
          CardPossession.Companion.find { CardPossessions.card.eq(card.id) and CardPossessions.finish.eq(finish) }
            .count { it.language in languagesToBeChecked && it.condition.isBetterThanOrEqual(condition) }.toInt()
        )

      val cardPossessionUpdates = mutableMapOf<Pair<Card, Finish>, PossessionUpdate2>()

      lateinit var set: ScryfallCardSet
      lateinit var prevSetNumber: String
      var setCodeNumber = terminal.prompt("collector number (optional with setCode)")!!.also {
        stdinPrint.println(it)
      }

      while (setCodeNumber.isNotBlank()) {
        fun add(setNumber: String) {
          val finish = when {
            setNumber.endsWith("#") -> Finish.Etched
            setNumber.endsWith("*") -> Finish.Foil
            else -> Finish.Normal
          }
          val setNumber2 = setNumber.removeSuffix("*").removeSuffix("#")
          val card = set.cards.firstOrNull { it.collectorNumber == setNumber2 }
          if (card != null) {
            echo(
              "add ${set.code.uppercase()} #${card.collectorNumber} \"${card.name}\" ${if (finish != Finish.Normal) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
              trailingNewline = false
            )
            cardPossessionUpdates.compute(card to finish) { _, possessionUpdate ->
              ((possessionUpdate ?: newPossessionUpdate2(card, finish)).also {
                if (it.isNeeded()) {
                  terminal.danger(" \u001b[31mNeeded for collection!\u001b[0m")
                }
              }).increment()
            }
          } else {
            terminal.danger("\u001b[31madd #${setNumber2} not found!\u001b[0m")
          }
        }

        fun remove(setNumber: String) {
          val finish = when {
            setNumber.endsWith("#") -> Finish.Etched
            setNumber.endsWith("*") -> Finish.Foil
            else -> Finish.Normal
          }
          val setNumber2 = setNumber.removeSuffix("*").removeSuffix("#")
          val card = set.cards.first { it.collectorNumber == setNumber2 }
          echo(
            "removed #${card.collectorNumber} \"${card.name}\" ${if (finish != Finish.Normal) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
            trailingNewline = false
          )
          cardPossessionUpdates.computeIfPresent(card to finish) { _, possessionUpdate ->
            possessionUpdate.decrement()
          }
          echo()
        }

        try {
          val tokens = setCodeNumber.split("#", limit = 2)
          val (setCode, setNumber) = if (tokens.size == 2) {
            (ScryfallCardSet.findByCode(tokens[0].lowercase()) ?: throw IllegalArgumentException("unknown set ${tokens[0]}")) to tokens[1]
          } else {
            set to setCodeNumber
          }
          set = setCode

          prevSetNumber = when (setNumber) {
            "+" -> {
              add(prevSetNumber); prevSetNumber
            }

            "-" -> {
              remove(prevSetNumber); prevSetNumber
            }

            else -> {
              add(setNumber); setNumber
            }
          }
        } catch (e: Exception) {
          terminal.danger("\u001b[31m${e.message}!\u001b[0m")
        }
        setCodeNumber = terminal.prompt("collector number")!!.also {
          stdinPrint.println(it)
        }
      }

      // TODO use CardCollectionItem to begin with for entering that stuff
      val cardCollectionItems = cardPossessionUpdates.map { (x, possessionUpdate) ->
        val (cardInfo, finish) = x
        CardCollectionItem(
          possessionUpdate.count.toUInt(),
          CardCollectionItemId(
            cardInfo,
            finish = finish,
            language = language,
            condition = condition,
          ),
        )
      }
      val file = Path("./src/test/http-requests/import.csv")
      when (format) {
        CollectionFileFormat.DECKBOX -> cardCollectionItems.exportDeckbox(file)
        CollectionFileFormat.ARCHIDEKT -> cardCollectionItems.exportArchidekt(file)
      }

      cardCollectionItems.forEach(CardCollectionItem::addToCollection)
    }
  }
}
