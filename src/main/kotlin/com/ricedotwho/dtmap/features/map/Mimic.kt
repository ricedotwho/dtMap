package com.ricedotwho.dtmap.features.map

import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.config.C2Esp
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Location
import com.ricedotwho.dtmap.utils.drawBlockOverlay
import com.ricedotwho.dtmap.utils.drawLineBox
import com.ricedotwho.dtmap.utils.toAABB
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.monster.zombie.Zombie

object Mimic {
    fun register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WorldRender)
    }

    val WorldRender = LevelRenderEvents.AfterTranslucentTerrain { context ->
        val chest = Scan.chest ?: return@AfterTranslucentTerrain

        if (C2Esp.mimicEspFillColor.alpha > 0) context.drawBlockOverlay(chest, C2Esp.mimicEspFillColor, C2Esp.mimicLegit)
        if (C2Esp.mimicEspOutlineColor.alpha > 0) context.drawLineBox(chest.toAABB(), C2Esp.mimicEspOutlineColor, C2Esp.mimicEspOutlineWidth, C2Esp.mimicLegit)
    }

    fun entityEvent(packet: ClientboundEntityEventPacket) {
        if (Location.dungeonFloor.number != 6 && Location.dungeonFloor.number != 7) return
        if (Scoreboard.stats.mimicKilled || DungeonMessages.inBoss) return

        if (packet.eventId != 3.toByte()) return

        val entity = packet.getEntity(mc.level!!) as? Zombie ?: return
        if (!entity.isBaby) return

        Scoreboard.stats.mimicKilled = true
    }
}