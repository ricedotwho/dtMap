package com.ricedotwho.dtmap.features.map

import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.DtMap.scope
import com.ricedotwho.dtmap.config.C1Map
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.events.LocationEvents
import com.ricedotwho.dtmap.features.SoloClear
import com.ricedotwho.dtmap.features.map.DungeonMap.mapCenter
import com.ricedotwho.dtmap.features.map.DungeonMap.roomSize
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Web
import com.ricedotwho.dtmap.utils.romanToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.util.Tuple
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.PlayerSkin
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil
import kotlin.math.floor

/*
 * Original code Copyright (c) 2026, odtheking (https://github.com/odtheking/Odin/blob/main/src/main/kotlin/com/odtheking/odin/utils/skyblock/dungeon/DungeonUtils.kt, https://github.com/odtheking/Odin/blob/main/src/main/kotlin/com/odtheking/odin/utils/skyblock/dungeon/DungeonListener.kt)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

// thanks odin.
object Scoreboard {
    enum class Floor(val secretFactor: Float = 1f) {
        Unknown,
        Entrance(0.3f),
        F1(0.3f), F2(0.4f), F3(0.5f), F4(0.6f), F5(0.7f), F6(0.85f), F7,
        M1, M2, M3, M4, M5, M6, M7;

        val number: Int = when (name) {
            "Unknown" -> -1
            "Entrance" -> 0
            else -> name.lastOrNull()?.digitToInt() ?: -1
        }

        companion object {
            fun fromString(str: String): Floor =
                if (str == "E") Entrance
                else entries.find { it.name == str } ?: Unknown
        }
    }

    data class DungeonPlayer(
        val name: String,
        var clazz: DungeonClass,
        var clazzLvl: Int,
        var skin: PlayerSkin? = null,
        var entity: Player? = null,
        var mapPos: Vec2i = Vec2i(0, 0),
        var yaw: Float = 0.0f,
        var isDead: Boolean = false,
        var deaths: Int = 0
    ) {
        fun mapRenderPosition(): Tuple<Float, Float> =
            if (entity != null) {
                val x = (entity!!.x + 201.0) / (32.0 / 20.0)
                val z = (entity!!.z + 201.0) / (32.0 / 20.0)
                Tuple(x.toFloat(), z.toFloat())
            } else if (roomSize != null) {
                val offset = this.mapPos.multiply(32.0 / (((roomSize!! + 4.0) * 2)))
                val pos = mapCenter!!.add(offset).add(Vec2i(201, 201)).divide(32.0 / 20.0)
                Tuple(pos.x.toFloat(), pos.z.toFloat())
            } else {
                Tuple(0f, 0f)
            }

        fun mapRenderYaw(): Float =
            if (entity != null) entity!!.yRot else yaw
    }

    enum class DungeonClass {
        Archer,
        Berserk,
        Healer,
        Mage,
        Tank,
        Unknown
    }

    data class TabData(
        var secretsFound: Int = 0,
        var secretsPercent: Float = 0f,
        var crypts: Int = 0,
        var openedRooms: Int = 0,
        var completedRooms: Int = 0,
        var deaths: Int = 0,
        var percentCleared: Int = 0,
        var elapsedTime: String = "0s",
        var mimicKilled: Boolean = false,
        var princeKilled: Boolean = false,
        var bats: MutableSet<String> = mutableSetOf(),
        var puzzleCount: Int = 0,
        var puzzles: MutableList<Puzzle> = mutableListOf()
    ) {
        fun isBloodDone(): Boolean =
            (Scan.blood != null && Scan.blood!!.state == Room.State.GREEN)

        fun calculateTotalSecrets(): Int =
            if (Scan.loadedAllRooms && SoloClear.hack(!C1Map.legitMode)) Scan.allSecrets
            else if (secretsFound == 0 || secretsPercent == 0f) 0
            else floor(100 / secretsPercent * secretsFound + 0.5).toInt()

        fun calculateMaxBonusScore(): Int {
            val princeMaxScore = if (SoloClear.legit(C1Map.legitMode)) Prince.legitPrince else Prince.cheaterPrince
            return 5 + (if (princeMaxScore) 1 else 0) + if (floor.number >= 6) 2 else 0
        }

        fun calculateTotalRooms(): Int =
            if (completedRooms == 0 || percentCleared == 0) 0 else floor((completedRooms / (percentCleared * 0.01).toFloat()) + 0.4).toInt()

        fun calculateScore(): Int {
            val totalRooms = calculateTotalRooms()
            val completed = completedRooms + (if (!isBloodDone()) 1 else 0) + if (!DungeonMessages.inBoss) 1 else 0
            val total = if (totalRooms != 0) totalRooms else 36

            val totalSecrets = if (secretsFound == 0 || secretsPercent == 0f) 0 else floor(100 / secretsPercent * secretsFound + 0.5).toInt()

            val exploration = floor.let {
                val secretScore = if (totalSecrets > 0) {
                    floor(secretsFound.toDouble() / (totalSecrets.toDouble() * it.secretFactor) * 40.0)
                        .toInt().coerceIn(0, 40)
                } else 0

                secretScore + floor(completed.toFloat() / total * 60f).coerceIn(0f, 60f).toInt()
            }

            val skillRooms = floor(completed.toFloat() / total * 80f).coerceIn(0f, 80f).toInt()
            val puzzlePenalty = (puzzleCount - puzzles.count { it.status == PuzzleStatus.Completed }) * 10

            return exploration + (20 + skillRooms - puzzlePenalty - (deaths * 2 - 1).coerceAtLeast(0)).coerceIn(20, 100) + 100 + calculateBonusScore()
        }

        fun calculateBonusScoreNoPaul(): Int =
            (if (mimicKilled) 2 else 0) + (if (princeKilled) 1 else 0) + bats.size.coerceAtMost(5) + crypts.coerceAtMost(5)

        fun calculateBonusScore(): Int =
             calculateBonusScoreNoPaul() + calculatePaulScore()

        fun calculatePaulScore(): Int =
            if (((C1Map.scorePaul == 0 && paul) || C1Map.scorePaul == 1) && !SoloClear.isSoloClearing()) 10 else 0

        fun calculateMinimumSecrets(forceNoMaxBonus: Boolean = false, forceNeeded: Boolean = false): Int {
            val bonus = (if (!forceNoMaxBonus && C1Map.scoreMaxBonusForMissingSecrets) calculateMaxBonusScore() else calculateBonusScoreNoPaul()) + if (C1Map.scoreMissingSecretsCalculatePaul) calculatePaulScore() else 0
            val found = if (!forceNeeded && C1Map.scoreNeededSecretsInsteadOfMissing) 0 else secretsFound
            return (ceil(
                calculateTotalSecrets() * floor.secretFactor * (40 - bonus + (deaths * 2 - 1).coerceAtLeast(0)) / 40f
            ).toInt() - found).coerceAtLeast(0)
        }
    }

    enum class Puzzle(
        val displayName: String,
        var status: PuzzleStatus = PuzzleStatus.Incomplete
    ) {
        UNKNOWN("???"),
        BLAZE("Higher Or Lower"),
        BEAMS("Creeper Beams"),
        WEIRDOS("Three Weirdos"),
        TTT("Tic Tac Toe"),
        WATER_BOARD("Water Board"),
        TP_MAZE("Teleport Maze"),
        BOULDER("Boulder"),
        ICE_FILL("Ice Fill"),
        ICE_PATH("Ice Path"),
        QUIZ("Quiz");
        // BOMB_DEFUSE("Bomb Defuse"); -- rip :(
    }

    enum class PuzzleStatus {
        Completed,
        Failed,
        Incomplete
    }

    var dungeonTeammates: ArrayList<DungeonPlayer> = ArrayList(5)
    var dungeonTeammatesNoSelf: List<DungeonPlayer> = ArrayList(4)
    var dungeonPlayer: DungeonPlayer? = null
    var stats: TabData = TabData()
    var floor = Floor.Unknown
    var paul = false

    init {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            dungeonTeammates.clear()
            dungeonTeammatesNoSelf = emptyList()
            dungeonPlayer = null
            stats = TabData()
            floor = Floor.Unknown
        }

        ChatEvents.SYSTEM_MESSAGE_EVENT.register { (_, _, unformatted) ->
            if (floor.number == -1) return@register

            if (princeRegex.matches(unformatted)) {
                stats.princeKilled = true
                return@register
            }

            if (batRegex.matches(unformatted)) {
                stats.bats.add(mc.player?.name?.string ?: return@register)
                return@register
            }

            var match = partyMessageRegex.find(unformatted)
            var name = match?.groupValues?.get(2)?.lowercase() ?: return@register
            when (match.groupValues[3].lowercase()) {
                "mimic killed", "mimic slain", "mimic killed!", "mimic dead", "mimic dead!", $$"$skytils-dungeon-score-mimic$" ->
                    stats.mimicKilled = true

                // amazing work LN, thank you for Prince Regicided, I didn't even know that was a word...
                "prince killed", "prince slain", "prince killed!", "prince dead", "prince dead!", "prince regicided!", $$"$skytils-dungeon-score-prince$" ->
                    stats.princeKilled = true

                "blaze done!", "blaze done", "blaze puzzle solved!" ->
                    stats.puzzles.find { it == Puzzle.BLAZE }.let { it?.status = PuzzleStatus.Completed }

                "bat killed", "bat killed!", "bat dead", "bat dead!" -> {
                    stats.bats.add(name)
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val entities = client.level?.entitiesForRendering()?.filter { it is Player } ?: return@register
            dungeonTeammatesNoSelf.forEach { teammate ->
                teammate.entity = entities.find { it.name.string == teammate.name } as? Player ?: teammate.entity
            }
        }
    }

    fun handlePlayerInfoUpdate(packet: ClientboundPlayerInfoUpdatePacket) {
        val tabListEntries = packet.entries().mapNotNull { it.displayName?.string }.ifEmpty { return }
        updateDungeonTeammates(dungeonTeammates, tabListEntries)
        if (dungeonTeammates.isNotEmpty()) {
            dungeonTeammatesNoSelf = dungeonTeammates.slice(1 until dungeonTeammates.size)
            dungeonPlayer = dungeonTeammates[0]
            dungeonPlayer!!.entity = mc.player!!
        }
        getDungeonPuzzles(tabListEntries)
        updateDungeonStats(tabListEntries)
    }

    private fun updateDungeonTeammates(previousTeammates: ArrayList<DungeonPlayer>, tabList: List<String>) {
        for (line in tabList) {
            val (_, name, clazz, clazzLevel) = tablistRegex.find(line)?.destructured ?: continue

            previousTeammates.find { it.name == name }?.let { player ->
                player.isDead = clazz == "DEAD"
                if (player.clazz == DungeonClass.Unknown) {
                    player.clazz = DungeonClass.entries.find { it.name == clazz } ?: DungeonClass.Unknown
                    player.clazzLvl = romanToInt(clazzLevel)
                }
            } ?: previousTeammates.add(
                DungeonPlayer(
                    name, DungeonClass.entries.find { it.name == clazz } ?: DungeonClass.Unknown,
                    romanToInt(clazzLevel), mc.connection?.getPlayerInfo(name)?.skin
                )
            )
        }
    }

    private fun getDungeonPuzzles(tabList: List<String>) {
        for (entry in tabList) {
            val (name, status) = puzzleRegex.find(entry)?.destructured ?: continue
            val puzzle = Puzzle.entries.find { it.displayName == name }?.takeIf { it != Puzzle.UNKNOWN } ?: continue
            if (puzzle !in stats.puzzles) stats.puzzles.add(puzzle)

            puzzle.status = when (status) {
                "✖" -> PuzzleStatus.Failed
                "✔" -> PuzzleStatus.Completed
                "✦" -> PuzzleStatus.Incomplete
                else -> continue
            }
        }
    }

    private fun updateDungeonStats(text: List<String>) {
        for (entry in text) {
            with(stats) {
                secretsPercent = secretPercentRegex.find(entry)?.groupValues?.get(1)?.toFloatOrNull() ?: secretsPercent
                completedRooms = completedRoomsRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: completedRooms
                secretsFound = secretCountRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: secretsFound
                openedRooms = openedRoomsRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: openedRooms
                puzzleCount = puzzleCountRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: puzzleCount
                deaths = deathsRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: deaths
                crypts = cryptRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: crypts
                elapsedTime = timeRegex.find(entry)?.groupValues?.get(1) ?: elapsedTime
            }
        }
    }

    fun handleSetPlayerTeam(packet: ClientboundSetPlayerTeamPacket) {
        val text = packet.parameters.getOrNull()?.let { ChatFormatting.stripFormatting(it.playerPrefix.string.plus(it.playerSuffix.string)) } ?: return

        val oldFloor = floor
        floorRegex.find(text)?.groupValues?.get(1)?.let { floor = Floor.fromString(it) }
        if (oldFloor == Floor.Unknown && floor != Floor.Unknown) {
            scope.launch(Dispatchers.IO) { paul = Web.hasBonusPaulScore() }
            LocationEvents.ON_ENTER_DUNGEON.invoker().enterDungeon(floor)
        }

        clearedRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            stats.percentCleared = it
        }
    }

    fun removedEntities(packet: ClientboundRemoveEntitiesPacket) {
        dungeonTeammates.forEach {
            val id = it.entity?.id ?: return@forEach
            if (packet.entityIds.contains(id)) it.entity = null
        }
    }

    private val timeRegex = Regex("^ Time: ((?:\\d+h ?)?(?:\\d+m ?)?\\d+s)$")
    private val cryptRegex = Regex("^ Crypts: (\\d+)$")
    private val deathsRegex = Regex("^Team Deaths: (\\d+)$")
    private val puzzleCountRegex = Regex("^Puzzles: \\((\\d+)\\)$")
    private val openedRoomsRegex = Regex("^ Opened Rooms: (\\d+)$")
    private val secretCountRegex = Regex("^ Secrets Found: (\\d+)$")
    private val completedRoomsRegex = Regex("^ Completed Rooms: (\\d+)$")
    private val secretPercentRegex = Regex("^ Secrets Found: ([\\d.]+)%$")
    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$")
    private val puzzleRegex = Regex("^ (\\w+(?: \\w+)*|\\?\\?\\?): \\[([✖✔✦])] ?(?:\\((\\w+)\\))?$")
    private val partyMessageRegex = Regex("Party > (?:\\[(.*?)] )?(.+?): (.+)$")
    private val princeRegex = Regex("^A Prince falls\\. \\+1 Bonus Score$")
    private val batRegex = Regex("^A Bat has been slain\\. \\+1 Bonus Score$")
    private val floorRegex = Regex("The Catacombs \\((\\w+)\\)$")
    private val clearedRegex = Regex("^Cleared: (\\d+)% \\(\\d+\\)$")
}