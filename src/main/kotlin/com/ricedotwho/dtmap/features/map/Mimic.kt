package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.ricedotwho.dtmap.config.dungeon.EspConfig
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.box
import com.ricedotwho.dtmap.utils.toAABB
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.monster.zombie.Zombie

object Mimic {
    init {
        on<RenderExtractEvent> {
            if (!EspConfig.mimicEsp) return@on
            val chest = Scan.chest ?: return@on

            if (EspConfig.mimicEspFillColor.alpha > 0) drawFilledBox(chest.box(), EspConfig.mimicEspFillColor, EspConfig.mimicLegit)
            if (EspConfig.mimicEspOutlineColor.alpha > 0) drawWireFrameBox(chest.toAABB(), EspConfig.mimicEspOutlineColor, EspConfig.mimicEspOutlineWidth, EspConfig.mimicLegit)
        }
    }

    fun entityEvent(packet: ClientboundEntityEventPacket) {
        if (DungeonListener.floor == null || DungeonListener.floor!!.floorNumber < 6) return
        if (Scoreboard.stats.mimicKilled || DungeonMessages.inBoss) return

        if (packet.eventId != 3.toByte()) return

        val entity = packet.getEntity(mc.level!!) as? Zombie ?: return
        if (!entity.isBaby) return

        Scoreboard.stats.mimicKilled = true
    }
}