package com.ricedotwho.dtmap.config.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.textDim
import com.ricedotwho.dtmap.DtMap.zero
import com.ricedotwho.dtmap.features.Condition
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.utils.Colours
import com.ricedotwho.dtmap.utils.dtHUD
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.chat.Component

object RoomSecrets : Module(
    name = "dtRoomSecrets",
    description = "provided t ytouu by dtMi0"
) {
    private val secretsRegex = Regex(".+§7(\\d+)/(\\d+) Secrets")
    private val hideActionBar by BooleanSetting("Hide from action bar", false, desc = "")
    private val roomSecrets by dtHUD("Room Secrets", default = mutableListOf(Condition.CLEAR, Condition.ALT)) {
        if (it) {
            textDim("2/4", 0, 0, Colours.YELLOW)
        } else {
            val room = DungeonMap.roomPlayerIn()?.owner ?: return@dtHUD zero

            val secretsInRoom = room.data!!.secrets
            if (secretsInRoom == 0) return@dtHUD zero
            val percent = room.secretsDone.toFloat() / secretsInRoom.toFloat()
            val color = when {
                percent < 0.5f -> Colours.RED
                percent < 1f -> Colours.YELLOW
                else -> Colours.GREEN
            }

            textDim("${room.secretsDone}/${secretsInRoom}", 0, 0, color, true)
        }
    }

    init {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            if (overlay) onOverlay(message)
            true
        }

        ClientReceiveMessageEvents.MODIFY_GAME.register { message, overlay ->
            if (overlay) modifyOverlay(message)
            else message
        }
    }

    fun onOverlay(message: Component) {
        val room = DungeonMap.roomPlayerIn()?.owner ?: return
        secretsRegex.find(message.string)?.let { match ->
            val found = match.groups[1]?.value?.toInt() ?: return@let
            val max = match.groups[2]?.value?.toInt() ?: return@let
            if (room.data?.secrets != max) return@let
            room.secretsDone = found
        }
    }

    fun modifyOverlay(message: Component): Component {
        secretsRegex.find(message.string)?.let { found ->
            val currentRoomSecrets = found.groups[1]?.value?.toIntOrNull()
            if (hideActionBar) {
                currentRoomSecrets?.let {
                    val first = found.groups[1]!!.range.first
                    return Component.literal(message.string.substring(0 until first - 2).trimEnd());
                }
            }
        }
        return message
    }
}