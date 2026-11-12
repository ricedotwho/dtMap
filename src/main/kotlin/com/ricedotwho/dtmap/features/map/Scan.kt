package com.ricedotwho.dtmap.features.map

import com.google.gson.JsonObject
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.utils.equalsAny
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.status.ChunkStatus
import java.util.Collections

object Scan {
    val topLeftRoom = Vec2i(-185, -185)
    var loadedAllRooms = false
    var roomsList: Array<Room.Tile> = Array(36) {
        val x = it / 6
        val z = it % 6
        Room.Tile(null, topLeftRoom.add(x * 32, z * 32))
    }

    val blacklisted = arrayOf(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST
    )

    fun register() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(Unload)
        ClientTickEvents.END_LEVEL_TICK.register(WorldTick)
    }

    var chest: BlockPos? = null
    val WorldTick = ClientTickEvents.EndLevelTick { level ->
        if (rooms.isEmpty() || chest != null) return@EndLevelTick

        chest = rooms.firstNotNullOfOrNull { room ->
            if (room.rotation == Room.Rotation.NONE) return@firstNotNullOfOrNull null

            val chests = room.data?.secretDetails["chest"]?.map { level.getBlockEntity(room.offset(it)!!) } ?: return@firstNotNullOfOrNull null
            val pos = chests.find { it is TrappedChestBlockEntity }?.blockPos ?: return@firstNotNullOfOrNull null
            room.mimic = true
            pos
        } ?: return@EndLevelTick
    }

    val Unload = ClientLevelEvents.AfterClientLevelChange { _, _ ->
        rooms.clear()
        roomsList = Array(36) {
            val x = it / 6
            val z = it % 6
            Room.Tile(null, topLeftRoom.add(x * 32, z * 32))
        }
        doors.clear()
        loadedAllRooms = false
        chest = null

        allSecrets = 0
        puzzles = listOf()
        blood = null
    }

    fun getTopY(chunk: ChunkAccess, pos: Vec2i): Int? {
        var height = 0
        for (y in 160 downTo 11) {
            val block = chunk.getBlockState(BlockPos(pos.x and 15, y, pos.z and 15)).block
            if (block == Blocks.VOID_AIR) {
                return null
            }

            if (block != Blocks.AIR) {
                height = y
                break
            }
        }

        return height
    }

    fun calculateCore(chunk: ChunkAccess, pos: Vec2i): Pair<Int, Int>? {
        val sb = StringBuilder(150)

        val height = getTopY(chunk, pos) ?: return null
        val scanHeight = height.coerceIn(11..140)

        sb.append((140 - scanHeight).toString())
        var bedrock = 0
        for (y in scanHeight downTo 12) {
            val block = chunk.getBlockState(BlockPos(pos.x and 15, y, pos.z and 15)).block

            if (bedrock >= 2 && block == Blocks.AIR) {
                sb.append(CharArray(y - 11) { 'a' })
            }

            if (block == Blocks.BEDROCK) {
                bedrock++
            } else {
                bedrock = 0

                if (blacklisted.any { it == block }) continue
            }

            sb.append(BuiltInRegistries.BLOCK.getKey(block).path[0].lowercaseChar())
        }

        return Pair(sb.toString().hashCode(), height)
    }
    
    fun getBottomY(chunk: ChunkAccess, pos: Vec2i): Int? {
        for (y in 0..160) {
            val block = chunk.getBlockState(BlockPos(pos.x and 15, y, pos.z and 15)).block
            if (block != Blocks.VOID_AIR && block != Blocks.AIR) {
                return y
            }
        }

        return null
    }

    val rooms = Collections.synchronizedSet(mutableSetOf<Room>())
    val doors = Collections.synchronizedList(mutableListOf<Door>())
    var allSecrets: Int = 0
    var puzzles: List<Room> = listOf()
    var blood: Room? = null

    fun scan(world: Level) {
        if (loadedAllRooms) {
            scanDoors(world)

            allSecrets = rooms.sumOf { it.data?.secrets ?: 0 }
            return
        }

        val consumedMapItemRooms = scanRooms(world)

        puzzles = rooms.filter { it.type == Room.Type.PUZZLE }

        // TODO: should update rooms to extend them based off their shape if possible

        val updatedRoomRotations = scanRoomRotations(world)
        scanDoors(world)

        loadedAllRooms = DungeonMap.mapSize?.let {
            if (SpecialColumn.column != -1 && rooms.count { room -> room.type == Room.Type.PUZZLE && room.rotation != Room.Rotation.NONE } != Scoreboard.stats.puzzleCount) return@let false

            val zMax = if (SpecialColumn.column != -1) it.z - 1 else it.z
            for (x in 0 until it.x) {
                for (z in 0 until zMax) {
                    val owner = roomsList[Vec2i(x, z).roomListIndex()].owner ?: return@let false
                    if (owner.data == null) return@let false
                    if (owner.rotation == Room.Rotation.NONE) return@let false
                }
            }

            return@let true
        } ?: false

        if (updatedRoomRotations.isNotEmpty() || consumedMapItemRooms.isNotEmpty()) {
            updatedRoomRotations.addAll(consumedMapItemRooms)
            MapEvents.ON_LOADED_ROOMS.invoker().loadedRooms(updatedRoomRotations)
        }
    }

    private val horizontals = Direction.entries.filter { it.axis.isHorizontal }

    private fun updateRotation(level: Level, room: Room): Boolean {
        if (room.data == null) return false

        if (updateRotationOffShape(room)) return true

        if (room.height == null) return false

        if (room.data!!.name == "Fairy") { // Fairy room doesn't have a clay block so we need to set it manually
            room.clayPos = room.tiles.firstOrNull()?.let { BlockPos(it.pos.x - 15, room.height!!, it.pos.z - 15) } ?: return false
            room.rotation = Room.Rotation.SOUTH
            return true
        }

        // for 4x1's the shape extends beyond just 1 tile so we need to make sure we've loaded everything
        // before scanning for the rotation
        if (room.shape == Room.Shape.S4x1 && room.tiles.size != 4) return false

        // Clay Position rotation scanning copied off odin, I should make this work better in the case of 4x1s where
        // it would look for 2 consecutive tiles with the clay position.
        room.rotation = Room.Rotation.entries.dropLast(1).find { rotation ->
            room.tiles.any { component ->
                BlockPos(component.pos.x + rotation.pos.x, room.height!!, component.pos.z + rotation.pos.z).let { blockPos ->
                    level.getBlockState(blockPos).block == Blocks.DYED_TERRACOTTA.blue && (room.tiles.size == 1 || horizontals.all { facing ->
                        level.getBlockState(
                            blockPos.offset((if (facing.axis == Direction.Axis.X) facing.stepX else 0), 0, (if (facing.axis == Direction.Axis.Z) facing.stepZ else 0))
                        ).block.equalsAny(Blocks.AIR, Blocks.DYED_TERRACOTTA.blue)
                    }).also { isCorrectClay -> if (isCorrectClay) room.clayPos = blockPos }
                }
            }
        } ?: Room.Rotation.NONE

        return room.rotation != Room.Rotation.NONE
    }

    fun updateRotationOffShape(room: Room): Boolean {
        if (room.shape == Room.Shape.S1x1 || room.tiles.size != room.shape.tileCount) {
            return false
        }

        val topLeft = room.tiles.minBy { it.pos.x * 1000 + it.pos.z }
        val bottomRight = room.tiles.maxBy { it.pos.x * 1000 + it.pos.z }

        when (room.shape) {
            Room.Shape.SL -> {
                val other = room.tiles.find { it != topLeft && it != bottomRight }!!

                if (topLeft.pos.x == bottomRight.pos.x) {
                    // top left is missing
                    room.clayPos = BlockPos(other.pos.x - 15, 0, topLeft.pos.z + 15)
                    room.rotation = Room.Rotation.EAST
                } else if (topLeft.pos.z == bottomRight.pos.z) {
                    // bottom right is missing
                    room.clayPos = BlockPos(bottomRight.pos.x + 15, 0, bottomRight.pos.z - 15)
                    room.rotation = Room.Rotation.WEST
                } else if (other.pos.x == topLeft.pos.x) {
                    // top right missing
                    room.clayPos = BlockPos(topLeft.pos.x - 15, 0, topLeft.pos.z - 15)
                    room.rotation = Room.Rotation.SOUTH
                } else {
                    // bottom left is missing
                    room.clayPos = BlockPos(bottomRight.pos.x + 15, 0, bottomRight.pos.z + 15)
                    room.rotation = Room.Rotation.NORTH
                }
            }
            else -> {
                if (topLeft.pos.x == bottomRight.pos.x) {
                    room.clayPos = BlockPos(topLeft.pos.x + 15, 0, topLeft.pos.z - 15)
                    room.rotation = Room.Rotation.WEST
                } else {
                    // this also handles 2x2s.
                    room.clayPos = BlockPos(topLeft.pos.x - 15, 0, topLeft.pos.z - 15)
                    room.rotation = Room.Rotation.SOUTH
                }
            }
        }

        return true
    }

    private fun scanRooms(world: Level): MutableList<Room> {
        val fullyScannedRooms = mutableListOf<Room>()

        for (x in 0 .. 5) {
            for (z in 0 .. 5) {
                val place = Vec2i(x, z)
                val curr = topLeftRoom.add(Vec2i(x, z).multiply(32))
                val chunk = world.chunkSource.getChunk(curr.x shr 4, curr.z shr 4, ChunkStatus.FULL, false) ?: continue

                // this should take care of checking whether the full map is loaded or not

                val (core, height) = calculateCore(chunk, curr) ?: continue

                // core for air.
                if (core == 48696) {
                    continue
                }

                val room = RoomData.getRoomData(core)
                if (room != null) {
                    var found = rooms.find { it.data?.name == room.name }
                    if (found?.tiles?.any { it.equals(curr) } == true) {
                        continue
                    }

                    val bottomY = getBottomY(chunk, curr) ?: continue

                    // the logic for rooms that do not have data.
                    roomsList[place.roomListIndex()].owner?.let { mapItemRoom ->
                        if (mapItemRoom == found) continue

                        // mapItemRoom is considered map item because it doesn't have data associated with it,
                        // and is not unknown(unknown rooms don't have owners).

                        // if map item room has 1 tile then that must mean that we're scanning the same tile right now
                        // in that case we can just consume the map item room since it won't have any data at all.
                        // we don't do 1x1 rotation scanning based off doors or anything, the only useful thing about
                        // 1x1 map item rooms is the doors associated with them.
                        if (found != null && mapItemRoom.tiles.size == 1) {
                            rooms.remove(mapItemRoom)
                            found.doors.addAll(mapItemRoom.doors)
                            val tile = found.roomTile(curr)

                            doors.filter { door -> door.rooms.any { it.owner!! == mapItemRoom } }.forEach { door ->
                                door.rooms.removeIf { it.owner!! == mapItemRoom }
                                if (tile != null) door.rooms.add(tile)
                            }

                            continue
                        }

                        // in case it's not a 1x1 that means it is a fully scanned bigger room and that guarantees:
                        // shape, type, rotation, claypos, doors. it also guarantees that this is the first collision
                        // with a world scanning tile, that's because on the first collision we just grab all the data
                        // and the room stops being just a map item room.
                        // if it's the first collision then we might as well just bring over all the data to the map
                        // item room instead of making a new world scanning room.
                        mapItemRoom.data = room
                        mapItemRoom.type = room.type
                        mapItemRoom.shape = room.shape
                        mapItemRoom.height = height
                        mapItemRoom.floorHeight = bottomY

                        // this also means the room is fully scanned now, we have its rotation so we can fire the
                        // loaded room event with it.
                        if (mapItemRoom.clayPos != null) fullyScannedRooms.add(mapItemRoom)

                        continue
                    }

                    if (found == null) {
                        found = Room(room, height, bottomY)
                        rooms.add(found)
                    }

                    found.roomTile(curr)
                } else {
                    // Chat.send("hello: $core")
                }
            }
        }

        return fullyScannedRooms
    }

    private fun scanRoomRotations(level: Level): MutableList<Room> {
        val updated = mutableListOf<Room>()
        rooms.forEach { room ->
            if (room.rotation != Room.Rotation.NONE) return@forEach

            if (updateRotation(level, room) && room.data != null) updated.add(room)
        }

        return updated
    }

    private fun scanDoors(world: Level) {
        val handleDoor = fun(pos: Vec2i, rooms: MutableList<Room.Tile>) {
            if (rooms.size != 2) {
                return
            }

            if (doors.any { it.pos == pos }) return

            val chunk = world.getChunk(pos.x shr 4, pos.z shr 4)
            val height = getTopY(chunk, pos) ?: return

            if (!arrayOf(73, 81).any { it == height }) {
                if (height <= 73) {
                    return
                }

                // sometimes the entrance door has blocks above it? for no apparent reason.
                if (rooms.any { it.owner!!.type == Room.Type.ENTRANCE }) {
                    val tile = Door(pos, Door.Type.NORMAL, rooms)
                    doors.add(tile)
                    return
                }

                return
            }

            val type = when (world.getBlockState(BlockPos(pos.x, 69, pos.z)).block) {
                Blocks.COAL_BLOCK -> {
                    rooms.forEach { it.owner!!.rushRoom = true }
                    Door.Type.WITHER
                }
                Blocks.DYED_TERRACOTTA.red -> Door.Type.BLOOD
                else -> Door.Type.NORMAL
            }

            val tile = Door(pos, type, rooms)
            doors.add(tile)

            return
        }

        for (a in 0 .. 5) {
            for (b in 0 .. 4) {
                // going right
                val goingRight = Vec2i(
                    topLeftRoom.x + a * 32,
                    topLeftRoom.z + 16 + 32 * b
                )

                val rooms1 = listOf(
                    roomsList[Vec2i(a, b).roomListIndex()],
                    roomsList[Vec2i(a, b + 1).roomListIndex()]
                ).filter { it.owner != null }.toMutableList()

                handleDoor(goingRight, rooms1)

                // going down
                val goingDown = Vec2i(goingRight.z, goingRight.x)

                val rooms2 = listOf(
                    roomsList[Vec2i(b, a).roomListIndex()],
                    roomsList[Vec2i(b + 1, a).roomListIndex()]
                ).filter { it.owner != null }.toMutableList()

                handleDoor(goingDown, rooms2)
            }
        }
    }

    fun parseOdinMessage(obj: JsonObject) {
        if (!obj.has("playerName") || !obj.has("roomName")) return
        val player = obj["playerName"].asString
        if (Scoreboard.dungeonTeammates.none { it.name == player }) return
        val foundSecrets = if (obj.has("foundSecrets")) obj["foundSecrets"].asInt else return
        val name = obj["roomName"].asString
        val room = rooms.find { it.data?.name == name } ?: return
        room.secretsDone = foundSecrets
    }
}