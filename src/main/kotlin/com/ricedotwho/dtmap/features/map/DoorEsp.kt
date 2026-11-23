package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.config.EspConfig
import com.ricedotwho.dtmap.config.SoloClearConfig
import com.ricedotwho.dtmap.events.ChatEvents
import com.ricedotwho.dtmap.features.SoloClear
import com.ricedotwho.dtmap.utils.DungeonMessages
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
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
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            witherKeys = 0
            bloodKey = false
            bloodOpened = false
            doorCount = 0
            doorBlockPositions.clear()
        }
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register(AddMessage)
        ClientTickEvents.END_CLIENT_TICK.register(EndTick)

        // hmm
        EventBus.subscribe(this)
    }

    init {
        on<RenderExtractEvent> {
            if (SoloClear.isSoloClearing()) {
                if (SoloClearConfig.soloClearingDoorEsp) SoloClear.renderDoorEsp(this)
                return@on
            }

            if (!EspConfig.doorEsp) return@on

            val playerRoom = DungeonMap.roomPlayerInNoBoundsCheck()?.owner
            if (playerRoom == null || !playerRoom.rushRoom) return@on

            Scan.doors.forEach {
                if (it.type == Door.Type.NORMAL || !it.locked || !it.seen) {
                    return@forEach
                }

                val renderDoor = fun(door: Door) {
                    val colors = if (door.locked && ((door.type == Door.Type.BLOOD && bloodKey) || (door.type == Door.Type.WITHER && witherKeys > 0))) arrayOf(EspConfig.doorOpenableColor, EspConfig.doorOpenableColorFilled)
                    else arrayOf(EspConfig.doorEspColor, EspConfig.doorEspColorFilled)

                    val box = AABB(
                        door.pos.x - 1.0, 69.0, door.pos.z - 1.0,
                        door.pos.x + 2.0, 73.0, door.pos.z + 2.0
                    )
                    if (colors[0].alpha > 0) drawWireFrameBox(box, colors[0], EspConfig.doorEspOutlineWidth, false)
                    if (colors[1].alpha > 0) drawFilledBox(box, colors[1], false)
                }

                renderDoor(it)
            }
        }
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
        if (LocationUtils.currentArea != Island.Dungeon) return@SystemMessageEvent

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
}