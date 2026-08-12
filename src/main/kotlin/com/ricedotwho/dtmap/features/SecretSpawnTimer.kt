package com.ricedotwho.dtmap.features

import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.gui.Hud
import com.ricedotwho.dtmap.utils.DungeonMessages
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object SecretSpawnTimer : Hud.Component("ItemPickup", 0.5, 0.6, Hud.Type.Dungeon, staticRenderConditions = mutableListOf(Hud.Condition.Clear)) {
    val a: Color = Color(85, 255, 85)
    val b: Color = Color.decode("#d1a000")
    val c: Color = Color(255, 85, 85)

    fun register() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            works = false
        }

        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register { (_, _, unformatted) ->
            if (unformatted == DungeonMessages.dungeonStartMessage) {
                counter = 0
                works = true
            } else if (DungeonMessages.dungeonEndString.contains(unformatted)) {
                works = false
            }
        }
    }

    var counter: Int = 0
    var works = false

    fun tick() {
        counter++
    }

    override fun render(context: GuiGraphicsExtractor) {
        if (!works) return

        val away = 20 - counter % 20

        val color = when {
            away < 5 -> a
            away < 10 -> b
            else -> c
        }

        context.centeredText(DtMap.mc.font, "$away", 0, 0, color.rgb)
    }

    override fun example(context: GuiGraphicsExtractor) {
        context.centeredText(DtMap.mc.font, "2", 0, 0, Color(85, 255, 85).rgb)
    }

    override fun offsetBounds(width: Int, height: Int): Pair<Int, Int> =
        Pair(-(DtMap.mc.font.width("2") / 2 * scale).toInt(), 0)

    override fun bounds(): Pair<Double, Double> =
        Pair(DtMap.mc.font.width("2").toDouble(), DtMap.mc.font.lineHeight.toDouble())
}