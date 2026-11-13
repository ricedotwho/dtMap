package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.config.dungeon.EspConfig
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.utils.DungeonMessages
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.round

object Key {
    var currentKey: Entity? = null
    val names = listOf("Wither Key", "Blood Key")

    private val SOUND_ID = Identifier.fromNamespaceAndPath("dtmap", "key")
    private val KEY_SOUND = SoundEvent.createVariableRangeEvent(SOUND_ID)

    init {
        EventBus.subscribe(this)

        on<RenderExtractEvent> {
            if (SoloClear.isSoloClearing()) return@on

            currentKey?.let {
                if (EspConfig.keyTracer) {
                    drawTracer(Vec3(it.x, it.y + 1.5, it.z),
                        EspConfig.keyTracerColour,
                        false,
                        EspConfig.keyWidth
                    )
                }

                if (EspConfig.keyEsp) {
                    drawFilledBox(
                        AABB.unitCubeFromLowerCorner(it.position().add(-0.5, 1.25, -0.5)),
                        EspConfig.keyColour
                    )
                }
            }
        }
    }

    fun register() {
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register { (_, _, unformatted) ->
            if (!EspConfig.keyPickupPling || LocationUtils.currentArea != Island.Dungeon || DungeonMessages.inBoss || SoloClear.isSoloClearing()) return@register

            val witherKeyPickup = DungeonMessages.witherKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.witherKeyPickedUpString
            val bloodKeyPickup = DungeonMessages.bloodKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.bloodKeyPickedUpString

            if (witherKeyPickup || bloodKeyPickup) mc.player?.playSound(KEY_SOUND, EspConfig.keyPickupVolume, 1.0f)
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (LocationUtils.currentArea != Island.Dungeon || DungeonMessages.inBoss) return@register
            if (client.level == null) return@register
            if (currentKey?.isAlive == false) currentKey = null
            if (currentKey != null) return@register

            currentKey = client.level!!.entitiesForRendering().find {
                if (it !is ArmorStand) return@find false
                names.contains(it.name.string)
            } ?: return@register
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            currentKey = null
        }
    }

    fun is64(yaw: Float) =
        yaw * 64F == round(yaw * 64F)
}