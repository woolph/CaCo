/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.caco.collection.CardCollectionItem
import at.woolph.caco.collection.CollectionFileFormat
import at.woolph.caco.collection.exportArchidekt
import at.woolph.caco.collection.exportDeckbox
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.Path

class CollectionExport : SuspendingTransactionCliktCommand(name = "export") {
    val format by option(help = "The format to export the entered cards to")
        .enum<CollectionFileFormat>()
        .default(CollectionFileFormat.ARCHIDEKT)

    val file by argument(help = "The file to export").path().default(
        Path(
            System.getProperty("user.home"),
            "Downloads",
        ),
    )

    override suspend fun runTransaction() {
        when (format) {
            CollectionFileFormat.ARCHIDEKT -> CardCollectionItem.Companion.getFromDatabase().exportArchidekt(file)
            CollectionFileFormat.DECKBOX -> CardCollectionItem.Companion.getFromDatabase().exportDeckbox(file)
        }
    }
}
