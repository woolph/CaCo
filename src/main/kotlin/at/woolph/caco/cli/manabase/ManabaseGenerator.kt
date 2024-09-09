package at.woolph.caco.cli.manabase

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class DecklistEntry(
    val cardName: String,
    val count: Int = 1,
) {
    override fun toString() = "$count $cardName"
}

// https://www.channelfireball.com/article/How-Many-Lands-Do-You-Need-in-Your-Deck-An-Updated-Analysis/cd1c1a24-d439-4a8e-b369-b936edb0b38a/
fun Collection<DecklistEntry>.suggestedLandCount() = transaction {
    val cards = this@suggestedLandCount.map { (cardName, _) ->
        try {
            Card.find { Cards.name match cardName }.limit(1).first()
        } catch(t: Throwable) {
            throw IllegalArgumentException("card $cardName not found", t)
        }
    }

    val baseLine = 31.42
    val averageManaValueFactor = 3.13
    val cheapDrawRampFactor = 0.28
    val untappedMdfcFactor = 1.0 // 0.74 according to Frank Karsten
    val tappedMdfcFactor = 1.0 // 0.38 according to Frank Karsten

    val averageManaValue = cards.sumOf { it.manaValue.toDouble() } / cards.size
    val untappedMdfcCount = cards.count { it.isMDFCLandUntapped }
    val tappedMdfcCount = cards.count { it.isMDFCLandTapped }
    val cheapDrawCount = cards.count { it.isCheapCardDraw }
    val cheapRampCount = cards.count { it.isCheapRamp }
    val cheapDrawRampCount = cheapDrawCount + cheapRampCount

    println("averageManaValue = $averageManaValue")
    println("untappedMdfcCount = $untappedMdfcCount")
    println("tappedMdfcCount = $tappedMdfcCount")
    println("cheapDrawCount = $cheapDrawCount")
    println("cheapRampCount = $cheapRampCount")

    return@transaction round(baseLine + averageManaValueFactor * averageManaValue - cheapDrawRampFactor * cheapDrawRampCount.toDouble() - tappedMdfcFactor * tappedMdfcCount - untappedMdfcFactor * untappedMdfcCount).toInt()
}

enum class ManaColor(val shortName: String, val symbol: String = "{$shortName}") {
    White("W", "{W}"),
    Blue("U", "{U}"),
    Black("B", "{B}"),
    Red("R", "{R}"),
    Green("G", "{G}"),
    Colorless("C", "{C}"),
}
fun Collection<DecklistEntry>.pipDistribution() = transaction {
    val cards = this@pipDistribution.map { (cardName, _) ->
        Card.find { Cards.name match cardName }.limit(1).first()
    }
    return@transaction PipDistribution(ManaColor.entries.map { manaColor ->
        manaColor to cards.mapNotNull { it.manaCost }.sumOf { manaCost ->
            Math.pow(Regex(Regex.escape(manaColor.symbol)).findAll(manaCost).count().toDouble(), 2.0).toInt()
        }
    }.filter { it.second > 0 }.associate { it })
}

fun generateManabase(selectionCriterion: SelectionCriterion, decklist: Collection<DecklistEntry>): List<DecklistEntry> =
    transaction {
        val suggestedLandCount = decklist.suggestedLandCount()
        val pipDistribution = decklist.pipDistribution()
        println("suggested land count = $suggestedLandCount")
        println("pipDistribution = $pipDistribution")

        val selectedLands = preferredLands
            .filter { selectionCriterion.commanderColorIdentity.contains(it) }
            .map { it to (Cards.select(Cards.price).where { (Cards.name match it.name) }.mapNotNull { it[Cards.price]?.toDouble() }.minOrNull() ?: 0.0) }
            .map { (land, price) -> land to land.desirability(selectionCriterion, pipDistribution, price)  }
            .filter { (_, desirability) -> desirability > 0.1 }
            .sortedByDescending { (_, desirability) -> desirability }
            .onEach { (land, desirability) ->
                println("${land.name} $desirability")
            }.map { (land, _) -> land }

        val neededColors = pipDistribution.pipDistribution.keys

        val colorProductionCount = neededColors.associateWith { color -> selectedLands.count { it.canProduce(color) } }
        val colorProductionDistribution = colorProductionCount.values.sum().let { totalPipCount ->
            colorProductionCount.mapValues { it.value.toDouble() / totalPipCount }
        }
        val remainingLandSlots = suggestedLandCount - selectedLands.size - neededColors.size // each basic should be contained once!

        return@transaction sequence {
            yieldAll(selectedLands.map { DecklistEntry(it.name, 1) }.sortedBy { (name, _) -> name })
            yieldAll(neededColors.map { color ->
                DecklistEntry(
                    basicLands[color]!!.name, 1 + max(
                    0,
                    ceil(
                        remainingLandSlots * (pipDistribution.pipDistribution[color]
                            ?: 0.0) - colorProductionDistribution[color]!!
                    ).toInt()
                ))
            })
        }.toList()
    }

class ColorIdentity(val colorIdentity: EnumSet<ManaColor>) {
    constructor(vararg color: ManaColor): this(if(color.isEmpty()) EnumSet.noneOf(ManaColor::class.java) else EnumSet.copyOf(color.asList()))
    operator fun contains(color: ColorIdentity) = if(color.colorIdentity.isEmpty()) true else color.colorIdentity.all { it in colorIdentity }

    override fun toString(): String = colorIdentity.toString()

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
    val manaProduction: ColorIdentity = colorIdentity,
    val basicTypes: ColorIdentity = ColorIdentity.COLORLESS,
    val desirableFactor: Double = 1.0,
    val tapLand: Boolean = false,
    val enablesFastStart: Boolean = !tapLand,
    val basic: Boolean = false,
    val artifact: Boolean = false,
    val enchantment: Boolean = false,
    val snow: Boolean = false,
    val gate: Boolean = false,
    val lifegain: Boolean = false,
    val surveil: Boolean = false,
    val scry: Boolean = false,
) {
    fun desirability(selectionCriterion: SelectionCriterion, pipDistribution: PipDistribution, price: Double) =
        if (price > selectionCriterion.maxPricePerCard)
            0.0
        else
            multiply(
                desirableFactor,
                pipDistribution.weighting(this),
                min(1.0, (manaProduction.colorIdentity.size.toDouble()+1)/(selectionCriterion.commanderColorIdentity.colorIdentity.size.toDouble()+1)),
                (1.0 + basicTypes.colorIdentity.size * selectionCriterion.basicLandTypeFactors), // increased desirability due to fetchability
                additionalDesirability(selectionCriterion),
                if (basic) selectionCriterion.basicLandFactor else 1.0, // basics are more desirable with evolving wilds et al, rampant growth, wayfarer's bauble, etc.
                if (tapLand) selectionCriterion.tapLandFactor else 1.0,
                if (enablesFastStart) selectionCriterion.fastStartFactor else 1.0,
                if (artifact) selectionCriterion.artifactFactor else 1.0,
                if (enchantment) selectionCriterion.enchantmentFactor else 1.0,
                if (snow) selectionCriterion.snowFactor else 1.0,
                if (gate) selectionCriterion.gateFactor else 1.0,
            )

    open fun additionalDesirability(selectionCriterion: SelectionCriterion) = 1.0

    fun canProduce(color: ManaColor) = color in manaProduction.colorIdentity

    fun multiply(vararg factors: Double) =
        factors.reduce { acc, d -> acc*d }
}

class SelectionCriterion(
    val commanderColorIdentity: ColorIdentity,
    val tapLandFactor: Double = 0.33,
    val basicLandFactor: Double = 1.0,
    val basicLandTypeFactors: Double = 0.2,
    val fastStartFactor: Double = 1.0,
    val maxPricePerCard: Double = Double.MAX_VALUE,
    val artifactFactor: Double = 0.05, // lower because the can be interacted better (set high if you have artifact synergies)
    val enchantmentFactor: Double = 0.05, // lower because the can be interacted better (set high if you have enchantment synergies)
    val snowFactor: Double = 0.05,
    val gateFactor: Double = 0.05,
    )

operator fun ColorIdentity.contains(land: Land) = this.contains(land.colorIdentity)

val basicLands = listOf(
    Land("Plains", ColorIdentity.WHITE, basicTypes = ColorIdentity.WHITE, basic = true),
    Land("Island", ColorIdentity.BLUE, basicTypes = ColorIdentity.BLUE, basic = true),
    Land("Swamp", ColorIdentity.BLACK, basicTypes = ColorIdentity.BLACK, basic = true),
    Land("Mountain", ColorIdentity.RED, basicTypes = ColorIdentity.RED, basic = true),
    Land("Forest", ColorIdentity.GREEN, basicTypes = ColorIdentity.GREEN, basic = true),
    Land("Wastes", ColorIdentity.COLORLESS, basic = true),
).associateBy { it.manaProduction.colorIdentity.single() }

val snowBasicLands = listOf(
    Land("Snow-Covered Plains", ColorIdentity.WHITE, basicTypes = ColorIdentity.WHITE, basic = true, snow = true),
    Land("Snow-Covered Island", ColorIdentity.BLUE, basicTypes = ColorIdentity.BLUE, basic = true, snow = true),
    Land("Snow-Covered Swamp", ColorIdentity.BLACK, basicTypes = ColorIdentity.BLACK, basic = true, snow = true),
    Land("Snow-Covered Mountain", ColorIdentity.RED, basicTypes = ColorIdentity.RED, basic = true, snow = true),
    Land("Snow-Covered Forest", ColorIdentity.GREEN, basicTypes = ColorIdentity.GREEN, basic = true, snow = true),
    Land("Snow-Covered Wastes", ColorIdentity.COLORLESS, basic = true, snow = true),
).associateBy { it.manaProduction.colorIdentity.single() }

val preferredLands = listOf(
    Land("Ancient Tomb", ColorIdentity.COLORLESS, desirableFactor = 1.5),
    object: Land("Command Tower", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            if (selectionCriterion.commanderColorIdentity.colorIdentity.size >= 2) 1.0 else 0.0
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
    Land("Hallowed Fountain", ColorIdentity.AZORIOUS, basicTypes = ColorIdentity.AZORIOUS),
    Land("Watery Grave", ColorIdentity.DIMIR, basicTypes = ColorIdentity.DIMIR),
    Land("Blood Crypt", ColorIdentity.RAKDOS, basicTypes = ColorIdentity.RAKDOS),
    Land("Stomping Ground", ColorIdentity.GRUUL, basicTypes = ColorIdentity.GRUUL),
    Land("Temple Garden", ColorIdentity.SELESNYA, basicTypes = ColorIdentity.SELESNYA),
    Land("Godless Shrine", ColorIdentity.ORZHOV, basicTypes = ColorIdentity.ORZHOV),
    Land("Steam Vents", ColorIdentity.IZZET, basicTypes = ColorIdentity.IZZET),
    Land("Overgrown Tomb", ColorIdentity.GOLGARI, basicTypes = ColorIdentity.GOLGARI),
    Land("Sacred Foundry", ColorIdentity.BOROS, basicTypes = ColorIdentity.BOROS),
    Land("Breeding Pool", ColorIdentity.SIMIC, basicTypes = ColorIdentity.SIMIC),

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
    object: Land("City of Brass", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            when (selectionCriterion.commanderColorIdentity.colorIdentity.size) {
                5 -> 1.0
                4 -> 0.67
                else -> 0.0
            }
    },
    object: Land("Grand Coliseum", ColorIdentity.COLORLESS, ColorIdentity.FIVE_COLORED, tapLand = true) {
        override fun additionalDesirability(selectionCriterion: SelectionCriterion) =
            when (selectionCriterion.commanderColorIdentity.colorIdentity.size) {
                5 -> 1.0
                4 -> 0.67
                else -> 0.0
            }
    },
    Land("Adarkar Wastes", ColorIdentity.AZORIOUS),
    Land("Underground River", ColorIdentity.DIMIR),
    Land("Sulfurous Springs", ColorIdentity.RAKDOS),
    Land("Karplusan Forest", ColorIdentity.GRUUL),
    Land("Brushland", ColorIdentity.SELESNYA),
    Land("Caves of Koilos", ColorIdentity.ORZHOV),
    Land("Shivan Reef", ColorIdentity.IZZET),
    Land("Llanowar Wastes", ColorIdentity.GOLGARI),
    Land("Battlefield Forge", ColorIdentity.BOROS),
    Land("Yavimaya Coast", ColorIdentity.SIMIC),
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
    Land("Scene of the Crime", ColorIdentity.COLORLESS, manaProduction = ColorIdentity.FIVE_COLORED, desirableFactor = 0.8, tapLand = true, artifact = true),
    Land("Power Depot", ColorIdentity.COLORLESS, manaProduction = ColorIdentity.FIVE_COLORED, desirableFactor = 0.67, tapLand = true, artifact = true),
    Land("Treasure Vault", ColorIdentity.COLORLESS, tapLand = true, artifact = true),

    // TODO Snow duals
    // TODO tapped duals
    // TODO tap lands
    // TODO scry lands
    // TODO surveil lands

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

class PipDistribution(pipCount: Map<ManaColor, Int>) {
    val pipDistribution = pipCount.values.sum().let { totalPipCount ->
        pipCount.mapValues { it.value.toDouble() / totalPipCount }
    }

    override fun toString(): String = pipDistribution.toString()

    fun weighting(land: Land) =
        pipDistribution.entries.filter { (color, _) ->
            land.manaProduction.colorIdentity.contains(color)
        }.sumOf { (_, pipWeight) -> pipWeight }
}
