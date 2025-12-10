/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.HomeDirectory
import at.woolph.caco.cli.command.*
import at.woolph.caco.datamodel.initDatabase
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
    initDatabase(homeDirectory)
    versionOption("0.4.0")
    context {
      obj = homeDirectory
      autoEnvvarPrefix = HomeDirectory.ENVVAR_PREFIX
      valueSource = PropertiesValueSource.from(homeDirectory.resolve("settings.properties").toString())
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
          PrintMissingCmd(),
          EnterCards(),
          PrintCollectionBinderLabels(),
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
          CheckDecklistMissingCards(),
        ),
    )
    .main(args)
