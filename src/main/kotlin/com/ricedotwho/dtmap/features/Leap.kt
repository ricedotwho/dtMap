package com.ricedotwho.dtmap.features

import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import kotlin.jvm.optionals.getOrNull

object Leap {
    var holdingLeap = false
    val ids = listOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(Tick)
    }

    private val Tick = ClientTickEvents.EndTick { client ->
        if (LocationUtils.currentArea != Island.Dungeon) return@EndTick

        val player = client.player ?: return@EndTick

        val item = player.mainHandItem
        val nbt = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        val id = nbt.getString("id").getOrNull() ?: return@EndTick
        holdingLeap = ids.contains(id)
    }
}