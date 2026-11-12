package com.ricedotwho.dtmap.features.map

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.odtheking.odin.OdinMod.mc
import com.ricedotwho.dtmap.utils.BlockPosAdapter
import com.ricedotwho.dtmap.utils.RoomShapeAdapter
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier

data class RoomData(
    val name: String = "Unknown",
    var type: Room.Type = Room.Type.NORMAL,
    var cores: List<Int> = emptyList(),
    val crypts: Int = 0,
    val secrets: Int = 0,
    val secretDetails: Map<String, List<BlockPos>> = mutableMapOf(),
    val trappedChests: List<BlockPos> = emptyList(),
    val shape: Room.Shape = Room.Shape.UNKNOWN,
    val prince: Boolean = false
) {
    companion object {
        private val gson: Gson = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockPos::class.java, BlockPosAdapter())
            .registerTypeAdapter(Room.Shape::class.java, RoomShapeAdapter()).create()

        lateinit var roomList: Set<RoomData>
            private set

        lateinit var roomMap: MutableMap<Int, RoomData>
            private set

        init {
            try {
                val content = mc.resourceManager.getResource(
                    Identifier.fromNamespaceAndPath("dtmap", "rooms.json")
                ).get().open().bufferedReader()

                roomList = gson.fromJson(content, object : TypeToken<Set<RoomData>>() {}.type)
                roomMap = mutableMapOf<Int, RoomData>().apply {
                    putAll(roomList.flatMap { room -> room.cores.map { it to room } })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getRoomData(hash: Int): RoomData? {
            return roomMap[hash]
        }
    }
}