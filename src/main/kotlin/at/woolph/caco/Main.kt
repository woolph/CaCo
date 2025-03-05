package at.woolph.caco

import at.woolph.caco.collection.EnterCards
import at.woolph.caco.collection.CollectionExportCommand
import at.woolph.caco.collection.HighValueTradables
import at.woolph.caco.collection.CollectionImportCommand
import at.woolph.caco.command.ImportDecklists
import at.woolph.caco.masterdata.MasterdataImportCommand
import at.woolph.caco.masterdata.ImportSet
import at.woolph.caco.command.PrintArchidektDecks
import at.woolph.caco.command.PrintCollectionBinderPageView
import at.woolph.caco.command.PrintDeckboxDecks
import at.woolph.caco.command.PrintDecklist
import at.woolph.caco.collection.PrintInventory
import at.woolph.caco.command.PrintManaBase
import at.woolph.caco.collection.PrintMissingCommander
import at.woolph.caco.collection.PrintMissingStats
import at.woolph.caco.collection.PrintPagePositions
import at.woolph.caco.gui.Ui
import at.woolph.caco.masterdata.UpdatesPrices
import at.woolph.caco.datamodel.Databases
import com.github.ajalt.clikt.core.*


fun main(args: Array<String>) {
    Databases.init()

    (object: NoOpCliktCommand(name="caco"){})
        .subcommands(
            Ui(),
            NoOpCliktCommand(name = "collection").subcommands(
                CollectionExportCommand(),
                CollectionImportCommand(),
                HighValueTradables(), // TODO rename command?!
                PrintInventory(), // TODO rename command?!
                PrintPagePositions(), // TODO rename command?!
                PrintMissingStats(), // TODO rename command?!
                PrintMissingCommander(), // TODO rename command?!
                EnterCards(),
            ),
            NoOpCliktCommand(name = "masterdata").subcommands(
                MasterdataImportCommand(),
                UpdatesPrices(),
                ImportSet(),
                PrintCollectionBinderPageView(),
            ),
            NoOpCliktCommand(name = "decklists").subcommands(
                PrintDecklist(), // TODO rename command?!
                PrintDeckboxDecks(), // TODO rename command?!
                PrintArchidektDecks(), // TODO rename command?!
                ImportDecklists(), // TODO rename command?!
                PrintManaBase(), // TODO rename command?!
            ),
        )
        .main(args)
}
