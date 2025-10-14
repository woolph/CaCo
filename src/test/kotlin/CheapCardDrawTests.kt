/* Copyright 2025 Wolfgang Mayer */
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class CheapCardDrawTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun startDb() {
            Databases.init()
        }
    }

    @TestFactory
    fun positiveTests(): Collection<DynamicTest> =
        transaction {
            listOf(
                "Brainstorm",
                "Faithless Looting",
                "Deadly Dispute",
                "Omen of the Sea",
                "Growth Spiral",
                "Manamorphose",
                "Ice-Fang Coatl",
                "Expressive Iteration",
                "Drannith Stinger",
                "Street Wraith",
            ).map {
                Card.find { Cards.name match it }.limit(1).first()
            }
        }.map {
            DynamicTest.dynamicTest("${it.name} should be a cheap card draw spell") {
                it.isCheapCardDraw shouldBe true
            }
        }

    @TestFactory
    fun negativeTests(): Collection<DynamicTest> =
        transaction {
            listOf(
                "Augur of Bolas",
                "Trail of Crumbs",
                "Esper Sentinel",
                "Edgewall Innkeeper",
                "Improbable Alliance",
                "Ravenous Squirrel",
                "Hydroid Krasis",
                "Shark Typhoon",
                "Bloodtithe Harvester",
                "Ox of Agonas",
                "Fateful Absence",
                "Ledger Shredder",
            ).map {
                Card.find { Cards.name match it }.limit(1).first()
            }
        }.map {
            DynamicTest.dynamicTest("${it.name} should not be a cheap card draw spell") {
                it.isCheapCardDraw shouldBe false
            }
        }
}
