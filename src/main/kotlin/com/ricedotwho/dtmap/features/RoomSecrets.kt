package com.ricedotwho.dtmap.features

import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.gui.Hud
import com.ricedotwho.dtmap.gui.Hud.Condition
import com.ricedotwho.dtmap.utils.Chat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.Color

object RoomSecrets : Hud.Component("room-secrets", 0.2, 0.6, Hud.Type.Dungeon, 1.5f,
    staticRenderConditions = mutableListOf(Condition.Boss, Condition.Clear, Condition.Alt, Condition.HideOverlaySecrets),
    allowedStaticRenderConditions = mutableListOf(Condition.Boss, Condition.F7Boss, Condition.Clear, Condition.BeforeMort, Condition.Alt, Condition.HideOverlaySecrets)) {
    val secretsRegex = Regex(".+§7(\\d+)/\\d+ Secrets")
    var currentRoomSecrets: Int? = null

    fun register() {
        MapEvents.ON_PLAYER_ENTER_ROOM.register { room ->
            if (room?.owner == null) {
                currentRoomSecrets = null
            }
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            currentRoomSecrets = null
        }

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
        secretsRegex.find(message.string)?.let { found ->
            currentRoomSecrets = found.groups[1]?.value?.toIntOrNull()
        }
    }

    fun modifyOverlay(message: Component): Component {
        secretsRegex.find(message.string)?.let { found ->
            currentRoomSecrets = found.groups[1]?.value?.toIntOrNull()
            if (staticRenderConditions.contains(Condition.HideOverlaySecrets)) {
                currentRoomSecrets?.let {
                    val first = found.groups[1]!!.range.first
                    return Component.literal(message.string.substring(0 until first - 2).trimEnd());
                }
            }
        }
        return message
    }

    override fun render(context: GuiGraphicsExtractor) {
        val currentRoomSecrets = currentRoomSecrets ?: return
        val currentRoom = DungeonMap.roomPlayerIn() ?: return

        val secretsInRoom = currentRoom.owner!!.data!!.secrets
        if (secretsInRoom == 0) return
        val percent = currentRoomSecrets.toFloat() / secretsInRoom.toFloat()
        val color = when {
            percent < 0.5f -> Color.RED
            percent < 1f -> Color.YELLOW
            else -> Color.GREEN
        }

        context.text(mc.font, "${currentRoomSecrets}/${secretsInRoom}", 0, 0, color.rgb, true)
    }

    override fun example(context: GuiGraphicsExtractor) {
        context.text(mc.font, "2/4", 0, 0, Color.YELLOW.rgb, true)
    }

    override fun bounds(): Pair<Double, Double> =
        Pair(mc.font.width("2/4").toDouble(), mc.font.lineHeight.toDouble())
}