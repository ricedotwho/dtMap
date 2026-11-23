package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.ricedotwho.dtmap.config.MapConfig
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.utils.Chat
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Scheduler
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object BloodRushInstaClear {
    fun register() {
        MapEvents.ON_UPDATED_ROOMS.register(RoomUpdated)
    }

    private val SOUND_ID = Identifier.fromNamespaceAndPath("dtmap", "siren")
    private val SIREN_SOUND = SoundEvent.createVariableRangeEvent(SOUND_ID)

    private val RoomUpdated = MapEvents.UpdatedRooms { rooms ->
        if (!MapConfig.bloodRushInstaclearNotification) return@UpdatedRooms

        rooms.forEach { (room, old, new) ->
            if (!room.rushRoom || room.type == Room.Type.FAIRY || !DungeonMessages.seenDungeonStart) return@forEach

            if ((old == Room.State.UNOPENED || old == Room.State.UNDISCOVERED) && (new == Room.State.CLEARED || new == Room.State.GREEN)) {
                Chat.send("A room was instacleared: ${room.data?.name}")
                Scheduler.scheduleTickEnd {
                    mc.player?.playSound(SIREN_SOUND, 1f, 1f)
                }
            }
        }
    }
}