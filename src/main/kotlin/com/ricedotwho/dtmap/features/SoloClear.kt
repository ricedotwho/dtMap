package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.config.EspConfig
import com.ricedotwho.dtmap.config.MapConfig
import com.ricedotwho.dtmap.config.SoloClearConfig
import com.ricedotwho.dtmap.config.SoloClearing
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.map.Door
import com.ricedotwho.dtmap.features.map.DoorEsp
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.features.map.Scan
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.features.map.SpecialColumn
import com.ricedotwho.dtmap.utils.clickSlot
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.AABB
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object SoloClear {
    fun isSoloClearing(): Boolean {
        if (fuckCurrentMap || insight()) return false
        return solo()
    }

    fun solo() =
        SoloClearConfig.soloClearing == SoloClearing.YES || (SoloClearConfig.soloClearing == SoloClearing.AUTO && Scoreboard.dungeonTeammatesNoSelf.isEmpty())

    fun insight() =
        SoloClearConfig.soloClearHudInsight && DtMap.keybindShowHud.isDown

    fun hack() =
        (!isSoloClearing() && !MapConfig.legitMode) ||
                (SoloClearConfig.soloClearCheaterInsight && solo() && insight())

    fun hack(bool: Boolean) =
        !isSoloClearing() && bool

    fun legit() =
        (isSoloClearing() || MapConfig.legitMode) && !(SoloClearConfig.soloClearCheaterInsight && solo() && insight())

    fun register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dtm", "soloclearhud"), HudCallback)
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            lastClickedIn = null
            lastClosed = 0L
            fuckCurrentMap = false
        }
        MapEvents.ON_PLAYER_ENTER_ROOM.register(EnterRoom)
    }

    var lastClickedIn: Int? = null

    var lastClosed = 0L
    var fuckCurrentMap = false
    val HudCallback = HudElement { _, _ ->
        if (!SoloClearConfig.soloClearingInstaJoin || !isSoloClearing() || LocationUtils.currentArea != Island.Dungeon) return@HudElement

        val player = mc.player ?: return@HudElement

        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return@HudElement
        if (screen.menu.containerId == lastClickedIn) return@HudElement

        if (screen.title.string != "Undersized party!") {
            return@HudElement
        }

        val slot = screen.menu.getSlot(13)
        if (slot.item.hoverName.string != "Undersized party!") {
            return@HudElement
        }

        player.clickSlot(screen.menu.containerId, slot.index)
        lastClickedIn = screen.menu.containerId
        player.closeContainer()
        fuckCurrentMap = true
        lastClosed = System.currentTimeMillis()
    }

    fun shouldCloseIncomingWindows(): Boolean =
        System.currentTimeMillis() - lastClosed < 2000

    fun openedWindow(packet: ClientboundOpenScreenPacket, ci: CallbackInfo): Boolean {
        if (!shouldCloseIncomingWindows()) return false

        val title = packet.title.string
        if (title != "Creating instance....") return false

        Minecraft.getInstance().gui.setScreen(null)
        mc.connection!!.send(ServerboundContainerClosePacket(packet.containerId))
        ci.cancel()
        return true
    }

    val EnterRoom = MapEvents.EnterRoom { room ->
        if (!SoloClearConfig.soloClearingInstantRoomUpdate || !isSoloClearing()) return@EnterRoom

        room?.owner?.let {
            if (it.type == Room.Type.ENTRANCE) return@EnterRoom

            val updatedRooms = mutableListOf<Room.StateUpdated>()
            if (it.state == Room.State.UNDISCOVERED || it.state == Room.State.UNOPENED) {

                val oldState = it.state
                it.state = Room.State.DISCOVERED

                updatedRooms.add(Room.StateUpdated(it, oldState, it.state))

                it.doors.forEach { door ->
                     door.rooms.forEach roomsForEach@ { room ->
                        if (it == room.owner) {
                            return@roomsForEach
                        }

                        if (room.owner!!.state == Room.State.UNDISCOVERED) {
                            room.owner.state = Room.State.UNOPENED
                            room.owner.entryTile = room.pos.add(201, 201).divide(32)

                            updatedRooms.add(Room.StateUpdated(it, Room.State.UNDISCOVERED, room.owner.state))
                        }
                    }
                }

                SpecialColumn.updateSpecialColumn()
            }

            MapEvents.ON_UPDATED_ROOMS.invoker().updatedRooms(updatedRooms)
        }
    }

    fun renderDoorEsp(context: RenderExtractEvent) {
        val roomPlayerIn = DungeonMap.roomPlayerInNoBoundsCheck()

        Scan.doors.forEach { door ->
            if (door.type == Door.Type.BLOOD || door.type == Door.Type.NORMAL || !door.locked) {
                return@forEach
            }

            if (door.rooms.none { it.owner == roomPlayerIn?.owner }) return@forEach

            val colors = if (door.locked && door.type == Door.Type.WITHER && DoorEsp.witherKeys > 0) arrayOf(EspConfig.doorOpenableColor, EspConfig.doorOpenableColorFilled)
            else arrayOf(EspConfig.doorEspColor, EspConfig.doorEspColorFilled)

            val box = AABB(
                door.pos.x - 1.0, 69.0, door.pos.z - 1.0,
                door.pos.x + 2.0, 73.0, door.pos.z + 2.0
            )

            if (colors[0].alpha > 0) context.drawWireFrameBox(box, colors[0], EspConfig.doorEspOutlineWidth, false)
            if (colors[1].alpha > 0) context.drawFilledBox(box, colors[1], false)
        }
    }
}