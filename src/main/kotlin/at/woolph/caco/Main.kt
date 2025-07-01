package at.woolph.caco

import at.woolph.caco.cli.suspendNoOpCliktCommand
import at.woolph.caco.collection.*
import at.woolph.caco.command.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.gui.Ui
import at.woolph.caco.masterdata.UpdateMasterdataCommand
import at.woolph.caco.masterdata.UpdateSetsCommand
import at.woolph.caco.masterdata.UpdatesPrices
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.sources.PropertiesValueSource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

val ENVVAR_PREFIX: String = "CACO"

data class HomeDirectory(val path: Path) {
  constructor(): this(Path(System.getenv("${ENVVAR_PREFIX}_HOME") ?: "${System.getProperty("user.home")}/.caco"))
  init {  path.createDirectories() }
  fun resolve(vararg sub: String): Path = sub.fold(path, Path::resolve)

  override fun toString(): String = path.toString()
}

suspend fun main(args: Array<String>) =
  suspendNoOpCliktCommand("caco") {
    val homeDirectory = HomeDirectory()
    Databases.init(homeDirectory)
    versionOption("0.3.0")
    context {
        obj = homeDirectory
        autoEnvvarPrefix = ENVVAR_PREFIX
        valueSource = PropertiesValueSource.from(homeDirectory.resolve("settings.properties"))
    }
  }.subcommands(
    Ui(),
    suspendNoOpCliktCommand(name = "collection").subcommands(
      CollectionExportCommand(),
      CollectionImportCommand(),
      HighValueTradables(),
      PrintInventory(),
      PrintPagePositions(),
      PrintMissingStats(),
      PrintMissingCommander(),
      EnterCards(),
    ),
    suspendNoOpCliktCommand(name = "masterdata").subcommands(
      UpdateMasterdataCommand(),
      UpdatesPrices(),
      UpdateSetsCommand(),
      PrintCollectionBinderPageView(),
    ),
    suspendNoOpCliktCommand(name = "decklists").subcommands(
      PrintDecklist(),
      PrintDeckboxDecks(),
      PrintArchidektDeck(),
      PrintArchidektDecks(),
      ImportDecklists(),
      PrintManaBase(),
      PrintManaBaseArchidektDeck(),
    ),
  ).main(args)
