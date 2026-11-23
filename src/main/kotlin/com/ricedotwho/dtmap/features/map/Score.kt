package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.ricedotwho.dtmap.DtMap.zero
import com.ricedotwho.dtmap.config.MapConfig
import com.ricedotwho.dtmap.config.ScoreCalculation
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.SoloClear
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object Score {
    var score = 0
    var foundSecretsCount = 0
    var sent270Message = false
    var sent300Message = false

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (mc.player == null || DungeonListener.floor == null) return@register

            score = Scoreboard.stats.calculateScore()

            if (MapConfig.score270MessageEnabled && score > 270 && !sent270Message && !SoloClear.isSoloClearing()) {
                mc.player?.connection?.sendCommand("pc ${MapConfig.score270Message}")
                sent270Message = true
            }

            if (MapConfig.score300MessageEnabled && score > 300 && !sent300Message && !SoloClear.isSoloClearing()) {
                mc.player?.connection?.sendCommand("pc ${MapConfig.score300Message}")
                sent300Message = true
            }
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            score = 0
            foundSecretsCount = 0
            sent270Message = false
            sent300Message = false
        }

        MapEvents.ON_UPDATED_ROOMS.register {
            foundSecretsCount = Scan.rooms.sumOf { room -> if (room.state == Room.State.UNDISCOVERED || room.state == Room.State.UNOPENED || room.data == null) 0 else room.data!!.secrets }
        }
    }

    fun actualRender(context: GuiGraphicsExtractor): Pair<Int, Int> {
        if (LocationUtils.currentArea != Island.Dungeon || DungeonListener.floor == null) return zero
        val totalSecrets = Scoreboard.stats.calculateTotalSecrets()
        val secrets = "§b${Scoreboard.stats.secretsFound}§7-§e${Scoreboard.stats.calculateMinimumSecrets()}§7-§c${totalSecrets}"
        val scoreText = "§${if (score < 270) "c" else if (score < 300) "e" else "a"}${score}"
        val showPrince = if (SoloClear.legit()) Prince.legitPrince else Prince.cheaterPrince

        val unfoundSecrets = if (SoloClear.isSoloClearing() && totalSecrets != 0) {
            val unfoundSecrets = totalSecrets - foundSecretsCount
            val color = if (unfoundSecrets >= Scoreboard.stats.calculateMinimumSecrets(forceNoMaxBonus = true, forceNeeded = true)) "§a" else "§6"
            "$color$unfoundSecrets   "
        } else ""

        val line1 = "$secrets   $unfoundSecrets$scoreText"

        val line2 = "${if (Scoreboard.stats.deaths > 0) "§7D: §c${Scoreboard.stats.deaths}   " else ""}${if (!Scoreboard.stats.mimicKilled && DungeonListener.floor!!.floorNumber >= 6) "§7M: §c✖   " else ""}${if (!Scoreboard.stats.princeKilled && showPrince) "§7P: §c✖   " else ""}${if (Scoreboard.stats.bats.size < (if (SoloClear.isSoloClearing()) 1 else 5)) "§7B: §c${Scoreboard.stats.bats.size.coerceAtMost(5)}   " else ""}${if (Scoreboard.stats.crypts < 5) "§c${Scoreboard.stats.crypts}§7/§a5" else ""}".trim()

        val mapSize = DungeonMap.calculateMapSize()
        val size = (mapSize.z * 16 + (mapSize.z - 1) * 4 + MapConfig.backgroundSize.toInt() * 2)
        val textSize = mc.font.width("$line1   $line2")
        val oneLine = textSize < size
        if (oneLine) {
            context.centeredText(mc.font, "$secrets   ${if (line2.isNotEmpty()) "$line2   " else ""}$unfoundSecrets$scoreText", 0, 0, Color.WHITE.rgb)
        } else {
            context.centeredText(mc.font, line1, 0, 0, Color.WHITE.rgb)
            context.centeredText(mc.font, line2, 0, mc.font.lineHeight, Color.WHITE.rgb)
        }

        return mc.font.width("§7D: §c0  §7M: §c✖  §7P: §c✖  §7B: §c✖  §c0§7/§a5") to  mc.font.lineHeight * 2 + 2
    }

    fun example(context: GuiGraphicsExtractor): Pair<Int, Int> {
        context.centeredText(mc.font, "§b10§7-§e49§7-§c55     §a300", 0, 0, Color.WHITE.rgb)
        context.centeredText(mc.font, "§7D: §c1  §7M: §c✖  §7P: §c✖  §7B: §c✖  §c0§7/§a5", 0, mc.font.lineHeight + 2, Color.WHITE.rgb)
        return mc.font.width("§7D: §c0  §7M: §c✖  §7P: §c✖  §7B: §c✖  §c0§7/§a5") to  mc.font.lineHeight * 2 + 2
    }

    fun render(context: GuiGraphicsExtractor, example: Boolean): Pair<Int, Int> {
        if (MapConfig.scoreCalculation != ScoreCalculation.STANDALONE) return zero
        if (example) return example(context)
        return actualRender(context)
    }
}