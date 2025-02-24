package at.woolph.caco

import at.woolph.caco.command.CaCoCli
import at.woolph.caco.command.EnterCards
import at.woolph.caco.command.HighValueTradables
import at.woolph.caco.command.ImportDeckboxCollection
import at.woolph.caco.command.ImportDecklists
import at.woolph.caco.command.ImportScryfall
import at.woolph.caco.command.ImportSet
import at.woolph.caco.command.PrintArchidektDecks
import at.woolph.caco.command.PrintCollectionBinderPageView
import at.woolph.caco.command.PrintDeckboxDecks
import at.woolph.caco.command.PrintDecklist
import at.woolph.caco.command.PrintInventory
import at.woolph.caco.command.PrintManaBase
import at.woolph.caco.command.PrintMissingStats
import at.woolph.caco.command.PrintPagePositions
import at.woolph.caco.command.Ui
import at.woolph.caco.command.UpdatesPrices
import at.woolph.caco.datamodel.Databases
import com.github.ajalt.clikt.core.*


fun main(args: Array<String>) {
    Databases.init()

    CaCoCli()
        .subcommands(
            Ui(),
            NoOpCliktCommand(name = "import").subcommands(
                ImportScryfall(),
                ImportDeckboxCollection(),
                UpdatesPrices(),
                ImportSet(),
                ImportDecklists(),
            ),
            NoOpCliktCommand(name = "print").subcommands(
                HighValueTradables(),
                PrintInventory(),
                PrintCollectionBinderPageView(),
                PrintPagePositions(),
                PrintDecklist(),
                PrintDeckboxDecks(),
                PrintArchidektDecks(),
                PrintMissingStats(),
                // TODO PrintMissingForDecks(),
                // TODO PrintMissingForCollection(),
                // TODO PrintDuplicatesRanked(), // by Price and or EDHREC Rank
            ),
            EnterCards(),
            PrintManaBase(),
        )
        .main(args)
}
