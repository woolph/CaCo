package at.woolph.caco.cli

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.imagecache.ImageCache
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.util.*

class DeckBuildingListPrinter {

    // TODO mark editions where I do have surplus (more than needed for collection)
    // TODO mark editions where I do have mystery booster/the list editions
    // TODO exclude from list every CardPossssion which is used for a deck
    suspend fun printList(deckName: String, cardNames: List<String>, file: String) {
        Databases.init()

        createPdfDocument {
            val pageFormat = PDRectangle.A4

            val fontColor = Color.BLACK
            val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))
            val fontTitle = Font(fontFamily72Black, 12f)
            val fontCard = Font(PDType1Font.HELVETICA, 8f)
            val fontCode = Font(PDType1Font.HELVETICA_OBLIQUE, 6f)

            data class Position(
                val x: Float,
                val y: Float,
            ) {
                operator fun plus(p: Position) = Position(x + p.x, y + p.y)
                operator fun times(d: Float) = Position(x * d, y * d)
                operator fun times(p: Position) = Position(x * p.x, y * p.y)
                operator fun div(d: Float) = Position(x / d, y / d)
                operator fun div(p: Position) = Position(x / p.x, y / p.y)
                operator fun minus(p: Position) = Position(x - p.x, y -p.y)
                operator fun unaryPlus() = this
                operator fun unaryMinus() = Position(-x, -y)
            }
            fun PDRectangle.toPosition() = Position(width, height)

            val margin = Position(10.0f, 10.0f)
            val pageGap = 10.0f

            val pageSize = (pageFormat.toPosition() - (margin * 2.0f) - Position(pageGap, 0f)) / Position(2.0f, 1.0f)
            val rows = 3
            val columns = 3
            val cardGap = Position(5.0f, 5.0f)
            val cardSize = Position((pageSize.x - cardGap.x * (columns - 1))/columns, (pageSize.y - cardGap.y * (rows - 1))/rows)

            val pageOffset = Position(pageSize.x + pageGap, 0f)
            val cardOffset = Position(cardSize.x + cardGap.x, cardSize.y + cardGap.y)


            fun position(index: Int): Position {
                val page = when(index) {
                    in 0 ..< 9 -> 0
                    in 9 ..< 18 -> 1
                    else -> throw IllegalArgumentException("index $index is not between 0 ..< 18")
                }

                val row = when(index - 9 * page) {
                    in 0 ..< 3 -> 0
                    in 3 ..< 6 -> 1
                    in 6 ..< 9 -> 2
                    else -> throw IllegalStateException()
                }

                val column = index - 9 * page - 3 * row
                require(column in 0 ..< 3)

                return margin +
                        pageOffset * page.toFloat() +
                        cardOffset * Position(column.toFloat(), rows - row.toFloat() - 1)
            }

            val blackListForSetSearch = listOf(
                "Plains",
                "Island",
                "Swamp",
                "Mountain",
                "Forest",
                "Command Tower",
                "Exotic Orchard",
                "Evolving Wilds",
                "Terramorphic Expanse",
                "Myriad Landscape",
                "Arcane Signet",
                "Sol Ring",
                "Commander's Sphere",
                "Mind Stone",
                "Fellwar Stone",
            )
            page(pageFormat) {
                drawText(deckName, fontTitle, HorizontalAlignment.CENTER, box.upperRightY-5f, fontColor)
                val entries = transaction {
                    cardNames
                        .map {
                            val tokens = it.split(Regex("\\s"), 2)
                            tokens[0].toInt() to tokens[1]
                        }
                        .sortedBy { it.second }
                        .filter { it.first > 0 }
                        .map { (amount, cardName) ->
                            val cardSets =
                                CardPossessions.innerJoin(Cards).innerJoin(ScryfallCardSets).innerJoin(CardSets)
                                    .slice(CardSets.id).select {
                                        (Cards.name match cardName)
                                    }.mapNotNull {
                                        CardSet.findById(it[CardSets.id])
                        }.groupingBy { it.shortName.toString().uppercase() }
                        .eachCount()
                            Triple(amount, cardName, cardSets)
                        }
                }
                sequenceOf(
                    entries.take(50) to 0f,
                    entries.drop(50) to box.width/2,
                ).forEach { (entries, x) ->
                    var line = 0
                    frame(20f + x, 20f, 20f, 20f) {
                        entries.take(50).forEach { (amount, cardName, cardSets) ->
                            val cardSetsString = if (cardName in blackListForSetSearch)
                                "[*]" else "[${cardSets.entries.sortedByDescending(Map.Entry<String, Int>::value).joinToString { if(it.value > 1 ) "${it.key}+" else it.key }}]"
                            drawText(
                                "$amount $cardName",
                                fontCard,
                                HorizontalAlignment.LEFT,
                                box.upperRightY - line * (3.0f + fontCard.height + fontCode.height),
                                fontColor
                            )
                            drawText(
                                "   $cardSetsString",
                                fontCode,
                                HorizontalAlignment.LEFT,
                                box.upperRightY - fontCard.height - line * (3.0f + fontCard.height + fontCode.height),
                                fontColor
                            )
                            line++
                        }
                    }
                }
            }

            save(file)
        }
    }
}

suspend fun main() {
    listOf(
    "Anim Pakal - Go Wide or Go Nome" to """
        1 Alpine Meadow
        1 Ancient Den
        1 Battlefield Forge
        1 Boros Garrison
        1 Captivating Cave
        1 Castle Embereth
        1 Clifftop Retreat
        1 Command Tower
        1 Forge of Heroes
        1 Furycalm Snarl
        1 Great Furnace
        1 Inspiring Vantage
        6 Mountain
        1 Needleverge Pathway // Pillarverge Pathway
        12 Plains
        1 Restless Bivouac
        1 Rugged Prairie
        1 Rustvale Bridge
        1 Sacred Foundry
        1 Sacred Peaks
        1 Sundown Pass
        1 Agitator Ant
        1 Andúril, Narsil Reforged
        1 Anim Pakal, Thousandth Moon
        1 Anointed Procession
        1 Arcane Signet
        1 Archaeomancer's Map
        1 Blade of the Bloodchief
        1 Boros Signet
        1 Cathars' Crusade
        1 Chaos Warp
        1 Citadel Siege
        1 Clever Concealment
        1 Coat of Arms
        1 Cyberman Patrol
        1 Deflecting Swat
        1 Extruder
        1 Felidar Retreat
        1 Flawless Maneuver
        1 Generous Gift
        1 Gingerbrute
        1 Goblin Bombardment
        1 Gold Myr
        1 Goldnight Commander
        1 Grateful Apparition
        1 Illustrious Wanderglyph
        1 Impact Tremors
        1 Ingenious Artillerist
        1 Iron Myr
        1 Jor Kadeen, the Prevailer
        1 Land Tax
        1 Loyal Apprentice
        1 Luminarch Aspirant
        1 Mikaeus, the Lunarch
        1 Mind Stone
        1 Mondrak, Glory Dominus
        1 Moonshaker Cavalry
        1 Neyali, Suns' Vanguard
        1 Noble Heritage
        1 Ojer Taq, Deepest Foundation // Temple of Civilization
        1 Ornithopter of Paradise
        1 Orzhov Advokist
        1 Path to Exile
        1 Purphoros, God of the Forge
        1 Reckless Fireweaver
        1 Rosie Cotton of South Lane
        1 Shared Animosity
        1 Skullclamp
        1 Sol Ring
        1 Steel Overseer
        1 Swords to Plowshares
        1 Talisman of Conviction
        1 Tarrian's Soulcleaver
        1 Teferi's Protection
        1 The Ozolith
        1 Thousand Moons Smithy // Barracks of the Thousand
        1 Throne of Geth
        1 Throne of the God-Pharaoh
        1 Thundering Raiju
        1 Unbreakable Formation
        1 Virtue of Loyalty // Ardenvale Fealty
        1 Warleader's Call
        1 Wayfarer's Bauble
        1 Witty Roastmaster
        Sideboard:

        1 Adeline, Resplendent Cathar
        1 Austere Command
        1 Basri Ket
        1 Blade Historian
        1 Blasphemous Act
        1 Fire Diamond
        1 Lightning Greaves
        1 Mana Crypt
        1 Marble Diamond
        1 Myriad Landscape
        1 Roar of Resistance
        1 Swiftfoot Boots
        1 The Millennium Calendar
        1 Threefold Thunderhulk
    """.trimIndent(),
    "Gale Infect" to """1 Castle Locthwain
1 Castle Vantress
1 Clearwater Pathway // Murkwater Pathway
1 Command Tower
1 Dimir Aqueduct
1 Drowned Catacomb
1 Escape Tunnel
1 Geier Reach Sanitarium
1 Inkmoth Nexus
6 Island
1 Karn's Bastion
1 Morphic Pool
1 Mystic Sanctuary
1 Peat Bog
1 Restless Reef
1 Rogue's Passage
1 Saprazzan Skerry
1 Shipwreck Marsh
7 Swamp
1 Tainted Isle
1 Temple of Deceit
1 Undercity Sewers
1 Underground River
1 Watery Grave
1 Anoint with Affliction
1 As Foretold
1 Astral Cornucopia
1 Bilious Skulldweller
1 Bitter Triumph
1 Blightbelly Rat
1 Blighted Agent
1 Bone Shards
1 Bring the Ending
1 Case of the Ransacked Lab
1 Consider
1 Contentious Plan
1 Deadly Rollick
1 Distorted Curiosity
1 Drown in Ichor
1 Everflowing Chalice
1 Experimental Augury
1 Feed the Swarm
1 Flux Channeler
1 Frantic Search
1 Fuel for the Cause
1 Gale, Waterdeep Prodigy
1 Geth's Summons
1 Gitaxian Probe
1 Glistening Sphere
1 God-Eternal Kefnet
1 Ichor Rats
1 Inexorable Tide
1 Infectious Inquiry
1 Midnight Clock
1 Murmuring Mystic
1 Myr Convert
1 Night's Whisper
1 Phyresis Outbreak
1 Plague Myr
1 Plague Stinger
1 Pongify
1 Primal Amulet // Primal Wellspring
1 Prologue to Phyresis
1 Ravenform
1 Reject Imperfection
1 Replicating Ring
1 Scion of Halaster
1 Search for Azcanta // Azcanta, the Sunken Ruin
1 Sedgemoor Witch
1 See the Truth
1 Serum Snare
1 Staff of Compleation
1 Steady Progress
1 Strategic Planning
1 Swan Song
1 Tainted Indulgence
1 Tekuthal, Inquiry Dominus
1 Tezzeret's Gambit
1 The Mirari Conjecture
1 Thought Scour
1 Vat Emergence
1 Viral Drake
1 Vivisurgeon's Insight
1 Voidwing Hybrid
1 Vraska, Betrayal's Sting
1 Vraska's Fall
1 Windfall
1 Wizards of Thay
1 Yawgmoth, Thran Physician
Sideboard:

1 Archmage Emeritus
1 Baral, Chief of Compliance
1 Corrupted Resolve
1 Counterspell
1 Curious Homunculus // Voracious Reader
1 Dreadship Reef
1 Empowered Autogenerator
1 Font of Magic
1 Founding the Third Path
1 Grim Affliction
1 Imprisoned in the Moon
1 Isochron Scepter
1 Jace's Sanctum
1 Lightning Greaves
1 Merchant Scroll
1 Mercurial Spelldancer
1 Mindsplice Apparatus
1 Mocking Sprite
1 Mystical Teachings
1 Mystical Tutor
1 Nephalia Drownyard
1 Ojer Pakpatiq, Deepest Epoch // Temple of Cyclical Time
1 Otherworldly Gaze
1 Pentad Prism
1 Pestilent Syphoner
1 Secrets of the Dead
1 Sign in Blood
1 Siphon Insight
1 Solve the Equation
1 Spread the Sickness
1 Swiftfoot Boots
1 Sword of Truth and Justice
1 Tocasia's Dig Site
1 Unmarked Grave
1 Venser, Corpse Puppet
1 Virulent Wound
1 Whisper of the Dross
""".trimIndent(),
    "Gale Poison" to """1 Castle Locthwain
1 Castle Vantress
1 Clearwater Pathway // Murkwater Pathway
1 Command Tower
1 Dimir Aqueduct
1 Drowned Catacomb
1 Escape Tunnel
1 Geier Reach Sanitarium
1 Inkmoth Nexus
6 Island
1 Karn's Bastion
1 Morphic Pool
1 Mystic Sanctuary
1 Peat Bog
1 Restless Reef
1 Rogue's Passage
1 Saprazzan Skerry
1 Shipwreck Marsh
7 Swamp
1 Tainted Isle
1 Temple of Deceit
1 Undercity Sewers
1 Underground River
1 Watery Grave
1 Anoint with Affliction
1 Archmage Emeritus
1 As Foretold
1 Astral Cornucopia
1 Bitter Triumph
1 Bone Shards
1 Bring the Ending
1 Case of the Ransacked Lab
1 Consider
1 Contentious Plan
1 Deadly Rollick
1 Distorted Curiosity
1 Drown in Ichor
1 Everflowing Chalice
1 Experimental Augury
1 Feed the Swarm
1 Flux Channeler
1 Frantic Search
1 Fuel for the Cause
1 Gale, Waterdeep Prodigy
1 Geth's Summons
1 Gitaxian Probe
1 Glistening Sphere
1 God-Eternal Kefnet
1 Grim Affliction
1 Inexorable Tide
1 Infectious Inquiry
1 Merchant Scroll
1 Midnight Clock
1 Murmuring Mystic
1 Mystical Teachings
1 Mystical Tutor
1 Night's Whisper
1 Pentad Prism
1 Phyresis Outbreak
1 Primal Amulet // Primal Wellspring
1 Prologue to Phyresis
1 Ravenform
1 Reject Imperfection
1 Replicating Ring
1 Scion of Halaster
1 Search for Azcanta // Azcanta, the Sunken Ruin
1 Sedgemoor Witch
1 See the Truth
1 Serum Snare
1 Solve the Equation
1 Spread the Sickness
1 Staff of Compleation
1 Steady Progress
1 Strategic Planning
1 Swan Song
1 Tainted Indulgence
1 Talrand, Sky Summoner
1 Tekuthal, Inquiry Dominus
1 Tezzeret's Gambit
1 The Mirari Conjecture
1 Thought Scour
1 Vat Emergence
1 Virulent Wound
1 Vivisurgeon's Insight
1 Vraska, Betrayal's Sting
1 Vraska's Fall
1 Windfall
1 Wizards of Thay
1 Yawgmoth, Thran Physician
Sideboard:

1 Baral, Chief of Compliance
1 Bilious Skulldweller
1 Blightbelly Rat
1 Blighted Agent
1 Corrupted Resolve
1 Counterspell
1 Curious Homunculus // Voracious Reader
1 Dreadship Reef
1 Empowered Autogenerator
1 Font of Magic
1 Founding the Third Path
1 Ichor Rats
1 Imprisoned in the Moon
1 Isochron Scepter
1 Jace's Sanctum
1 Lightning Greaves
1 Mercurial Spelldancer
1 Mindsplice Apparatus
1 Mocking Sprite
1 Myr Convert
1 Nephalia Drownyard
1 Ojer Pakpatiq, Deepest Epoch // Temple of Cyclical Time
1 Otherworldly Gaze
1 Pestilent Syphoner
1 Plague Myr
1 Plague Stinger
1 Pongify
1 Secrets of the Dead
1 Sign in Blood
1 Siphon Insight
1 Swiftfoot Boots
1 Sword of Truth and Justice
1 Tocasia's Dig Site
1 Unmarked Grave
1 Venser, Corpse Puppet
1 Viral Drake
1 Voidwing Hybrid
1 Whisper of the Dross
""".trimIndent(),
 "Delina - Let Chaos Ensue" to """1 Access Tunnel
1 Castle Embereth
1 Hidden Volcano
1 Maze of Ith
29 Mountain
1 Myriad Landscape
1 Rogue's Passage
1 War Room
1 Ancient Copper Dragon
1 Anger
1 Arcane Signet
1 Aspiring Champion
1 Audacious Swap
1 Barbarian Class
1 Blasphemous Act
1 Bloodthirster
1 Chaos Warp
1 Combat Celebrant
1 Combustible Gearhulk
1 Deflecting Swat
1 Delina, Wild Mage
1 Dire Fleet Daredevil
1 Dockside Extortionist
1 Dolmen Gate
1 Etali, Primal Storm
1 Faithless Looting
1 Fanatic of Mogis
1 Fire Diamond
1 Gamble
1 Goblin Bombardment
1 Goblin Chainwhirler
1 Goldspan Dragon
1 Guff Rewrites History
1 Harmonic Prodigy
1 Hazoret's Monument
1 Hellkite Tyrant
1 Helm of the Host
1 Impact Tremors
1 Jeska's Will
1 Kiki-Jiki, Mirror Breaker
1 Lightning Greaves
1 Meteor Golem
1 Mind Stone
1 Mindclaw Shaman
1 Ogre Battledriver
1 Orthion, Hero of Lavabrink
1 Panharmonicon
1 Plundering Barbarian
1 Port Razer
1 Professional Face-Breaker
1 Purphoros, God of the Forge
1 Rising of the Day
1 Roaming Throne
1 Ruby Medallion
1 Sol Ring
1 Solemn Simulacrum
1 Strionic Resonator
1 Sundial of the Infinite
1 Swiftfoot Boots
1 Terror of the Peaks
1 Thrill of Possibility
1 Tibalt's Trickery
1 Torbran, Thane of Red Fell
1 Valakut Awakening // Valakut Stoneforge
1 Vandalblast
1 Vexing Puzzlebox
1 Wand of Wonder
1 Warstorm Surge
1 Wyll, Blade of Frontiers
1 Wyll's Reversal
1 Zealous Conscripts
1 Zoyowa's Justice
Sideboard:

1 Abrade
1 Aggravated Assault
1 Arni Metalbrow
1 Burnished Hart
1 Charming Scoundrel
1 Fury of the Horde
1 Illusionist's Bracers
1 Imperial Recruiter
1 Karlach, Fury of Avernus
1 Moraug, Fury of Akoum
1 Nykthos, Shrine to Nyx
1 Relentless Assault
1 Seize the Day
1 Throne of Eldraine
1 Whispersilk Cloak
1 World at War
1 Zariel, Archduke of Avernus
    """.trimIndent(),
"Tazri - To The Best of My Ability" to """
        1 Cascading Cataracts
        1 City of Brass
        1 Command Tower
        1 Exotic Orchard
        5 Forest
        1 Indatha Triome
        5 Island
        1 Jetmir's Garden
        1 Ketria Triome
        2 Mountain
        3 Plains
        1 Raffine's Tower
        1 Raugrin Triome
        1 Savai Triome
        1 Spara's Headquarters
        2 Swamp
        1 Xander's Lounge
        1 Zagoth Triome
        1 Ziatora's Proving Ground
        1 Adric, Mathematical Genius
        1 Aftermath Analyst
        1 Agatha of the Vile Cauldron
        1 Agatha's Soul Cauldron
        1 Anguished Unmaking
        1 Arcane Signet
        1 Assassin's Trophy
        1 Azorius Guildmage
        1 Beast Within
        1 Biomancer's Familiar
        1 Bramble Familiar // Fetch Quest
        1 Brown Ouphe
        1 Captivating Crew
        1 Chainer, Nightmare Adept
        1 Chulane, Teller of Tales
        1 Crackleburr
        1 Dauntless Dismantler
        1 Dawntreader Elk
        1 Deathrite Shaman
        1 Drumbellower
        1 Duskmantle Guildmage
        1 Dynaheir, Invoker Adept
        1 Experiment Kraj
        1 Faerie Formation
        1 Farseek
        1 Freed from the Real
        1 General's Enforcer
        1 Generous Gift
        1 Gretchen Titchwillow
        1 Havengul Lich
        1 Heartstone
        1 Incubation Druid
        1 Ioreth of the Healing House
        1 Jegantha, the Wellspring
        1 Kinnan, Bonder Prodigy
        1 Kiora's Follower
        1 Koma, Cosmos Serpent
        1 Krosan Grip
        1 Leech Bonder
        1 Leyline of Abundance
        1 Likeness Looter
        1 Nature's Lore
        1 Nicol Bolas, the Ravager // Nicol Bolas, the Arisen
        1 Omen Hawker
        1 Order of Whiteclay
        1 Patchwork Crawler
        1 Patriar's Seal
        1 Patrol Signaler
        1 Queen Kayla bin-Kroog
        1 Quicksilver Elemental
        1 Rhythm of the Wild
        1 Robaran Mercenaries
        1 Rona, Herald of Invasion // Rona, Tolarian Obliterator
        1 Sakura-Tribe Elder
        1 Samut, Voice of Dissent
        1 Sol Ring
        1 Spectral Sailor
        1 Svella, Ice Shaper
        1 Syr Konrad, the Grim
        1 Tazri, Stalwart Survivor
        1 The Ancient One
        1 The Enigma Jewel // Locus of Enlightenment
        1 Thousand-Year Elixir
        1 Thrasios, Triton Hero
        1 Training Grounds
        1 Triskaidekaphile
        1 Xantcha, Sleeper Agent
        1 Xathrid Gorgon
        1 Zacama, Primal Calamity
        Sideboard:

        1 Ayesha Tanaka
        1 Bloom Tender
        1 Drana and Linvala
        1 Myr Welder
        1 Necrotic Ooze
        1 Seedborn Muse
        1 The Peregrine Dynamo
        1 Vexing Shusher
        1 Wild Cantor
        1 Zirda, the Dawnwaker
    """.trimIndent(),
        "Codie, Vociferous Codex - Cycle of Living End" to  """
        1 Alpine Meadow
        1 Arctic Treeline
        1 Ash Barrens
        1 Barren Moor
        1 Blasted Landscape
        1 Bojuka Bog
        1 Canyon Slough
        1 Command Tower
        1 Drifting Meadow
        1 Exotic Orchard
        1 Fetid Pools
        1 Forest
        1 Forgotten Cave
        1 Glacial Floodplain
        1 Ice Tunnel
        1 Irrigated Farmland
        1 Island
        1 Lonely Sandbar
        1 Molten Tributary
        1 Mountain
        1 Plains
        1 Polluted Mire
        1 Remote Isle
        1 Rimewood Falls
        1 Scattered Groves
        1 Secluded Steppe
        1 Sheltered Thicket
        1 Slippery Karst
        1 Smoldering Crater
        1 Snowfield Sinkhole
        1 Swamp
        1 Tangled Islet
        1 Woodland Chasm
        1 Alabaster Host Intercessor
        1 Ancient Excavation
        1 Angel of the Ruins
        1 Arcane Signet
        1 Archfiend of Ifnir
        1 Autumn's Veil
        1 Brainstorm
        1 Bring to Light
        1 Codie, Vociferous Codex
        1 Coffin Purge
        1 Colossal Skyturtle
        1 Consider
        1 Crawl from the Cellar
        1 Curator of Mysteries
        1 Deadshot Minotaur
        1 Dispel
        1 Dizzy Spell
        1 Drannith Stinger
        1 Dream Cache
        1 Eagles of the North
        1 Faerie Macabre
        1 Faithless Looting
        1 Fellwar Stone
        1 Finale of Promise
        1 Fluctuator
        1 Generous Ent
        1 Glassdust Hulk
        1 Gloomfang Mauler
        1 Greater Sandwurm
        1 Greater Tanuki
        1 Hollow One
        1 Horror of the Broken Lands
        1 Imposing Vantasaur
        1 Krosan Tusker
        1 Lat-Nam's Legacy
        1 Living End
        1 Mind Stone
        1 Mirrorshell Crab
        1 Monstrosity of the Lake
        1 Nature's Claim
        1 Nimble Obstructionist
        1 Oliphaunt
        1 Opt
        1 Pale Recluse
        1 Quicken
        1 Rampaging War Mammoth
        1 Rooting Moloch
        1 Ruin Grinder
        1 Scion of Darkness
        1 See Beyond
        1 Serene Remembrance
        1 Shefet Monitor
        1 Shigeki, Jukai Visionary
        1 Sol Ring
        1 Stream of Thought
        1 Street Wraith
        1 Sunblade Samurai
        1 The Balrog of Moria
        1 Tidal Terror
        1 Titanoth Rex
        1 Topiary Panther
        1 Troll of Khazad-dûm
        1 Turn the Earth
        1 Valiant Rescuer
        1 Waker of Waves
        1 Wayfarer's Bauble
        1 Windcaller Aven
        Sideboard:

        1 Anger
        1 Bone Shards
        1 Haunted Mire
        1 Idyllic Beachfront
        1 Restless Dreams
        1 Sacred Peaks
        1 Striped Riverwinder
        1 Sunlit Marsh
        1 Tranquil Thicket
    """.trimIndent(),
        "Rakdos, Lord of Riots - No Pain, No Game" to """
            1 Bojuka Bog
            1 Castle Locthwain
            1 Command Tower
            1 Dragonskull Summit
            1 Eldrazi Temple
            1 Evolving Wilds
            1 Exotic Orchard
            1 Fabled Passage
            1 Foreboding Ruins
            1 Graven Cairns
            1 Leechridden Swamp
            1 Mount Doom
            4 Mountain
            1 Myriad Landscape
            1 Prismatic Vista
            1 Rakdos Carnarium
            1 Shadowblood Ridge
            1 Shivan Gorge
            1 Shrine of the Forsaken Gods
            1 Smoldering Marsh
            1 Spinerock Knoll
            1 Sulfurous Springs
            4 Swamp
            1 Temple of Malice
            1 Terramorphic Expanse
            1 War Room
            1 Wastes
            1 Acidic Soil
            1 All Is Dust
            1 Ancient Stone Idol
            1 Arcane Signet
            1 Artisan of Kozilek
            1 Baleful Mastery
            1 Bane of Bala Ged
            1 Black Market Connections
            1 Blasphemous Act
            1 Blightsteel Colossus
            1 Blood for the Blood God!
            1 Burnished Hart
            1 Chandra's Ignition
            1 Chaos Warp
            1 Charcoal Diamond
            1 Commander's Sphere
            1 Conduit of Ruin
            1 Court of Ambition
            1 Crushing Disappointment
            1 Cryptolith Fragment // Aurora of Emrakul
            1 Deflecting Swat
            1 Descent into Avernus
            1 Earthquake
            1 Fellwar Stone
            1 Fire Diamond
            1 Flame Rift
            1 Flayer of Loyalties
            1 Ill-Gotten Inheritance
            1 It That Betrays
            1 Keen Duelist
            1 Kozilek, Butcher of Truth
            1 Kozilek, the Great Distortion
            1 Liberator, Urza's Battlethopter
            1 Light Up the Stage
            1 Lightning Bolt
            1 Lightning Greaves
            1 Mind Stone
            1 Nettle Drone
            1 Night's Whisper
            1 Palace Siege
            1 Pathrazer of Ulamog
            1 Pestilence
            1 Price of Progress
            1 Prisoner's Dilemma
            1 Rakdos Charm
            1 Rakdos Signet
            1 Rakdos, Lord of Riots
            1 Retreat to Hagra
            1 Rising of the Day
            1 Risk Factor
            1 Sarkhan's Unsealing
            1 Sign in Blood
            1 Sol Ring
            1 Solemn Simulacrum
            1 Spawn of Mayhem
            1 Stormfist Crusader
            1 Talisman of Indulgence
            1 Terminate
            1 Theater of Horrors
            1 Tibalt's Trickery
            1 Ulamog, the Infinite Gyre
            1 Ulamog's Crusher
            1 Unlicensed Disintegration
            1 Walking Ballista
            1 Wandering Archaic // Explore the Vastlands
            1 Warstorm Surge
            1 Wayfarer's Bauble
            Sideboard:

            1 Faithless Looting
            1 Plague Spitter
            1 Vedalken Orrery
            """.trimIndent(),
        "Rakdos, Lord of Riots - No Pain, No Game (Low Salt)" to """
            1 Blood Crypt
            1 Bojuka Bog
            1 Castle Locthwain
            1 Command Tower
            1 Dragonskull Summit
            1 Evolving Wilds
            1 Exotic Orchard
            1 Fabled Passage
            1 Foreboding Ruins
            1 Graven Cairns
            1 Leechridden Swamp
            1 Mount Doom
            5 Mountain
            1 Myriad Landscape
            1 Prismatic Vista
            1 Rakdos Carnarium
            1 Shadowblood Ridge
            1 Shivan Gorge
            1 Shrine of the Forsaken Gods
            1 Smoldering Marsh
            1 Spinerock Knoll
            1 Sulfurous Springs
            4 Swamp
            1 Temple of Malice
            1 Terramorphic Expanse
            1 War Room
            1 Acidic Soil
            1 Ancient Stone Idol
            1 Arcane Signet
            1 Artisan of Kozilek
            1 Baleful Mastery
            1 Bane of Bala Ged
            1 Black Market Connections
            1 Blasphemous Act
            1 Blightsteel Colossus
            1 Blood for the Blood God!
            1 Burnished Hart
            1 Chain Lightning
            1 Chandra's Ignition
            1 Chaos Warp
            1 Charcoal Diamond
            1 Cityscape Leveler
            1 Commander's Sphere
            1 Court of Ambition
            1 Crushing Disappointment
            1 Cryptolith Fragment // Aurora of Emrakul
            1 Cybermen Squadron
            1 Deflecting Swat
            1 Descent into Avernus
            1 Duplicant
            1 Earthquake
            1 Fellwar Stone
            1 Fire Diamond
            1 Flame Rift
            1 Ill-Gotten Inheritance
            1 Keen Duelist
            1 Liberator, Urza's Battlethopter
            1 Light Up the Stage
            1 Lightning Greaves
            1 Maelstrom Colossus
            1 Meteor Golem
            1 Mind Stone
            1 Nettle Drone
            1 Night's Whisper
            1 Palace Siege
            1 Pathrazer of Ulamog
            1 Pestilence
            1 Price of Progress
            1 Prisoner's Dilemma
            1 Rakdos Charm
            1 Rakdos Signet
            1 Rakdos, Lord of Riots
            1 Retreat to Hagra
            1 Rising of the Day
            1 Risk Factor
            1 Sarkhan's Unsealing
            1 Sign in Blood
            1 Sol Ring
            1 Solemn Simulacrum
            1 Spawn of Mayhem
            1 Steel Hellkite
            1 Stormfist Crusader
            1 Talisman of Indulgence
            1 Terminate
            1 Theater of Horrors
            1 Thopter Assembly
            1 Tibalt's Trickery
            1 Ulamog's Crusher
            1 Unlicensed Disintegration
            1 Vilis, Broker of Blood
            1 Walking Ballista
            1 Wandering Archaic // Explore the Vastlands
            1 Warstorm Surge
            Sideboard:

            1 Phyrexian Triniform
            1 Plague Spitter
            1 Skitterbeam Battalion
            1 Skittering Cicada
            1 Teeka's Dragon
            1 Terror Ballista
            1 Threefold Thunderhulk
            1 Wurmcoil Engine
            1 Zof Consumption // Zof Bloodbog
        """.trimIndent(),
        "Kenrith - King of Hearts" to """
            1 Azorius Guildgate
            1 Baldur's Gate
            1 Basilisk Gate
            1 Black Dragon Gate
            1 Boros Guildgate
            1 Citadel Gate
            1 Cliffgate
            1 Dimir Guildgate
            1 Forbidden Orchard
            4 Forest
            1 Gateway Plaza
            1 Golgari Guildgate
            1 Gond Gate
            1 Gruul Guildgate
            1 Heap Gate
            1 Homeward Path
            1 Island
            1 Izzet Guildgate
            1 Manor Gate
            1 Maze of Ith
            1 Maze's End
            1 Mountain
            1 Orzhov Guildgate
            2 Plains
            1 Plaza of Harmony
            1 Rakdos Guildgate
            1 Rogue's Passage
            1 Sea Gate
            1 Selesnya Guildgate
            1 Simic Guildgate
            1 Swamp
            1 The Black Gate
            1 Thran Portal
            1 Allure of the Unknown
            1 Arcane Signet
            1 Baleful Mastery
            1 Benevolent Offering
            1 Benthic Explorers
            1 Bigger on the Inside
            1 Bramble Sovereign
            1 Circuitous Route
            1 Círdan the Shipwright
            1 Creative Technique
            1 Crop Rotation
            1 Cultivate
            1 Discerning Financier
            1 Divine Gambit
            1 Dubious Challenge
            1 Eon Frolicker
            1 Excavation Technique
            1 Explore the Underdark
            1 Farseek
            1 Flumph
            1 Forcemage Advocate
            1 Generous Gift
            1 Healing Technique
            1 Heartstone
            1 Incarnation Technique
            1 Infernal Offering
            1 Ingenious Mastery
            1 Intellectual Offering
            1 Keen Duelist
            1 Kenrith, the Returned King
            1 Kros, Defense Contractor
            1 Loran of the Third Path
            1 Metamorphose
            1 Nature's Lore
            1 Nullmage Advocate
            1 Pendant of Prosperity
            1 Pulsemage Advocate
            1 Rampant Growth
            1 Replication Technique
            1 Rootweaver Druid
            1 Scheming Symmetry
            1 Secret Rendezvous
            1 Seedborn Muse
            1 Sheltering Ancient
            1 Shieldmage Advocate
            1 Skullwinder
            1 Sol Ring
            1 Soldevi Golem
            1 Spectral Searchlight
            1 Spore Frog
            1 Spurnmage Advocate
            1 Sudden Salvation
            1 Tempt with Discovery
            1 Tenuous Truce
            1 The Twelfth Doctor
            1 The Wedding of River Song
            1 Training Grounds
            1 Verdant Mastery
            1 Victory Chimes
            1 Wedding Ring
            1 Willbreaker
            1 Your Temple Is Under Attack
            1 Zirda, the Dawnwaker
            Sideboard:

            1 Cowardice
            1 Dismiss into Dream
            1 Kodama's Reach
            1 Open the Gates
            1 Shadrix Silverquill
            1 Wilderness Reclamation
        """.trimIndent(),
        "Kalamax - Spellosaurus Rex" to """
1 Blighted Woodland
1 Command Tower
1 Evolving Wilds
1 Exotic Orchard
1 Fabled Passage
7 Forest
1 Gruul Turf
1 Holdout Settlement
7 Island
1 Izzet Boilerworks
1 Mosswort Bridge
6 Mountain
1 Myriad Landscape
1 Mystic Sanctuary
1 Scene of the Crime
1 Simic Growth Chamber
1 Survivors' Encampment
1 Terramorphic Expanse
1 An Offer You Can't Refuse
1 Archdruid's Charm
1 Archmage Emeritus
1 Beast Within
1 Big Score
1 Case of the Ransacked Lab
1 Channeled Force
1 Chaos Warp
1 Chemister's Insight
1 Comet Storm
1 Commune with Lava
1 Crop Rotation
1 Cryptolith Rite
1 Cultivator's Caravan
1 Deflecting Swat
1 Electrodominance
1 Entish Restoration
1 Expansion // Explosion
1 Fierce Guardianship
1 Fling
1 Frantic Search
1 Goblin Electromancer
1 Growth Spiral
1 Harrow
1 Honor-Worn Shaku
1 Jace's Sanctum
1 Kalamax, the Stormsire
1 Kazuul's Fury // Kazuul's Cliffs
1 Khalni Ambush // Khalni Territory
1 Krosan Grip
1 Lightning Greaves
1 Lithoform Engine
1 Manamorphose
1 Melek, Izzet Paragon
1 Melek, Reforged Researcher
1 Murmuring Mystic
1 Narset's Reversal
1 Natural Connection
1 Nexus of Fate
1 Ojer Pakpatiq, Deepest Epoch // Temple of Cyclical Time
1 Paradise Mantle
1 Primal Amulet // Primal Wellspring
1 Prophetic Bolt
1 Quick Study
1 Ral, Storm Conduit
1 Rashmi, Eternities Crafter
1 Relic of Legends
1 Roiling Regrowth
1 Silundi Vision // Silundi Isle
1 Slice in Twain
1 Sol Ring
1 Soul's Fire
1 Springleaf Drum
1 Sublime Epiphany
1 Swarm Intelligence
1 Talrand, Sky Summoner
1 Temur Charm
1 Thousand-Year Storm
1 Thrill of Possibility
1 Twinning Staff
1 Unexpected Windfall
1 Valakut Awakening // Valakut Stoneforge
1 Veyran, Voice of Duality
1 Whiplash Trap
1 Wilderness Reclamation
Sideboard:

1 Arcane Signet
1 Artifact Mutation
1 Baral, Chief of Compliance
1 Birgi, God of Storytelling // Harnfel, Horn of Bounty
1 Charmbreaker Devils
1 Chord of Calling
1 Cinder Glade
1 Clash of Titans
1 Crackling Drake
1 Curious Herd
1 Cyclonic Rift
1 Deadeye Navigator
1 Decoy Gambit
1 Desolate Lighthouse
1 Djinn Illuminatus
1 Dualcaster Mage
1 Eon Frolicker
1 Etali, Primal Storm
1 Evolution Charm
1 Flame Sweep
1 Frontier Bivouac
1 Glademuse
1 Goblin Dark-Dwellers
1 Haldan, Avid Arcanist
1 Hunter's Insight
1 Hunting Pack
1 Ketria Triome
1 Legolas's Quick Reflexes
1 March of Reckless Joy
1 Mossfire Valley
1 Naru Meha, Master Wizard
1 Nascent Metamorph
1 Niblis of Frost
1 Pako, Arcane Retriever
1 Price of Progress
1 Pyromancer's Goggles
1 Ravenous Gigantotherium
1 Reality Shift
1 Scavenger Grounds
1 Solemn Simulacrum
1 Starstorm
1 Steam Vents
1 Strength of the Tajuru
1 Surreal Memoir
1 Tribute to the Wild
1 Underworld Breach
1 Wort, the Raidmother
1 Xyris, the Writhing Storm
1 Yavimaya Coast
        """.trimIndent(),
        "Blaster, Combat DJ - Final Counter" to """
1 Ancient Tomb
1 Blinkmoth Nexus
12 Forest
1 Inkmoth Nexus
1 Karn's Bastion
1 Llanowar Reborn
8 Mountain
1 Nesting Grounds
1 Power Depot
1 Ruins of Oran-Rief
1 Shrine of the Forsaken Gods
1 Temple of the False God
1 Urza's Mine
1 Urza's Power Plant
1 Urza's Tower
1 Urza's Workshop
1 Yavimaya, Cradle of Growth
1 All Will Be One
1 Arcane Signet
1 Arcbound Overseer
1 Arcbound Ravager
1 Arcbound Reclaimer
1 Arcbound Stinger
1 Archdruid's Charm
1 Ashnod's Altar
1 Beast Within
1 Blaster, Combat DJ // Blaster, Morale Booster
1 Branching Evolution
1 Burnished Hart
1 Cauldron of Souls
1 Chaos Warp
1 Court of Garenbrig
1 Cradle Clearcutter
1 Crop Rotation
1 Crystalline Crawler
1 Expedition Map
1 Extruder
1 Foundry Inspector
1 Fractal Harness
1 Garruk's Uprising
1 Greater Good
1 Hardened Scales
1 Haywire Mite
1 Invigorating Hot Spring
1 Iron Apprentice
1 Jhoira's Familiar
1 Kami of Whispered Hopes
1 Kazuul's Fury // Kazuul's Cliffs
1 Krenko's Buzzcrusher
1 Liberator, Urza's Battlethopter
1 Lifecrafter's Bestiary
1 Mindless Automaton
1 Myr Retriever
1 Ornithopter of Paradise
1 Ozolith, the Shattered Spire
1 Patchwork Automaton
1 Pir's Whim
1 Reap and Sow
1 Rishkar, Peema Renegade
1 Scrap Trawler
1 Scrapyard Recombiner
1 Shimmer Myr
1 Simian Simulacrum
1 Smell Fear
1 Sol Ring
1 Solemn Simulacrum
1 Solidarity of Heroes
1 Steel Overseer
1 Stonecoil Serpent
1 Syr Ginger, the Meal Ender
1 Tanuki Transplanter
1 Tarrian's Soulcleaver
1 Tempt with Discovery
1 The Great Henge
1 The Ozolith
1 Throne of Geth
1 Towashi Guide-Bot
1 Trading Post
1 Triskelion
1 Verdurous Gearhulk
1 Walking Ballista
1 Workshop Assistant
Sideboard:

1 Bone Sabres
1 Contagion Clasp
1 Cultivate
1 Cybermen Squadron
1 Fling
1 Goblin Bombardment
1 Goblin Engineer
1 Goblin Welder
1 Guardian Project
1 Hoard Hauler
1 Invigorating Surge
1 Junk Diver
1 Kodama's Reach
1 Krark-Clan Ironworks
1 Lizard Blades
1 Mechtitan Core
1 Palladium Myr
1 Tarrian's Journal // The Tomb of Aclazotz
2 Weatherlight Compleated
        """.trimIndent(),
        "Yarus - Face Off" to """
            1 Blighted Woodland
            1 Branch of Vitu-Ghazi
            1 Cinder Glade
            1 Command Tower
            13 Forest
            1 Game Trail
            1 Gruul Turf
            1 Kessig Wolf Run
            1 Mossfire Valley
            6 Mountain
            1 Myriad Landscape
            1 Rockfall Vale
            1 Rootbound Crag
            1 Sheltered Thicket
            1 Shrine of the Forsaken Gods
            1 Spire Garden
            1 Stomping Ground
            1 Temple of Abandon
            1 Wooded Ridgeline
            1 Zoetic Cavern
            1 Ainok Survivalist
            1 Akroma, Angel of Fury
            1 Arcane Signet
            1 Ashcloud Phoenix
            1 Ashnod's Altar
            1 Beast Whisperer
            1 Beast Within
            1 Blasphemous Act
            1 Bolrac-Clan Basher
            1 Boltbender
            1 Broodhatch Nantuko
            1 Chaos Warp
            1 Cloud Key
            1 Deathmist Raptor
            1 Den Protector
            1 Dream Chisel
            1 Evolutionary Leap
            1 Experiment Twelve
            1 Expose the Culprit
            1 Farseek
            1 Flourishing Bloom-Kin
            1 Fortune Thief
            1 Goblin Bombardment
            1 Goblin Maskmaker
            1 Greenbelt Radical
            1 Guardian Project
            1 Hide in Plain Sight
            1 Hooded Hydra
            1 Jeering Instigator
            1 Krosan Cloudscraper
            1 Krosan Colossus
            1 Lightning Greaves
            1 Nantuko Vigilante
            1 Nature's Lore
            1 Nervous Gardener
            1 Nylea, Keen-Eyed
            1 Obscuring Aether
            1 Ohran Frostfang
            1 Panoptic Projektor
            1 Primal Whisperer
            1 Printlifter Ooze
            1 Pyrotechnic Performer
            1 Rampant Growth
            1 Rhythm of the Wild
            1 Riftburst Hellion
            1 Root Elemental
            1 Salt Road Ambushers
            1 Scroll of Fate
            1 Showstopping Surprise
            1 Sol Ring
            1 Temur Sabertooth
            1 Temur War Shaman
            1 Thelonite Hermit
            1 Three Visits
            1 Trail of Mystery
            1 Tunnel Tipster
            1 Ugin, the Ineffable
            1 Ugin's Mastery
            1 Vengeful Creeper
            1 Whisperwood Elemental
            1 Wildcall
            1 Yarus, Roar of the Old Gods
            1 Yedora, Grave Gardener
            Sideboard:

            1 Cultivate
            1 Forsaken Monument
            1 Gruul Signet
            1 Impact Tremors
            1 Karplusan Forest
            1 Omarthis, Ghostfire Initiate
            1 Temur Charger
            1 Tin Street Gossip
            1 Toski, Bearer of Secrets
        """.trimIndent(),
        "Baeloth Barrityl, Entertainer - Let You Entertain Me" to """
            1 Ash Barrens
            1 Battlefield Forge
            1 Command Tower
            1 Desert
            1 Evolving Wilds
            1 Exotic Orchard
            1 Furycalm Snarl
            1 Lorehold Campus
            10 Mountain
            1 Myriad Landscape
            9 Plains
            1 Rugged Prairie
            1 Rustvale Bridge
            1 Slayers' Stronghold
            1 Spinerock Knoll
            1 Temple of Triumph
            1 Terramorphic Expanse
            1 Agitator Ant
            1 Akki Battle Squad
            1 All That Glitters
            1 Arcane Signet
            1 Archon of Coronation
            1 Baeloth Barrityl, Entertainer
            1 Bastion Protector
            1 Blackblade Reforged
            1 Blasphemous Act
            1 Boros Charm
            1 Boros Signet
            1 Brigid, Hero of Kinsbaile
            1 Citadel Siege
            1 Collective Effort
            1 Combat Calligrapher
            1 Commander's Sphere
            1 Contraband Livestock
            1 Coveted Jewel
            1 Death Kiss
            1 Disenchant
            1 Don't Move
            1 Dragon Mantle
            1 Dragonmaster Outcast
            1 Duelist's Heritage
            1 Emberwilde Captain
            1 Fateful Absence
            1 Fire Diamond
            1 Fists of Flame
            1 Flame Sweep
            1 Fling
            1 Frontier Warmonger
            1 Ghirapur Aether Grid
            1 Glittering Stockpile
            1 Goro-Goro, Disciple of Ryusei
            1 Havoc Jester
            1 Hopeful Initiate
            1 Kazuul's Fury // Kazuul's Cliffs
            1 Life of the Party
            1 Loxodon Warhammer
            1 Marble Diamond
            1 Mind Stone
            1 Mirror Shield
            1 Nils, Discipline Enforcer
            1 Noble Heritage
            1 Orzhov Advokist
            1 Outpost Siege
            1 Powerstone Minefield
            1 Professional Face-Breaker
            1 Quicksmith Genius
            1 Rain of Riches
            1 Return to Dust
            1 Sejiri Shelter // Sejiri Glacier
            1 Shatter the Sky
            1 Shiny Impetus
            1 Showdown of the Skalds
            1 Smuggler's Share
            1 Sol Ring
            1 Sun Titan
            1 Sunforger
            1 Swiftfoot Boots
            1 Talisman of Conviction
            1 Temur Battle Rage
            1 Together Forever
            1 Valorous Stance
            1 Wayfarer's Bauble
            1 Wild Ricochet
            Sideboard:

            1 Aurelia, the Law Above
            1 Breya's Apprentice
            1 Court of Ardenvale
            1 Crescendo of War
            1 Inspired Tinkering
            1 Mass Hysteria
            1 Vicious Shadows
            1 Felidar Retreat
        """.trimIndent()
    ).forEach { deck ->
        DeckBuildingListPrinter().printList(
            deck.first,
            deck.second.lines().takeWhile { it != "Sideboard:" },
            "C:\\Users\\001121673\\private\\${deck.first}.pdf"
        )
    }
}
