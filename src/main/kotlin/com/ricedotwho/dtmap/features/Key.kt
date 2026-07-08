package com.ricedotwho.dtmap.features

import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.config.C2Esp
import com.ricedotwho.dtmap.config.C3Other
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Location
import com.ricedotwho.dtmap.utils.drawFilled
import com.ricedotwho.dtmap.utils.drawLineFromCursor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object Key {
    var currentKey: Entity? = null
    val uuids = listOf("2865274b-3097-394e-8149-ec629c72d850", "73f6d1f9-df41-3d1d-b98c-e1442d915885")

    private val SOUND_ID = Identifier.fromNamespaceAndPath("dtmap", "key")
    private val KEY_SOUND = SoundEvent.createVariableRangeEvent(SOUND_ID)

    fun register() {
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register { (_, _, unformatted) ->
            if (!C3Other.keyPickupPling || Location.island != Location.Island.Dungeon || DungeonMessages.inBoss || SoloClear.isSoloClearing()) return@register

            val witherKeyPickup = DungeonMessages.witherKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.witherKeyPickedUpString
            val bloodKeyPickup = DungeonMessages.bloodKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.bloodKeyPickedUpString

            if (witherKeyPickup || bloodKeyPickup) DtMap.mc.player?.playSound(KEY_SOUND, C3Other.keyPickupPlingVolume, 1.0f)
        }

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { ctx ->
            if (SoloClear.isSoloClearing()) return@register

            currentKey?.let {
                if (C2Esp.keyTracer) {
                    ctx.drawLineFromCursor(
                        Vec3(it.x, it.y + 1.5, it.z),
                        C2Esp.keyTracerColor,
                        C2Esp.keyTracerWidth
                    )
                }

                if (C2Esp.keyEsp) {
                    ctx.drawFilled(
                        AABB.unitCubeFromLowerCorner(it.position().add(-0.5, 1.25, -0.5)),
                        C2Esp.keyColor,
                        true
                    )
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (Location.island != Location.Island.Dungeon || DungeonMessages.inBoss) return@register
            if (client.level == null) return@register
            if (currentKey?.isAlive == false) currentKey = null
            if (currentKey != null) return@register

            currentKey = client.level!!.entitiesForRendering().find {
                if (it !is ArmorStand) return@find false
                val head = it.getItemBySlot(EquipmentSlot.HEAD)
                if (!head.`is`(Items.PLAYER_HEAD)) return@find false
                val profile = head.get(DataComponents.PROFILE) ?: return@register
                uuids.contains(profile.partialProfile().id.toString())
            } ?: return@register
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            currentKey = null
        }
    }
}