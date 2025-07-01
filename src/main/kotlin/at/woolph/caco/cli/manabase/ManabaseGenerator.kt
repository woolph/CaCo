package at.woolph.caco.cli.manabase

import at.woolph.caco.cli.manabase.ManaColor
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.filterNot
import kotlin.compareTo
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.text.Regex

data class DecklistEntry(
    val cardName: String,
    val count: Int = 1,
) {
    override fun toString() = "$count ${cardName}"
}

data class DecklistEntryCard(
    val card: Card,
    val count: Int = 1,
) {
    override fun toString() = "$count ${card.name}"
}

fun Map<String, Int>.toDecklistEntries(): Collection<DecklistEntry> =
    map { (cardName, count) ->
        DecklistEntry(cardName, count)
    }

fun Collection<DecklistEntry>.toDecklistEntryCards(): Collection<DecklistEntryCard> = transaction {
    map { (cardName, count) ->
        DecklistEntryCard(
            try {
                Card.find { Cards.name match cardName }.limit(1).first()
            } catch(t: Throwable) {
                throw IllegalArgumentException("card $cardName not found", t)
            },
            count,
        )
    }
}

// https://www.channelfireball.com/article/How-Many-Lands-Do-You-Need-in-Your-Deck-An-Updated-Analysis/cd1c1a24-d439-4a8e-b369-b936edb0b38a/
fun Collection<DecklistEntryCard>.suggestedLandCount(): Int {
    val baseLine = 31.42
    val averageManaValueFactor = 3.13
    val cheapDrawRampFactor = 0.28
    val untappedMdfcFactor = 1.0 // 0.74 according to Frank Karsten
    val tappedMdfcFactor = 1.0 // 0.38 according to Frank Karsten

    val averageManaValue = sumOf { it.card.manaValue.toDouble() } / size
    val untappedMdfcCount = count { it.card.isMDFCLandUntapped }
    val tappedMdfcCount = count { it.card.isMDFCLandTapped }
    val cheapDrawCount = count { it.card.isCheapCardDraw }
    val cheapRampCount = count { it.card.isCheapRamp }
    val cheapDrawRampCount = cheapDrawCount + cheapRampCount

    println("averageManaValue = $averageManaValue")
    println("untappedMdfcCount = $untappedMdfcCount")
    println("tappedMdfcCount = $tappedMdfcCount")
    println("cheapDrawCount = $cheapDrawCount")
    println("cheapRampCount = $cheapRampCount")

    return round(baseLine + averageManaValueFactor * averageManaValue - cheapDrawRampFactor * cheapDrawRampCount.toDouble() - tappedMdfcFactor * tappedMdfcCount - untappedMdfcFactor * untappedMdfcCount).toInt()
}

enum class ManaColor(val shortName: String, val symbol: String = "{$shortName}") {
    White("W", "{W}"),
    Blue("U", "{U}"),
    Black("B", "{B}"),
    Red("R", "{R}"),
    Green("G", "{G}"),
    Colorless("C", "{C}"),
}
fun Collection<DecklistEntryCard>.pipDistribution(): PipDistribution {
    val pipCount = ManaColor.entries.map { manaColor ->
        manaColor to mapNotNull { it.card.manaCost }.sumOf { manaCost ->
            Math.pow(Regex(Regex.escape(manaColor.symbol)).findAll(manaCost).count().toDouble(), 2.0).toInt()
        }
    }.filter { it.second > 0 }.associate { it }

    return PipDistribution(pipCount.values.sum().let { totalPipCount ->
        pipCount.mapValues { it.value.toDouble() / totalPipCount.toDouble() }
    })
}

fun Collection<DecklistEntryCard>.meanPipRequirement() = PipDistribution(ManaColor.entries.associateWith { manaColor ->
    this@meanPipRequirement.filterNot { it.card.isLand }.mapNotNull { it.card.manaCost }.map { Regex(Regex.escape(manaColor.symbol)).findAll(it).count() }.average()
}.filterValues { it > 0.0 })

fun Collection<Land>.meanProduction(totalCount: Int = this@meanProduction.size) = ManaColor.entries.associateWith { color -> this@meanProduction.count { color in it.manaProduction }
  .toDouble() / totalCount }

fun generateManabase(selectionCriterion: SelectionCriterion, decklist: Collection<DecklistEntryCard>): List<DecklistEntry> =
    transaction {
        val decklistWithoutLands =  decklist.filterNot{ it.card.isLand }
        val suggestedLandCount = decklistWithoutLands.suggestedLandCount()
        val pipDistribution = decklistWithoutLands.meanPipRequirement()
        println("suggested land count = $suggestedLandCount")
        println("pipDistribution = $pipDistribution")

        val neededColors = pipDistribution.pipDistribution.keys
        val filteredBasicLands = basicLands.filterKeys { it in neededColors }.values
        val filteredLands = preferredLands
            .filter { selectionCriterion.commanderColorIdentity.contains(it) }
            .filter { (Cards.select(Cards.price).where { (Cards.name match it.name) }.mapNotNull { it[Cards.price]?.toDouble() }.minOrNull() ?: 0.0) <= selectionCriterion.maxPricePerCard }
            .toMutableList()

        val selectedLands = mutableListOf<Land>()

        while (selectedLands.size < suggestedLandCount-selectionCriterion.minBasicLandCount && filteredLands.isNotEmpty()) {
            val pickedLand = filteredLands.maxBy { it.desirability(selectionCriterion, pipDistribution, selectedLands.meanProduction(suggestedLandCount)) }
            selectedLands.add(pickedLand)
            filteredLands.remove(pickedLand)

            println("picked ${pickedLand.name}")
        }

        // each basic once
        filteredBasicLands.filter { neededColors.contains(it.manaProduction.single()) }.forEach { pickedLand ->
            selectedLands.add(pickedLand)
            println("picked ${pickedLand.name}")
        }
306
        while (selectedLands.size < suggestedLandCount) {
            val pickedLand = filteredBasicLands.maxBy { it.desirability(selectionCriterion, pipDistribution, selectedLands.meanProduction(suggestedLandCount)) }
            selectedLands.add(pickedLand)
            println("picked ${pickedLand.name}")
        }

        val lands = selectedLands.groupBy { it }.mapValues { (_, list) -> list.size }.entries

        println("productionDistribution = ${ManaColor.entries.associateWith { color -> lands.filter { (land, _) -> color in land.manaProduction }.sumOf { (_, count) -> count }.toDouble() / lands.size }}")

        return@transaction lands.map { (land, count) -> DecklistEntry(land.name, count) }
    }

class ColorIdentity(val colorIdentity: Set<ManaColor>) {
    constructor(vararg color: ManaColor): this(if(color.isEmpty()) EnumSet.noneOf(ManaColor::class.java) else EnumSet.copyOf(color.asList()))
    operator fun contains(color: ColorIdentity) = if(color.colorIdentity.isEmpty()) true else color.colorIdentity.all { it in colorIdentity }

    override fun toString(): String = colorIdentity.toString()

    operator fun plus(colorIdentity: ColorIdentity) = this + colorIdentity.colorIdentity
    operator fun plus(colorIdentity: Set<ManaColor>) = ColorIdentity(this.colorIdentity + colorIdentity)

    companion object {
        operator fun invoke(string: String) = when(val colorCode = string.lowercase()) {
            "" -> COLORLESS
            "azorious" -> AZORIOUS
            "dimir" -> DIMIR
            "rakdos" -> RAKDOS
            "gruul" -> GRUUL
            "selesnya" -> SELESNYA
            "orzhov" -> ORZHOV
            "golgari" -> GOLGARI
            "simic" -> SIMIC
            "izzet" -> IZZET
            "boros" -> BOROS
            "esper" -> ESPER
            "grixis" -> GRIXIS
            "jund" -> JUND
            "naya" -> NAYA
            "bant" -> BANT
            "jeskai" -> JESKAI
            "sultai" -> SULTAI
            "mardu" -> MARDU
            "temur" -> TEMUR
            "abzan" -> ABZAN
            "5c" -> FIVE_COLORED
            else -> ColorIdentity(EnumSet.copyOf(buildSet<ManaColor> {
                if(colorCode.contains("w")) add(ManaColor.White)
                if(colorCode.contains("u")) add(ManaColor.Blue)
                if(colorCode.contains("b")) add(ManaColor.Black)
                if(colorCode.contains("r")) add(ManaColor.Red)
                if(colorCode.contains("g")) add(ManaColor.Green)
            }))
        }

        val WHITE = ColorIdentity(ManaColor.White)
        val BLUE = ColorIdentity(ManaColor.Blue)
        val BLACK = ColorIdentity(ManaColor.Black)
        val RED = ColorIdentity(ManaColor.Red)
        val GREEN = ColorIdentity(ManaColor.Green)
        val AZORIOUS = ColorIdentity(ManaColor.Blue, ManaColor.White)
        val DIMIR = ColorIdentity(ManaColor.Blue, ManaColor.Black)
        val RAKDOS = ColorIdentity(ManaColor.Red, ManaColor.Black)
        val GRUUL = ColorIdentity(ManaColor.Red, ManaColor.Green)
        val SELESNYA = ColorIdentity(ManaColor.White, ManaColor.Green)
        val ORZHOV = ColorIdentity(ManaColor.White, ManaColor.Black)
        val GOLGARI = ColorIdentity(ManaColor.Green, ManaColor.Black)
        val SIMIC = ColorIdentity(ManaColor.Green, ManaColor.Blue)
        val IZZET = ColorIdentity(ManaColor.Red, ManaColor.Blue)
        val BOROS = ColorIdentity(ManaColor.Red, ManaColor.White)
        val ESPER = ColorIdentity(ManaColor.White, ManaColor.Blue, ManaColor.Black)
        val GRIXIS = ColorIdentity(ManaColor.Blue, ManaColor.Black, ManaColor.Red)
        val JUND = ColorIdentity(ManaColor.Black, ManaColor.Red, ManaColor.Green)
        val NAYA = ColorIdentity(ManaColor.Red, ManaColor.Green, ManaColor.White)
        val BANT = ColorIdentity(ManaColor.Green, ManaColor.White, ManaColor.Blue)
        val JESKAI = ColorIdentity(ManaColor.White, ManaColor.Blue, ManaColor.Red)
        val SULTAI = ColorIdentity(ManaColor.Blue, ManaColor.Black, ManaColor.Green)
        val MARDU = ColorIdentity(ManaColor.Black, ManaColor.Red, ManaColor.White)
        val TEMUR = ColorIdentity(ManaColor.Red, ManaColor.Green, ManaColor.Blue)
        val ABZAN = ColorIdentity(ManaColor.Green, ManaColor.White, ManaColor.Black)
        val COLORLESS = ColorIdentity(ManaColor.Colorless)
        val FIVE_COLORED = ColorIdentity(ManaColor.White, ManaColor.Blue, ManaColor.Black, ManaColor.Red, ManaColor.Green)
    }
}

open class Land(
    val name: String,
    val colorIdentity: ColorIdentity,
    val manaProduction: Set<ManaColor> = colorIdentity.colorIdentity,
    val basicTypes: ColorIdentity = ColorIdentity.COLORLESS,
    val desirableFactor: Double = 1.0,
    val tapLand: Boolean = false,
    val enablesFastStart: Boolean = !tapLand,
    val basic: Boolean = false,
    val artifact: Boolean = false,
    val enchantment: Boolean = false,
    val snow: Boolean = false,
    val gate: Boolean = false,
    val pain: Boolean = false,
    val lifegain: Boolean = false,
    val surveil: Boolean = false,
    val scry: Boolean = false,
) {
    fun desirability(selectionCriterion: SelectionCriterion, pipDistribution: PipDistribution, alreadyProduced: Map<ManaColor, Double>) = multiply(
        desirableFactor,
        (pipDistribution - alreadyProduced).weighting(this), // FIXME correct the alreadyProduced weighting
        min(1.0, (manaProduction.size.toDouble()+1)/(selectionCriterion.commanderColorIdentity.colorIdentity.size.toDouble()+1)),
        (1.0 + basicTypes.colorIdentity.size * selectionCriterion.basicLandTypeFactors), // increased desirability due to fetchability
        additionalDesirability(selectionCriterion),
        if (basic) selectionCriterion.basicLandFactor else 1.0, // basics are more desirable with evolving wilds et al, rampant growth, wayfarer's bauble, etc.
        if (tapLand) selectionCriterion.tapLandFactor else 1.0,
        if (enablesFastStart) selectionCriterion.fastStartFactor else 1.0,
        if (artifact) selectionCriterion.artifactFactor else 1.0,
        if (enchantment) selectionCriterion.enchantmentFactor else 1.0,
        if (snow) selectionCriterion.snowFactor else 1.0,
        if (gate) selectionCriterion.gateFactor else 1.0,
        if (pain) selectionCriterion.painFactor else 1.0,
        if (lifegain) selectionCriterion.lifegainFactor else 1.0,
        if (surveil) selectionCriterion.surveilFactor else 1.0,
        if (scry) selectionCriterion.scryFactor else 1.0,
    )

    open fun additionalDesirability(selectionCriterion: SelectionCriterion) = 1.0

    fun canProduce(color: ManaColor) = color in manaProduction

    fun multiply(vararg factors: Double) =
        factors.reduce { acc, d -> acc*d }
}

class SelectionCriterion(
    val commanderColorIdentity: ColorIdentity,
    val minBasicLandCount: Int = commanderColorIdentity.colorIdentity.size,
    val tapLandFactor: Double = 0.33,
    val basicLandFactor: Double = 1.0,
    val basicLandTypeFactors: Double = 0.2,
    val fastStartFactor: Double = 1.0,
    val maxPricePerCard: Double = Double.MAX_VALUE,
    val artifactFactor: Double = 0.05, // lower because the can be interacted better (set high if you have artifact synergies)
    val enchantmentFactor: Double = 0.05, // lower because the can be interacted better (set high if you have enchantment synergies)
    val snowFactor: Double = 0.05,
    val gateFactor: Double = 0.05,
    val painFactor: Double = 0.95,
    val lifegainFactor: Double = 1.05,
    val surveilFactor: Double = 1.15,
    val scryFactor: Double = 1.1,
)

operator fun ColorIdentity.contains(land: Land) = this.contains(land.colorIdentity)

val basicLands = listOf(
    Land("Plains", ColorIdentity.WHITE, basicTypes = ColorIdentity.WHITE, basic = true),
    Land("Island", ColorIdentity.BLUE, basicTypes = ColorIdentity.BLUE, basic = true),
    Land("Swamp", ColorIdentity.BLACK, basicTypes = ColorIdentity.BLACK, basic = true),
    Land("Mountain", ColorIdentity.RED, basicTypes = ColorIdentity.RED, basic = true),
    Land("Forest", ColorIdentity.GREEN, basicTypes = ColorIdentity.GREEN, basic = true),
    Land("Wastes", ColorIdentity.COLORLESS, basic = true),
).associateBy { it.manaProduction.single() }

val snowBasicLands = listOf(
    Land("Snow-Covered Plains", ColorIdentity.WHITE, basicTypes = ColorIdentity.WHITE, basic = true, snow = true),
    Land("Snow-Covered Island", ColorIdentity.BLUE, basicTypes = ColorIdentity.BLUE, basic = true, snow = true),
    Land("Snow-Covered Swamp", ColorIdentity.BLACK, basicTypes = ColorIdentity.BLACK, basic = true, snow = true),
    Land("Snow-Covered Mountain", ColorIdentity.RED, basicTypes = ColorIdentity.RED, basic = true, snow = true),
    Land("Snow-Covered Forest", ColorIdentity.GREEN, basicTypes = ColorIdentity.GREEN, basic = true, snow = true),
    Land("Snow-Covered Wastes", ColorIdentity.COLORLESS, basic = true, snow = true),
).associateBy { it.manaProduction.single() }

val preferredLands = listOf(
    Land("Ancient Tomb", ColorIdentity.COLORLESS, desirableFactor = 1.5, pain = true),
    object: Land("Command Tower", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED.colorIdentity) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            if (selectionCriterion.commanderColorIdentity.colorIdentity.size >= 2) 1.0 else 0.0
    },
    object: Land("Exotic Orchard", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED.colorIdentity) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            when (selectionCriterion.commanderColorIdentity.colorIdentity.size) {
                5 -> 0.95
                4 -> 0.6
                else -> 0.0
            }
    },

    // Artifact lands
    Land("Ancient Den", ColorIdentity.WHITE, artifact = true),
    Land("Seat of the Synod", ColorIdentity.BLUE, artifact = true),
    Land("Vault of Whispers", ColorIdentity.BLACK, artifact = true),
    Land("Great Furnace", ColorIdentity.RED, artifact = true),
    Land("Tree of Tales", ColorIdentity.GREEN, artifact = true),
    Land("Darksteel Citadel", ColorIdentity.COLORLESS, artifact = true),
    Land("Razortide Bridge", ColorIdentity.AZORIOUS, artifact = true, tapLand = true),
    Land("Mistvault Bridge", ColorIdentity.DIMIR, artifact = true, tapLand = true),
    Land("Drossforge Bridge", ColorIdentity.RAKDOS, artifact = true, tapLand = true),
    Land("Slagwoods Bridge", ColorIdentity.GRUUL, artifact = true, tapLand = true),
    Land("Thornglint Bridge", ColorIdentity.SELESNYA, artifact = true, tapLand = true),
    Land("Goldmire Bridge", ColorIdentity.ORZHOV, artifact = true, tapLand = true),
    Land("Silverbluff Bridge", ColorIdentity.IZZET, artifact = true, tapLand = true),
    Land("Darkmoss Bridge", ColorIdentity.GOLGARI, artifact = true, tapLand = true),
    Land("Rustvale Bridge", ColorIdentity.BOROS, artifact = true, tapLand = true),
    Land("Tanglepool Bridge", ColorIdentity.SIMIC, artifact = true, tapLand = true),

    // Triomes
    Land("Savai Triome", ColorIdentity.MARDU, basicTypes = ColorIdentity.MARDU, tapLand = true),
    Land("Indatha Triome", ColorIdentity.ABZAN, basicTypes = ColorIdentity.ABZAN, tapLand = true),
    Land("Ketria Triome", ColorIdentity.TEMUR, basicTypes = ColorIdentity.TEMUR, tapLand = true),
    Land("Raugrin Triome", ColorIdentity.JESKAI, basicTypes = ColorIdentity.JESKAI, tapLand = true),
    Land("Zagoth Triome", ColorIdentity.SULTAI, basicTypes = ColorIdentity.SULTAI, tapLand = true),
    Land("Spara's Headquarters", ColorIdentity.BANT, basicTypes = ColorIdentity.BANT, tapLand = true),
    Land("Raffine's Tower", ColorIdentity.ESPER, basicTypes = ColorIdentity.ESPER, tapLand = true),
    Land("Xander's Lounge", ColorIdentity.GRIXIS, basicTypes = ColorIdentity.GRIXIS, tapLand = true),
    Land("Ziatora's Proving Ground", ColorIdentity.JUND, basicTypes = ColorIdentity.JUND, tapLand = true),
    Land("Jetmir's Garden", ColorIdentity.NAYA, basicTypes = ColorIdentity.NAYA, tapLand = true),

    // Tri-color taplands
    Land("Arcane Sanctum", ColorIdentity.ESPER, tapLand = true),
    Land("Jungle Shrine", ColorIdentity.NAYA, tapLand = true),
    Land("Crumbling Necropolis", ColorIdentity.GRIXIS, tapLand = true),
    Land("Savage Lands", ColorIdentity.JUND, tapLand = true),
    Land("Seaside Citadel", ColorIdentity.BANT, tapLand = true),
    Land("Mystic Monastery", ColorIdentity.JESKAI, tapLand = true),
    Land("Nomad Outpost", ColorIdentity.MARDU, tapLand = true),
    Land("Opulent Palace", ColorIdentity.SULTAI, tapLand = true),
    Land("Sandsteppe Citadel", ColorIdentity.ABZAN, tapLand = true),
    Land("Frontier Bivouac", ColorIdentity.TEMUR, tapLand = true),

    // Shocks
    Land("Hallowed Fountain", ColorIdentity.AZORIOUS, basicTypes = ColorIdentity.AZORIOUS, pain = true),
    Land("Watery Grave", ColorIdentity.DIMIR, basicTypes = ColorIdentity.DIMIR, pain = true),
    Land("Blood Crypt", ColorIdentity.RAKDOS, basicTypes = ColorIdentity.RAKDOS, pain = true),
    Land("Stomping Ground", ColorIdentity.GRUUL, basicTypes = ColorIdentity.GRUUL, pain = true),
    Land("Temple Garden", ColorIdentity.SELESNYA, basicTypes = ColorIdentity.SELESNYA, pain = true),
    Land("Godless Shrine", ColorIdentity.ORZHOV, basicTypes = ColorIdentity.ORZHOV, pain = true),
    Land("Steam Vents", ColorIdentity.IZZET, basicTypes = ColorIdentity.IZZET, pain = true),
    Land("Overgrown Tomb", ColorIdentity.GOLGARI, basicTypes = ColorIdentity.GOLGARI, pain = true),
    Land("Sacred Foundry", ColorIdentity.BOROS, basicTypes = ColorIdentity.BOROS, pain = true),
    Land("Breeding Pool", ColorIdentity.SIMIC, basicTypes = ColorIdentity.SIMIC, pain = true),

    // Fast Lands
    Land("Seachrome Coast", ColorIdentity.AZORIOUS),
    Land("Darkslick Shores",  ColorIdentity.DIMIR),
    Land("Blackcleave Cliffs", ColorIdentity.RAKDOS),
    Land("Copperline Gorge",  ColorIdentity.GRUUL),
    Land("Razorverge Thicket", ColorIdentity.SELESNYA),
    Land("Concealed Courtyard", ColorIdentity.ORZHOV),
    Land("Spirebluff Canal", ColorIdentity.IZZET),
    Land("Blooming Marsh", ColorIdentity.GOLGARI),
    Land("Inspiring Vantage", ColorIdentity.BOROS),
    Land("Botanical Sanctum", ColorIdentity.SIMIC),

    // Bounce Lands (increased desirablity due to card advantage and utilization with MDFC)
    Land("Azorius Chancery", ColorIdentity.AZORIOUS, tapLand = true, desirableFactor = 1.33),
    Land("Dimir Aqueduct", ColorIdentity.DIMIR, tapLand = true, desirableFactor = 1.33),
    Land("Rakdos Carnarium", ColorIdentity.RAKDOS, tapLand = true, desirableFactor = 1.33),
    Land("Gruul Turf", ColorIdentity.GRUUL, tapLand = true, desirableFactor = 1.33),
    Land("Selesnya Sanctuary", ColorIdentity.SELESNYA, tapLand = true, desirableFactor = 1.33),
    Land("Orzhov Basilica", ColorIdentity.ORZHOV, tapLand = true, desirableFactor = 1.33),
    Land("Izzet Boilerworks", ColorIdentity.IZZET, tapLand = true, desirableFactor = 1.33),
    Land("Golgari Rot Farm", ColorIdentity.GOLGARI, tapLand = true, desirableFactor = 1.33),
    Land("Boros Garrison", ColorIdentity.BOROS, tapLand = true, desirableFactor = 1.33),
    Land("Simic Growth Chamber", ColorIdentity.SIMIC, tapLand = true, desirableFactor = 1.33),

    // Bond lands
    Land("Sea of Clouds", ColorIdentity.AZORIOUS),
    Land("Morphic Pool", ColorIdentity.DIMIR),
    Land("Luxury Suite", ColorIdentity.RAKDOS),
    Land("Spire Garden", ColorIdentity.GRUUL),
    Land("Bountiful Promenade", ColorIdentity.SELESNYA),
    Land("Vault of Champions", ColorIdentity.ORZHOV),
    Land("Training Center", ColorIdentity.IZZET),
    Land("Undergrowth Stadium", ColorIdentity.GOLGARI),
    Land("Spectator Seating", ColorIdentity.BOROS),
    Land("Rejuvenating Springs", ColorIdentity.SIMIC),

    // Slow lands
    Land("Deserted Beach", ColorIdentity.AZORIOUS),
    Land("Shipwreck Marsh", ColorIdentity.DIMIR),
    Land("Haunted Ridge", ColorIdentity.RAKDOS),
    Land("Rockfall Vale", ColorIdentity.GRUUL),
    Land("Overgrown Farmland", ColorIdentity.SELESNYA),
    Land("Shattered Sanctum", ColorIdentity.ORZHOV),
    Land("Stormcarved Coast", ColorIdentity.IZZET),
    Land("Deathcap Glade", ColorIdentity.GOLGARI),
    Land("Sundown Pass", ColorIdentity.BOROS),
    Land("Dreamroot Cascade", ColorIdentity.SIMIC),

    // Reveal lands
    Land("Port Town", ColorIdentity.AZORIOUS),
    Land("Choked Estuary", ColorIdentity.DIMIR),
    Land("Foreboding Ruins", ColorIdentity.RAKDOS),
    Land("Game Trail", ColorIdentity.GRUUL),
    Land("Fortified Village", ColorIdentity.SELESNYA),
    Land("Shineshadow Snarl", ColorIdentity.ORZHOV),
    Land("Frostboil Snarl", ColorIdentity.IZZET),
    Land("Necroblossom Snarl", ColorIdentity.GOLGARI),
    Land("Furycalm Snarl", ColorIdentity.BOROS),
    Land("Vineglimmer Snarl", ColorIdentity.SIMIC),

    // Gates
    Land("Azorius Guildgate", ColorIdentity.AZORIOUS, gate = true, tapLand = true),
    Land("Dimir Guildgate", ColorIdentity.DIMIR, gate = true, tapLand = true),
    Land("Rakdos Guildgate", ColorIdentity.RAKDOS, gate = true, tapLand = true),
    Land("Gruul Guildgate", ColorIdentity.GRUUL, gate = true, tapLand = true),
    Land("Selesnya Guildgate", ColorIdentity.SELESNYA, gate = true, tapLand = true),
    Land("Orzhov Guildgate", ColorIdentity.ORZHOV, gate = true, tapLand = true),
    Land("Izzet Guildgate", ColorIdentity.IZZET, gate = true, tapLand = true),
    Land("Golgari Guildgate", ColorIdentity.GOLGARI, gate = true, tapLand = true),
    Land("Boros Guildgate", ColorIdentity.BOROS, gate = true, tapLand = true),
    Land("Simic Guildgate", ColorIdentity.SIMIC, gate = true, tapLand = true),

    // Filter lands
    Land("Mystic Gate", ColorIdentity.AZORIOUS),
    Land("Sunken Ruins", ColorIdentity.DIMIR),
    Land("Graven Cairns", ColorIdentity.RAKDOS),
    Land("Fire-Lit Thicket", ColorIdentity.GRUUL),
    Land("Wooded Bastion", ColorIdentity.SELESNYA),
    Land("Fetid Heath", ColorIdentity.ORZHOV),
    Land("Cascade Bluffs", ColorIdentity.IZZET),
    Land("Twilight Mire", ColorIdentity.GOLGARI),
    Land("Rugged Prairie", ColorIdentity.BOROS),
    Land("Flooded Grove", ColorIdentity.SIMIC),
    Land("Skycloud Expanse", ColorIdentity.AZORIOUS),
    Land("Darkwater Catacombs", ColorIdentity.DIMIR),
    Land("Shadowblood Ridge", ColorIdentity.RAKDOS),
    Land("Mossfire Valley", ColorIdentity.GRUUL),
    Land("Sungrass Prairie", ColorIdentity.SELESNYA),
    Land("Desolate Mire", ColorIdentity.ORZHOV),
    Land("Ferrous Lake", ColorIdentity.IZZET),
    Land("Viridescent Bog", ColorIdentity.GOLGARI),
    Land("Sunscorched Divide", ColorIdentity.BOROS),
    Land("Overflowing Basin", ColorIdentity.SIMIC),

    // MDFCs
    Land("Hengegate Pathway // Mistgate Pathway", ColorIdentity.AZORIOUS),
    Land("Clearwater Pathway // Murkwater Pathway", ColorIdentity.DIMIR),
    Land("Blightstep Pathway // Searstep Pathway", ColorIdentity.RAKDOS),
    Land("Cragcrown Pathway // Timbercrown Pathway", ColorIdentity.GRUUL),
    Land("Branchloft Pathway // Boulderloft Pathway", ColorIdentity.SELESNYA),
    Land("Brightclimb Pathway // Grimclimb Pathway", ColorIdentity.ORZHOV),
    Land("Riverglide Pathway // Lavaglide Pathway", ColorIdentity.IZZET),
    Land("Darkbore Pathway // Slitherbore Pathway", ColorIdentity.GOLGARI),
    Land("Needleverge Pathway // Pillarverge Pathway", ColorIdentity.BOROS),
    Land("Barkchannel Pathway // Tidechannel Pathway", ColorIdentity.SIMIC),

    // Pain lands
    object: Land("City of Brass", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED.colorIdentity, pain = true) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            when (selectionCriterion.commanderColorIdentity.colorIdentity.size) {
                5 -> 1.0
                4 -> 0.67
                else -> 0.0
            }
    },
    object: Land("Grand Coliseum", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED.colorIdentity + ManaColor.Colorless, tapLand = true, pain = true) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            when (selectionCriterion.commanderColorIdentity.colorIdentity.size) {
                5 -> 1.0
                4 -> 0.67
                else -> 0.0
            }
    },
    Land("Adarkar Wastes", ColorIdentity.AZORIOUS, ColorIdentity.AZORIOUS.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Underground River", ColorIdentity.DIMIR, ColorIdentity.DIMIR.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Sulfurous Springs", ColorIdentity.RAKDOS, ColorIdentity.RAKDOS.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Karplusan Forest", ColorIdentity.GRUUL, ColorIdentity.GRUUL.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Brushland", ColorIdentity.SELESNYA, ColorIdentity.SELESNYA.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Caves of Koilos", ColorIdentity.ORZHOV, ColorIdentity.ORZHOV.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Shivan Reef", ColorIdentity.IZZET, ColorIdentity.IZZET.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Llanowar Wastes", ColorIdentity.GOLGARI, ColorIdentity.GOLGARI.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Battlefield Forge", ColorIdentity.BOROS, ColorIdentity.BOROS.colorIdentity + ManaColor.Colorless, pain = true),
    Land("Yavimaya Coast", ColorIdentity.SIMIC, ColorIdentity.SIMIC.colorIdentity + ManaColor.Colorless, pain = true),
    // Utility Colorless
    object: Land("Command Beacon", ColorIdentity.COLORLESS) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            1.0 // TODO calculate importance of commander
    },

    // fetches
    FetchLand("Flooded Strand", ColorIdentity.AZORIOUS),
    FetchLand("Polluted Delta", ColorIdentity.DIMIR),
    FetchLand("Bloodstained Mire", ColorIdentity.RAKDOS),
    FetchLand("Wooded Foothills", ColorIdentity.GRUUL),
    FetchLand("Windswept Heath", ColorIdentity.SELESNYA),
    FetchLand("Marsh Flats", ColorIdentity.ORZHOV),
    FetchLand("Scalding Tarn", ColorIdentity.IZZET),
    FetchLand("Verdant Catacombs", ColorIdentity.GOLGARI),
    FetchLand("Arid Mesa", ColorIdentity.BOROS),
    FetchLand("Misty Rainforest", ColorIdentity.SIMIC),

    // dual lands
    Land("Tundra", ColorIdentity.AZORIOUS, basicTypes= ColorIdentity.AZORIOUS),
    Land("Underground Sea", ColorIdentity.DIMIR, basicTypes= ColorIdentity.DIMIR),
    Land("Badlands", ColorIdentity.RAKDOS, basicTypes= ColorIdentity.RAKDOS),
    Land("Taiga", ColorIdentity.GRUUL, basicTypes= ColorIdentity.GRUUL),
    Land("Savannah", ColorIdentity.SELESNYA, basicTypes= ColorIdentity.SELESNYA),
    Land("Scrubland", ColorIdentity.ORZHOV, basicTypes= ColorIdentity.ORZHOV),
    Land("Volcanic Island", ColorIdentity.IZZET, basicTypes= ColorIdentity.IZZET),
    Land("Bayou", ColorIdentity.GOLGARI, basicTypes= ColorIdentity.GOLGARI),
    Land("Plateau", ColorIdentity.BOROS, basicTypes= ColorIdentity.BOROS),
    Land("Tropical Island", ColorIdentity.SIMIC, basicTypes= ColorIdentity.SIMIC),

    // utility
    Land("Scene of the Crime", ColorIdentity.COLORLESS, manaProduction = ColorIdentity.FIVE_COLORED.colorIdentity + ManaColor.Colorless, desirableFactor = 0.8, tapLand = true, artifact = true),
    Land("Power Depot", ColorIdentity.COLORLESS, manaProduction = ColorIdentity.FIVE_COLORED.colorIdentity + ManaColor.Colorless, desirableFactor = 0.67, tapLand = true, artifact = true),
    Land("Treasure Vault", ColorIdentity.COLORLESS, tapLand = true, artifact = true),

    // TODO Snow duals
    // TODO tapped duals
    // TODO tap lands

    // Surveil lands
    Land("Meticulous Archive", ColorIdentity.AZORIOUS, basicTypes= ColorIdentity.AZORIOUS, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Undercity Sewers", ColorIdentity.DIMIR, basicTypes= ColorIdentity.DIMIR, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Raucous Theater", ColorIdentity.RAKDOS, basicTypes= ColorIdentity.RAKDOS, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Commercial District", ColorIdentity.GRUUL, basicTypes= ColorIdentity.GRUUL, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Lush Portico", ColorIdentity.SELESNYA, basicTypes= ColorIdentity.SELESNYA, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Shadowy Backstreet", ColorIdentity.ORZHOV, basicTypes= ColorIdentity.ORZHOV, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Thundering Falls", ColorIdentity.IZZET, basicTypes= ColorIdentity.IZZET, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Underground Mortuary", ColorIdentity.GOLGARI, basicTypes= ColorIdentity.GOLGARI, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Elegant Parlor", ColorIdentity.BOROS, basicTypes= ColorIdentity.BOROS, tapLand = true, surveil = true, desirableFactor = 1.2),
    Land("Hedge Maze", ColorIdentity.SIMIC, basicTypes= ColorIdentity.SIMIC, tapLand = true, surveil = true, desirableFactor = 1.2),

    // Scry lands
    Land("Temple of Enlightenment", ColorIdentity.AZORIOUS, basicTypes= ColorIdentity.AZORIOUS, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Deceit", ColorIdentity.DIMIR, basicTypes= ColorIdentity.DIMIR, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Malice", ColorIdentity.RAKDOS, basicTypes= ColorIdentity.RAKDOS, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Abandon", ColorIdentity.GRUUL, basicTypes= ColorIdentity.GRUUL, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Plenty", ColorIdentity.SELESNYA, basicTypes= ColorIdentity.SELESNYA, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Silence", ColorIdentity.ORZHOV, basicTypes= ColorIdentity.ORZHOV, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Epiphany", ColorIdentity.IZZET, basicTypes= ColorIdentity.IZZET, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Malady", ColorIdentity.GOLGARI, basicTypes= ColorIdentity.GOLGARI, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Triumph", ColorIdentity.BOROS, basicTypes= ColorIdentity.BOROS, tapLand = true, scry = true, desirableFactor = 1.1),
    Land("Temple of Mystery", ColorIdentity.SIMIC, basicTypes= ColorIdentity.SIMIC, tapLand = true, scry = true, desirableFactor = 1.1),


    // Gain lands
    Land("Radiant Fountain", ColorIdentity.COLORLESS, tapLand = true, lifegain = true),
    Land("Sejiri Refuge", ColorIdentity.AZORIOUS, tapLand = true, lifegain = true),
    Land("Jwar Isle Refuge", ColorIdentity.DIMIR, tapLand = true, lifegain = true),
    Land("Akoum Refuge", ColorIdentity.RAKDOS, tapLand = true, lifegain = true),
    Land("Kazandu Refuge", ColorIdentity.GRUUL, tapLand = true, lifegain = true),
    Land("Graypelt Refuge", ColorIdentity.SELESNYA, tapLand = true, lifegain = true),
    Land("Tranquil Cove", ColorIdentity.AZORIOUS, tapLand = true, lifegain = true),
    Land("Dismal Backwater", ColorIdentity.DIMIR, tapLand = true, lifegain = true),
    Land("Bloodfell Caves", ColorIdentity.RAKDOS, tapLand = true, lifegain = true),
    Land("Rugged Highlands", ColorIdentity.GRUUL, tapLand = true, lifegain = true),
    Land("Blossoming Sands ", ColorIdentity.SELESNYA, tapLand = true, lifegain = true),
    Land("Scoured Barrens", ColorIdentity.ORZHOV, tapLand = true, lifegain = true),
    Land("Swiftwater Cliffs", ColorIdentity.IZZET, tapLand = true, lifegain = true),
    Land("Jungle Hollow", ColorIdentity.GOLGARI, tapLand = true, lifegain = true),
    Land("Wind-Scarred Crag", ColorIdentity.BOROS, tapLand = true, lifegain = true),
    Land("Thornwood Falls", ColorIdentity.SIMIC, tapLand = true, lifegain = true),

)

class FetchLand(
    name: String,
    val fetchingIdentity: ColorIdentity,
): Land(name, ColorIdentity.COLORLESS, desirableFactor = 1.5) {
    override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
        fetchingIdentity.colorIdentity.count { it in selectionCriterion.commanderColorIdentity.colorIdentity } * 0.5
}

class PipDistribution(val pipDistribution: Map<ManaColor, Double>) {

    override fun toString(): String = pipDistribution.toString()

    fun weighting(land: Land) =
        pipDistribution.entries.filter { (color, _) ->
            land.manaProduction.contains(color)
        }.sumOf { (_, pipWeight) -> pipWeight }

    operator fun minus(meanProduction: Map<ManaColor, Double>) = PipDistribution(pipDistribution.mapValues { (key, value) -> value - (meanProduction[key] ?: 0.0) })
}
