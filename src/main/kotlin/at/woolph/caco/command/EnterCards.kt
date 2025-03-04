package at.woolph.caco.command

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.exporter.collection.ArchidektCollectionExportItem
import at.woolph.caco.exporter.collection.exportArchidekt
import at.woolph.caco.importer.collection.setNameMapping
import at.woolph.caco.importer.collection.toDeckboxCondition
import at.woolph.caco.importer.collection.toLanguageDeckbox
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.prompt
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDate
import kotlin.collections.map
import kotlin.io.path.Path
import kotlin.io.path.writer
import kotlin.toUInt

enum class ExportFormat {
  DECKBOX,
  ARCHIDEKT,
}

class EnterCards: CliktCommand() {
  val set by argument(help="The set code of the cards to be entered").convert {
    transaction {
      CardSet.Companion.findById(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
    }
  }
  val export by option(help="The format to export the entered cards to")
    .enum<ExportFormat>()
    .default(ExportFormat.ARCHIDEKT)

  val condition by option(help="The language of the cards")
      .convert { CardCondition.Companion.parse(it) }
      .default(CardCondition.NEAR_MINT)
      .validate { it != CardCondition.UNKNOWN }
  val language by option(help="The language of the cards")
      .convert { CardLanguage.Companion.parse(it) }
      .prompt("Select the language of the cards")
      .validate { it != CardLanguage.UNKNOWN }

  /** TODO make it configurable */
  val languageRankingList = listOf(
    CardLanguage.ENGLISH,
    CardLanguage.GERMAN,
  )

  override fun run() {
    transaction {
      /** this is a list of languages to be looked for while checking, if I need the entered card for collection or not
       * (picture this: I'm entering cards for a set in German, but I'm preferring collecting English cards, so when I have the card in English already, I don't need the German one;
       * on the other hand, if I don't have the card in English, I need the German one; now let's say I'm entering cards in English, then I don't need to check for my German stock,
       * because I want to replace them anyway if this is the only printing I have)
       */
      val languagesToBeChecked = languageRankingList.takeWhile { it != language }.toMutableList().apply { add(language) }

      echo("enter cards for ${set.shortName} ${set.name}")
      echo("language = $language")
      echo("condition = $condition")

      File("./import-${set.shortName}.stdin").printWriter().use { stdinPrint ->
        data class PossessionUpdate(
          val count: Int = 1,
        ) {
          fun increment() = PossessionUpdate(count + 1)
        }

        data class PossessionUpdate2(
          val count: Int = 0,
          val alreadyCollected: Int,
        ) {
          fun increment() = PossessionUpdate2(count + 1, alreadyCollected)
          fun decrement() = if (count > 0) PossessionUpdate2(count - 1, alreadyCollected) else this
          fun isNeeded() = (count + alreadyCollected) == 0


        }
        fun newPossessionUpdate2(card: Card, foil: Boolean) =
          PossessionUpdate2(0, CardPossession.find { CardPossessions.card.eq(card.id) and CardPossessions.foil.eq(foil) }.count { it.language in languagesToBeChecked }.toInt())

        val cardPossessionUpdates = mutableMapOf<Pair<Card, Boolean>, PossessionUpdate2>()

        lateinit var prevSetNumber: String
        var setNumber = terminal.prompt("collector number")!!.also {
          stdinPrint.println(it)
        }

        while (setNumber.isNotBlank()) {
          fun add(setNumber: String) {
            val foil = setNumber.endsWith("*")
            val setNumber2 = setNumber.removeSuffix("*").toInt().toString()
            val card = set.cards.firstOrNull { it.numberInSet == setNumber2 }
            if (card != null) {
              echo(
                "add #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
                trailingNewline = false
              )
              cardPossessionUpdates.compute(card to foil) { _, possessionUpdate ->
                ((possessionUpdate ?: newPossessionUpdate2(card, foil)).also {
                  if (it.isNeeded()) {
                    terminal.danger(" \u001b[31mNeeded for collection!\u001b[0m")
                  }
                }).increment()
              }
              echo()
            } else {
              terminal.danger("\u001b[31madd #${setNumber2} not found!\u001b[0m")
            }
          }

          fun remove(setNumber: String) {
            val foil = setNumber.endsWith("*")
            val setNumber2 = setNumber.removeSuffix("*").toInt().toString()
            val card = set.cards.first { it.numberInSet == setNumber2 }
            echo(
              "removed #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
              trailingNewline = false
            )
            cardPossessionUpdates.computeIfPresent(card to foil) { _, possessionUpdate ->
              possessionUpdate.decrement()
            }
            echo()
          }
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
          setNumber = terminal.prompt("collector number")!!.also {
            stdinPrint.println(it)
          }
        }

        val file = Path("./import-${set.shortName}.csv")
          when (export) {
            ExportFormat.DECKBOX ->
              file.writer().use { out ->
                out.write("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price\n")
                cardPossessionUpdates.forEach { (x, possessionUpdate) ->
                  val (cardInfo, foil) = x
                  val cardName = cardInfo.name
                  val cardNumberInSet = cardInfo.numberInSet
                  val token = cardInfo.token
                  val promo = cardInfo.promo
                  val condition = CardCondition.NEAR_MINT.toDeckboxCondition()
                  val prereleasePromo = false
                  val language = language.toLanguageDeckbox()
                  val setName = setNameMapping.asSequence().firstOrNull { it.value == set.name }?.key ?: set.name.let {
                    when {
                      prereleasePromo -> "Prerelease Events: ${it}"
                      token -> "Extras: ${it}"
                      else -> it
                    }
                  }
                  if (possessionUpdate.count > 0) {
                    out.write("${possessionUpdate.count},0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,${if (foil) "foil" else ""},,,,,,,\n")
                  }
                }
              }

            ExportFormat.ARCHIDEKT -> cardPossessionUpdates.map { (x, possessionUpdate) ->
              val (cardInfo, foil) = x
                ArchidektCollectionExportItem(
                  possessionUpdate.count.toUInt(),
                  foil,
                  condition,
                  language,
                  cardInfo.scryfallId,
                  LocalDate.now(),
                )
              }.exportArchidekt(file)
          }
      }
    }
  }
}
