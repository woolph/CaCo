//package at.woolph.caco.collection
//
//import at.woolph.caco.cli.SuspendingTransactionCliktCommand
//import at.woolph.caco.datamodel.sets.ScryfallCardSet
//import at.woolph.caco.gui.view.collection.CardPossessionModel
//import at.woolph.caco.gui.view.collection.PaperCollectionView
//import at.woolph.caco.utils.Quadruple
//import com.github.ajalt.clikt.parameters.options.default
//import com.github.ajalt.clikt.parameters.options.option
//import com.github.ajalt.clikt.parameters.types.double
//import kotlin.math.min
//
//class HighValueTradables : SuspendingTransactionCliktCommand() {
//  val priceThreshold by option(help = "The price threshold above which cards are considered 'high value'").double()
//    .default(1.0)
//
//  override suspend fun runTransaction() {
//    ScryfallCardSet.all().map { set ->
//      val cardsSorted = set.cards.sortedBy { it.collectorNumber }
//        .map { CardPossessionModel(it, PaperCollectionView.Companion.COLLECTION_SETTINGS) }
//      set to cardsSorted.asSequence().flatMap { card ->
//        val suffixName = if (cardsSorted.asSequence().filter { it2 ->
//            it2.name.value == card.name.value && it2.extra.value == card.extra.value
//          }.count() > 1) {
//          val numberInSetWithSameName = cardsSorted.asSequence().filter { it2 ->
//            it2.name.value == card.name.value && it2.extra.value == card.extra.value && it2.collectorNumber.value < card.collectorNumber.value
//          }.count() + 1
//          " (V.$numberInSetWithSameName)"
//        } else {
//          ""
//        }
//        val suffixSet = when {
//          card.promo.value -> ": Promos"
//          card.extra.value -> ": Extras"
//          else -> ""
//        }
//
//        val excessNonPremium = card.possessionNonPremium.value - card.possessionNonPremiumTarget.value
//        val excessPremium =
//          card.possessionPremium.value - card.possessionPremiumTarget.value + min(0, excessNonPremium)
//
//        sequenceOf(
//          Quadruple(
//            excessNonPremium,
//            "${card.name.value}$suffixName",
//            card.set.value!!.name.let { "$it$suffixSet" },
//            card.price.value
//          ),
//          Quadruple(
//            excessPremium,
//            "${card.name.value}$suffixName(Foil)",
//            card.set.value!!.name.let { "$it$suffixSet" },
//            card.priceFoil.value
//          ),
//        )
//      }.filter { it.t1 > 0 && it.t4 >= priceThreshold }.joinToString("\n") { (excess, cardName, setName, price) ->
//        "$excess $cardName ($setName) $price"
//      }
//    }.forEach { (set, cards) ->
//      if (cards.isNotBlank()) {
//        echo("------------------------\n${set.name}\n$cards")
//        echo()
//      }
//    }
//  }
//}