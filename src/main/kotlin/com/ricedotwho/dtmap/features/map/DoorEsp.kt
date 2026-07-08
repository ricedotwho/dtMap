package com.ricedotwho.dtmap.features.map

import com.ricedotwho.dtmap.config.C2Esp
import com.ricedotwho.dtmap.config.C3Other
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.features.SoloClear
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Location
import com.ricedotwho.dtmap.utils.drawFilled
import com.ricedotwho.dtmap.utils.drawLineBox
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB

object DoorEsp {
    var witherKeys = 0
    var bloodKey = false
    var bloodOpened = false
    private var doorCount = 0
    var doorBlockPositions = mutableSetOf<BlockPos>()

    fun register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(DoorRenderer)
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            witherKeys = 0
            bloodKey = false
            bloodOpened = false
            doorCount = 0
            doorBlockPositions.clear()
        }
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register(AddMessage)
        ClientTickEvents.END_CLIENT_TICK.register(EndTick)
    }

    private val EndTick = ClientTickEvents.EndTick { client ->
        if (doorCount >= Scan.doors.size) {
            return@EndTick
        }

        val level = client.level ?: return@EndTick

        Scan.doors.forEach { door ->
            if ((door.type == Door.Type.NORMAL || !door.locked) && door.rooms.none { it.owner!!.type == Room.Type.ENTRANCE }) return@forEach

            for (x in door.pos.x-1..door.pos.x+1) {
                for (z in door.pos.z-1..door.pos.z+1) {
                    for (y in 69..72) {
                        val pos = BlockPos(x, y, z)
                        if (doorBlockPositions.add(pos)) {
                            val state = level.getBlockState(pos)
                            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)
                        }
                    }
                }
            }
        }

        doorCount = Scan.doors.size
    }

    private val AddMessage = ChatEvents.SystemMessageEvent { (component, message, unformatted) ->
        if (Location.island != Location.Island.Dungeon) return@SystemMessageEvent

        if (DungeonMessages.witherKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.witherKeyPickedUpString) {
            witherKeys++
        } else if (DungeonMessages.witherDoorOpenPattern.matcher(unformatted).matches()) {
            witherKeys--
        } else if (DungeonMessages.bloodKeyClaimPattern.matcher(unformatted).matches() || unformatted == DungeonMessages.bloodKeyPickedUpString) {
            bloodKey = true
        } else if (unformatted == DungeonMessages.bloodDoorOpenMessage) {
            bloodKey = false
            bloodOpened = true
        }
    }

    private val DoorRenderer = LevelRenderEvents.AfterTranslucentTerrain { context ->
        if (SoloClear.isSoloClearing()) {
            if (C3Other.soloClearingDoorEsp) SoloClear.renderDoorEsp(context)
            return@AfterTranslucentTerrain
        }

        if (C2Esp.doorEsp == 0) return@AfterTranslucentTerrain

        val playerRoom = DungeonMap.roomPlayerInNoBoundsCheck()?.owner
        if (C2Esp.doorEsp > 1 && (playerRoom == null || !playerRoom.rushRoom)) return@AfterTranslucentTerrain

        val fairy = Scan.rooms.find { room -> room.type == Room.Type.FAIRY }
        Scan.doors.forEach {
            if (it.type == Door.Type.NORMAL || !it.locked) {
                return@forEach
            }

            if (C2Esp.doorEsp != 1 && !it.seen) {
                return@forEach
            }

            val renderDoor = fun(door: Door) {
                val colors = if (door.locked && ((door.type == Door.Type.BLOOD && bloodKey) || (door.type == Door.Type.WITHER && witherKeys > 0))) arrayOf(C2Esp.doorOpenableColor, C2Esp.doorOpenableColorFilled)
                             else arrayOf(C2Esp.doorEspColor, C2Esp.doorEspColorFilled)

                val box = AABB(
                    door.pos.x - 1.0, 69.0, door.pos.z - 1.0,
                    door.pos.x + 2.0, 73.0, door.pos.z + 2.0
                )
                if (colors[0].alpha > 0) context.drawLineBox(box, colors[0], C2Esp.doorEspWidth, false)
                if (colors[1].alpha > 0) context.drawFilled(box, colors[1], false)
            }

            if (C2Esp.doorEsp == 2 && fairy != null && it.rooms.contains(fairy.tiles[0])) {
                val door = Scan.doors.find { door -> door != it && door.type == Door.Type.WITHER && door.rooms.any { room -> room.owner!!.type == Room.Type.FAIRY } }
                if (door != null && door.locked) {
                    val box = AABB(
                        it.pos.x - 1.0, 69.0, it.pos.z - 1.0,
                        it.pos.x + 2.0, 73.0, it.pos.z + 2.0
                    )
                    if (C2Esp.doorFairyColor.alpha > 0) context.drawLineBox(box, C2Esp.doorFairyColor, C2Esp.doorEspWidth, false)
                    if (C2Esp.doorFairyColorFilled.alpha > 0) context.drawFilled(box, C2Esp.doorFairyColorFilled, false)

                    renderDoor(door)

                    return@forEach
                }
            }

            renderDoor(it)
        }
    }
}