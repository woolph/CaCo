/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.HomeDirectory
import at.woolph.caco.cli.command.*
import at.woolph.caco.cli.command.ImportDecklists
import at.woolph.caco.cli.command.PrintArchidektDeck
import at.woolph.caco.cli.command.PrintArchidektDecks
import at.woolph.caco.cli.command.PrintCollectionBinderPageView
import at.woolph.caco.cli.command.PrintDeckboxDecks
import at.woolph.caco.cli.command.PrintDecklist
import at.woolph.caco.cli.command.PrintManaBase
import at.woolph.caco.cli.command.PrintManaBaseArchidektDeck
import at.woolph.caco.cli.command.UpdateMasterdata
import at.woolph.caco.cli.command.UpdateSets
import at.woolph.caco.cli.command.UpdatesPrices
import at.woolph.caco.datamodel.Databases
import at.woolph.lib.clikt.suspendNoOpCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.sources.PropertiesValueSource

suspend fun main(args: Array<String>) =
    suspendNoOpCliktCommand("caco") {
          val homeDirectory = HomeDirectory()
          Databases.init(homeDirectory)
          versionOption("0.3.0")
          context {
            obj = homeDirectory
            autoEnvvarPrefix = HomeDirectory.ENVVAR_PREFIX
            valueSource = PropertiesValueSource.from(homeDirectory.resolve("settings.properties"))
          }
        }
        .subcommands(
            suspendNoOpCliktCommand(name = "collection")
                .subcommands(
                    CollectionExport(),
                    CollectionImport(),
                    HighValueTradables(),
                    PrintInventory(),
                    PrintExcessPossessions(),
                    PrintPagePositions(),
                    PrintMissingStats(),
                    PrintMissing(),
                    EnterCards(),
                ),
            suspendNoOpCliktCommand(name = "masterdata")
                .subcommands(
                    UpdateMasterdata(),
                    UpdatesPrices(),
                    UpdateSets(),
                    PrintCollectionBinderPageView(),
                ),
            suspendNoOpCliktCommand(name = "decklists")
                .subcommands(
                    PrintDecklist(),
                    PrintDeckboxDecks(),
                    PrintArchidektDeck(),
                    PrintArchidektDecks(),
                    PrintDeckDiff(),
                    ImportDecklists(),
                    PrintManaBase(),
                    PrintManaBaseArchidektDeck(),
                ),
        )
        .main(args)
