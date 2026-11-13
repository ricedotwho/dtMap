package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.features.map.Scan
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.TickCounters
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.max

object BloodRush {
    class RushRoom(
        val room: Room,
        val cleared: TickCounters.Timer = TickCounters.Timer.start(),
        var keyPickUp: TickCounters.Timer? = null,
        var doorOpen: TickCounters.Timer? = null
    ) {
        companion object {
            fun start(room: Room): RushRoom {
                val timer = RushRoom(room)
                rooms.add(timer)
                return timer
            }
        }
    }

    val rooms = mutableListOf<RushRoom>()
    var currentRoom: RushRoom? = null
    var done = false

    fun register() {
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register(AddMessage)

        MapEvents.ON_UPDATED_ROOMS.register(RoomUpdated)

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            rooms.clear()
            currentRoom = null
            done = false
        }
    }

    fun render(context: GuiGraphicsExtractor, example: Boolean): Pair<Int, Int> {
        if (example) return example(context)

        if (LocationUtils.currentArea != Island.Dungeon || rooms.isEmpty() || SoloClear.isSoloClearing()) return DtMap.zero

        val height = mc.font.lineHeight + 2
        var m = 0
        rooms.forEachIndexed { i, room ->
            val name = if (room.room.data == null) "???" else room.room.data!!.name
            m = max(mc.font.width(name), m)
            context.text(mc.font, "§6$name", 0, (i + 1) * height, Color.WHITE.rgb)
        }

        var nextWidth = m + 5
        m = mc.font.width("Clear")
        context.text(mc.font, "§fClear", nextWidth, 0, Color.WHITE.rgb)
        rooms.forEachIndexed { i, room ->
            val str = "§f${room.cleared.format()}"
            m = max(mc.font.width(str), m)
            context.text(mc.font, str, nextWidth, (i + 1) * height, Color.WHITE.rgb)
        }

        nextWidth += m + 5
        m = mc.font.width("Key")
        context.text(mc.font, "§7Key", nextWidth, 0, Color.WHITE.rgb)
        rooms.forEachIndexed { i, room ->
            if (room.keyPickUp == null) return@forEachIndexed
            val str = "§7${room.keyPickUp!!.format()}"
            m = max(mc.font.width(str), m)
            context.text(mc.font, str, nextWidth, (i + 1) * height, Color.WHITE.rgb)
        }

        nextWidth += m + 5
        context.text(mc.font, "§8Door", nextWidth, 0, Color.WHITE.rgb)
        rooms.forEachIndexed { i, room ->
            if (room.doorOpen == null) return@forEachIndexed
            context.text(mc.font, "§8${room.doorOpen!!.format()}", nextWidth, (i + 1) * height, Color.WHITE.rgb)
        }

        return nextWidth to height * rooms.size
    }

    private fun example(context: GuiGraphicsExtractor): Pair<Int, Int> {
        val height = mc.font.lineHeight + 2
        context.text(mc.font, "§6Layers", 0, height, Color.WHITE.rgb)
        context.text(mc.font, "§6Mines", 0, 2 * height, Color.WHITE.rgb)

        var nextWidth = mc.font.width("§6Layers") + 5
        context.text(mc.font, "§fClear", nextWidth, 0, Color.WHITE.rgb)
        context.text(mc.font, "§f10.00 | 11.00", nextWidth, height, Color.WHITE.rgb)
        context.text(mc.font, "§f9.00 | 8.00", nextWidth, 2 * height, Color.WHITE.rgb)

        nextWidth += mc.font.width("§710.00 | 11.00") + 5
        context.text(mc.font, "§7Key", nextWidth, 0, Color.WHITE.rgb)
        context.text(mc.font, "§71.00", nextWidth, height, Color.WHITE.rgb)
        context.text(mc.font, "§72.30 | 2.10", nextWidth, 2 * height, Color.WHITE.rgb)

        nextWidth += mc.font.width("§82.30 | 2.10") + 5
        context.text(mc.font, "§8Door", nextWidth, 0, Color.WHITE.rgb)
        context.text(mc.font, "§80", nextWidth, height, Color.WHITE.rgb)
        context.text(mc.font, "§80.30", nextWidth, 2 * height, Color.WHITE.rgb)

        return nextWidth to height * 3
    }

    private val AddMessage = ChatEvents.SystemMessageEvent { (component, message, unformatted) ->
        if (LocationUtils.currentArea != Island.Dungeon) return@SystemMessageEvent

        if (unformatted == DungeonMessages.dungeonStartMessage) {
            rooms.clear()
            val door = Scan.doors.find { it.rooms.any { it.owner!!.type == Room.Type.ENTRANCE }} ?: return@SystemMessageEvent
            val other = door.rooms.find { it.owner!!.type != Room.Type.ENTRANCE } ?: return@SystemMessageEvent
            currentRoom = RushRoom.start(other.owner!!)
            return@SystemMessageEvent
        }

        val isWitherKeyClaim = DungeonMessages.witherKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.witherKeyPickedUpString
        val isBloodKeyClaim = DungeonMessages.bloodKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.bloodKeyPickedUpString
        val witherDoorMatcher = DungeonMessages.witherDoorOpenPattern.matcher(unformatted)
        val isWitherDoorOpen = witherDoorMatcher.matches()
        val isBloodDoorOpen = unformatted == DungeonMessages.bloodDoorOpenMessage

        if (isBloodDoorOpen) {
            done = true
        }

        if (isWitherKeyClaim || isBloodKeyClaim) {
            if (currentRoom?.keyPickUp != null) currentRoom?.keyPickUp?.end()
            else {
                currentRoom?.cleared?.end()
                currentRoom?.keyPickUp = TickCounters.Timer.zero()
            }

            currentRoom?.doorOpen = TickCounters.Timer.start()
        } else if (isWitherDoorOpen || isBloodDoorOpen) {
            currentRoom?.doorOpen?.end()
            currentRoom = null
        }
    }

    private val RoomUpdated = MapEvents.UpdatedRooms { rooms ->
        rooms.forEach { (room, old, new) ->
            if (!room.rushRoom) return@forEach

            if (currentRoom?.room == room) {
                if (old != Room.State.CLEARED && old != Room.State.GREEN &&
                    (new == Room.State.CLEARED || new == Room.State.GREEN)
                ) {
                    currentRoom!!.cleared.end()
                    if (currentRoom!!.keyPickUp == null) currentRoom!!.keyPickUp = TickCounters.Timer.start()
                }
            } else if (currentRoom == null && !done) {
                if (new == Room.State.DISCOVERED) {
                    currentRoom = RushRoom.start(room)
                }
            }
        }
    }
}