package com.ricedotwho.dtmap.features

import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.config.C3Other
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.utils.drawBlockOverlay
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object RedstoneKeySkullHighlight {
    private var key: BlockPos? = null

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (mc.player == null) return@register
            if (Scoreboard.floor == Scoreboard.Floor.Unknown) return@register

            DungeonMap.roomPlayerIn()?.let {
                if (it.owner?.rotation == Room.Rotation.NONE || it.owner?.data?.name != "Redstone Key") {
                    key = null
                    return@register
                }

                key = it.owner.data!!.secretDetails["redstone_key"]?.find { position ->
                    val pos = it.owner.offset(position)!!
                    mc.level!!.getBlockState(pos).block == Blocks.PLAYER_HEAD
                }?.let { pos -> it.owner.offset(pos) }
            }
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            key = null
        }

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { ctx ->
            if (!SoloClear.hack(C3Other.redstoneKeySkullHighlight)) return@register

            key?.let { key ->
                ctx.drawBlockOverlay(key, Color.WHITE, false)
            }
        }
    }
}