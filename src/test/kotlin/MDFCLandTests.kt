/* Copyright 2025 Wolfgang Mayer */
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class MDFCLandTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun startDb() {
            Databases.init()
        }
    }

    @TestFactory
    fun tappedPositiveTests(): Collection<DynamicTest> =
        testCards(
            "Beyeen Veil // Beyeen Coast",
            "Zof Consumption // Zof Bloodbog",
            "Bloodsoaked Insight // Sanguine Morass",
        ).map {
            DynamicTest.dynamicTest("${it.name} should be a cheap ramp spell") {
                it.isMDFCLandTapped shouldBe true
            }
        }

    @TestFactory
    fun tappedNegativeTests(): Collection<DynamicTest> =
        testCards(
            "Boreal Shelf",
            "Needleverge Pathway // Pillarverge Pathway",
            "Agadeem's Awakening // Agadeem, the Undercrypt",
            "Bridgeworks Battle // Tanglespan Bridgeworks",
        ).map {
            DynamicTest.dynamicTest("${it.name} should not be a tapped MDFC") {
                withClue("${it.name} should not be a cheap ramp spell oracleText = ${it.oracleText}") {
                    it.isMDFCLandTapped shouldBe false
                }
            }
        }

    @TestFactory
    fun untappedPositiveTests(): Collection<DynamicTest> =
        testCards(
            "Agadeem's Awakening // Agadeem, the Undercrypt",
            "Bridgeworks Battle // Tanglespan Bridgeworks",
        ).map {
            DynamicTest.dynamicTest("${it.name} should be a cheap ramp spell") {
                it.isMDFCLandUntapped shouldBe true
            }
        }

    @TestFactory
    fun untappedNegativeTests(): Collection<DynamicTest> =
        testCards(
            "Boreal Shelf",
            "Needleverge Pathway // Pillarverge Pathway",
            "Beyeen Veil // Beyeen Coast",
            "Zof Consumption // Zof Bloodbog",
            "Bloodsoaked Insight // Sanguine Morass",
        ).map {
            DynamicTest.dynamicTest("${it.name} should not be a tapped MDFC") {
                withClue("${it.name} should not be a cheap ramp spell oracleText = ${it.oracleText}") {
                    it.isMDFCLandUntapped shouldBe false
                }
            }
        }

    private fun testCards(vararg cardNames: String) =
        transaction {
            cardNames.map {
                Card.find { Cards.name match it }.limit(1).first()
            }
        }
}
