package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.ricedotwho.dtmap.config.dungeon.EspConfig
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.utils.Colours
import com.ricedotwho.dtmap.utils.box
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

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

        EventBus.subscribe(this)

        on<RenderExtractEvent> {
            if (!SoloClear.hack(EspConfig.redstoneKeyEsp)) return@on

            key?.let { key ->
                drawWireFrameBox(key.box(), Colours.WHITE)
            }
        }
    }
}