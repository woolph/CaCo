import at.woolph.caco.cli.manabase.ColorIdentity
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class CheapRampTests {
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
                "Llanowar Elves",
                "Skirk Prospector",
                "Springleaf Drum",
                "Lotus Cobra",
                "Dark Ritual",
                "Sylvan Scrying",
                "Wolfwillow Haven",
                "Aether Vial",
                "Wayfarer's Bauble",
            ).map {
                Card.find { Cards.name match it }.limit(1).first()
            }
        }.map {
            DynamicTest.dynamicTest("${it.name} should be a cheap ramp spell") {
                it.isCheapRamp shouldBe true
            }
        }


    @TestFactory
    fun negativeTests(): Collection<DynamicTest> =
        transaction {
            listOf(
                "Ranger Class",
                "Tangled Florahedron // Tangled Vale",
                "Shambling Ghast",
                "Manamorphose",
                "Crop Rotation",
                "Sylvan Scrying",
                "Armillary Sphere",
                "Ordeal of Nylea",
                "Open the Gates",
                "Mycosynth Wellspring",
                "Dreamscape Artist",
            ).map {
                Card.find { Cards.name match it }.limit(1).first()
            }
        }.map {
            DynamicTest.dynamicTest("${it.name} should not be a cheap ramp spell") {
                withClue("${it.name} should not be a cheap ramp spell oracleText = ${it.oracleText}") {
                    it.isCheapRamp shouldBe false
                }
            }
        }

    @Test
    fun printCheapRamp() {
        transaction {
            Card.all().filter { it.isCheapRamp }.forEach {
                println(it.name)
            }
        }
    }

    @Test
    fun printCheapRampInDimir() {
        transaction {
            Card.all().filter { it.colorIdentity in ColorIdentity.DIMIR }.filter { it.isCheapRamp }.forEach {
                println("${it.name}")
            }
        }
    }
}
