package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.darker
import com.ricedotwho.dtmap.config.MapConfig
import com.ricedotwho.dtmap.features.SoloClear
import net.minecraft.client.gui.GuiGraphicsExtractor

class Door private constructor(val pos: Vec2i, var type: Type, val rooms: MutableList<Room.Tile>, nothing: Boolean = false) {
    enum class Type {
        BLOOD, NORMAL, WITHER;
    }

    inline val placement: Pair<Float, Float>
        get() {
            val x = (pos.x + 185) shr 4
            val z = (pos.z + 185) shr 4
            val xEven = x % 2
            val zEven = z % 2
            val thicknessBasedOffset = (16f - MapConfig.doorThicknessF) / 2f
            val xOffset = (x shr 1) * (20f) + xEven * 16f + (xEven xor 1) * thicknessBasedOffset
            val yOffset = (z shr 1) * (20f) + zEven * 16f + (zEven xor 1) * thicknessBasedOffset

            return Pair(xOffset, yOffset)
        }

    var locked = type == Type.WITHER || type == Type.BLOOD
    var seen
        get() = rooms.any { it.owner!!.state != Room.State.UNDISCOVERED && it.owner.state != Room.State.UNOPENED }
        private set(_) {}


    constructor(pos: Vec2i, type: Type, rooms: MutableList<Room.Tile>) : this(pos, type, rooms, false) {
        rooms.forEach { it.owner!!.doors.add(this) }
    }

    private fun size(): Vec2i {
        val xOffset = ((pos.x + 185) shr 4) % 2
        val zOffset = ((pos.z + 185) shr 4) % 2
        return Vec2i(
            (xOffset xor 1) * MapConfig.doorThicknessF.toInt() + xOffset * 4,
            (zOffset xor 1) * MapConfig.doorThicknessF.toInt() + zOffset * 4
        )
    }

    fun render(context: GuiGraphicsExtractor) {
        val size = size()
        if (size != Vec2i(0, 0)) {
            val matrices = context.pose()
            matrices.pushMatrix()
            matrices.translate(placement.first, placement.second)

            context.fill(0, 0, size.x, size.z, color().rgba)

            matrices.popMatrix()
        }
    }

    private fun color(): Color {
        val getCheatingColor = fun (): Color {
            return when (type) {
                Type.BLOOD -> MapConfig.bloodDoorColor
                Type.WITHER -> {
                    if (locked) {
                        MapConfig.witherDoorColor
                    } else {
                        if (rooms.firstOrNull { it.owner!!.type != Room.Type.NORMAL }?.owner?.type == Room.Type.FAIRY) {
                            MapConfig.fairyDoorColor
                        } else {
                            MapConfig.normalDoorColor
                        }
                    }
                }
                Type.NORMAL ->
                    when (rooms.firstOrNull { it.owner!!.type != Room.Type.NORMAL && it.owner.type != Room.Type.FAIRY }?.owner?.type) {
                        Room.Type.ENTRANCE -> MapConfig.entranceDoorColor
                        Room.Type.BLOOD -> MapConfig.bloodDoorColor
                        Room.Type.CHAMPION -> MapConfig.championDoorColor
                        Room.Type.FAIRY -> MapConfig.fairyDoorColor // this doesn't matter because we scan if the rooms are fairy or not.
                        Room.Type.NORMAL -> MapConfig.normalDoorColor
                        Room.Type.PUZZLE -> MapConfig.puzzleDoorColor
                        Room.Type.RARE -> MapConfig.rareDoorColor
                        Room.Type.TRAP -> MapConfig.trapDoorColor
                        Room.Type.UNKNOWN -> MapConfig.normalDoorColor
                        null -> MapConfig.normalDoorColor
                    }
            }
        }

        val noInfoColor = rooms.any { it.owner!!.state == Room.State.UNDISCOVERED || it.owner.state == Room.State.UNOPENED }
        val legit = SoloClear.legit()
        if (legit && noInfoColor) {
            return when (type) {
                Type.BLOOD -> if (locked) MapConfig.bloodDoorColor.darker(MapConfig.darkenMultiplier) else MapConfig.bloodDoorColor
                Type.WITHER -> if (locked) MapConfig.witherDoorColor.darker(MapConfig.darkenMultiplier) else MapConfig.witherDoorColor
                Type.NORMAL -> MapConfig.unopenedDoorColor
            }
        }

        val color = getCheatingColor()

        if (noInfoColor) {
            return color.darker(MapConfig.darkenMultiplier)
        }

        return color
    }
}