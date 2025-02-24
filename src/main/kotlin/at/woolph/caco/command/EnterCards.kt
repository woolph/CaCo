package at.woolph.caco.command

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
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
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.prompt
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class EnterCards: CliktCommand() {
    val set by argument(help="The set code of the cards to be entered").convert {
      transaction {
        CardSet.Companion.findById(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
      }
    }
    val condition by option(help="The language of the cards")
        .convert { CardCondition.Companion.parse(it) }
        .default(CardCondition.NEAR_MINT)
        .validate { it != CardCondition.UNKNOWN }
    val language by option(help="The language of the cards")
        .convert { CardLanguage.Companion.parse(it) }
        .prompt("Select the language of the cards")
        .validate { it != CardLanguage.UNKNOWN }

    override fun run() {
      transaction {
        echo("enter cards for ${set.shortName} ${set.name}")
        echo("language = $language")
        echo("condition = $condition")

        File("./import-${set.shortName}.stdin").printWriter().use { stdinPrint ->
          data class PossessionUpdate(
            val count: Int = 1,
          ) {
            fun increment() = PossessionUpdate(count + 1)
          }

          val cardPossessionUpdates = mutableMapOf<Pair<Card, Boolean>, Int>()

          lateinit var prevSetNumber: String
          var setNumber = terminal.prompt("collector number")!!.also {
            stdinPrint.println(it)
          }
          while (setNumber.isNotBlank()) {
            fun add(setNumber: String) {
              val foil = setNumber.endsWith("*")
              val setNumber2 = setNumber.removeSuffix("*").toInt().toString().padStart(3, '0')
              val card = set.cards.firstOrNull { it.numberInSet == setNumber2 }
              if (card != null) {
                echo(
                  "add #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
                  trailingNewline = false
                )
                cardPossessionUpdates.compute(card to foil) { _, possessionUpdate ->
                  (possessionUpdate ?: 0.also {
                    terminal.danger(" \u001b[31mNeeded for collection!\u001b[0m")
                  }) + 1
                }
                echo()
              } else {
                terminal.danger("\u001b[31madd #${setNumber2} not found!\u001b[0m")
              }
            }

            fun remove(setNumber: String) {
              val foil = setNumber.endsWith("*")
              val setNumber2 = setNumber.removeSuffix("*").toInt().toString().padStart(3, '0')
              val card = set.cards.first { it.numberInSet == setNumber2 }
              echo(
                "removed #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}",
                trailingNewline = false
              )
              cardPossessionUpdates.computeIfPresent(card to foil) { _, possessionUpdate ->
                possessionUpdate - 1
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

          File("./import-${set.shortName}.csv").printWriter().use { out ->
            out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
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
              if (possessionUpdate > 0) {
                out.println("${possessionUpdate},0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,${if (foil) "foil" else ""},,,,,,,")
              }
            }
          }
        }
      }
    }
}