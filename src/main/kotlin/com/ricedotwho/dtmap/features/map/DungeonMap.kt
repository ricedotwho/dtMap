package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.ricedotwho.dtmap.events.MapEvents
import com.ricedotwho.dtmap.features.RedstoneKeySkullHighlight
import com.ricedotwho.dtmap.utils.Chat
import com.ricedotwho.dtmap.utils.DungeonMessages
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapId
import kotlin.jvm.optionals.getOrNull

object DungeonMap {
    fun register() {
        Score
        Scoreboard
        RedstoneKeySkullHighlight
        SpecialColumn.register()
        Scan.register()
        Mimic
        DoorEsp.register()
        Prince.register()

        ClientTickEvents.END_LEVEL_TICK.register { world ->
            if (shouldScan) {
                // TODO: stop scanning the entire world, start scanning based off loaded chunks
                Scan.scan(world)
                shouldScan = false
            }

            updatePlayerRoom()
        }
        ClientChunkEvents.CHUNK_LOAD.register(ChunkLoad)
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            mapID = null
            mapCenter = null
            shouldScan = false
            startCoords = null
            mapSize = null
            roomSize = null
        }
    }

    private var mapID: MapId? = null
    var mapCenter: Vec2i? = null
    var startCoords: Vec2i? = null
    var mapSize: Vec2i? = null
    var roomSize: Int? = null
    var shouldScan = false

    fun rescanMapItem(packet: ClientboundMapItemDataPacket) {
        if (DungeonListener.floor == null || DungeonMessages.dungeonEnded || (!DungeonMessages.seenDungeonStart && Scoreboard.stats.elapsedTime == "0s")) return

        // TODO: fix this to not just assume first map if still broken after seen dungeon start checks
        if (mapID == null) mapID = packet.mapId()

        if (mapID!!.id() != packet.mapId.id()) return

        val state = mc.level?.getMapData(packet.mapId) ?: return

        if (startCoords == null && !initializeSizes(state.colors)) return

        updatePlayerDecorations(packet)

        if (!Scan.loadedAllRooms) updateRoomTiles(state.colors)

        val updatedRooms = updateRoomState(state.colors)

        scanDoors(state.colors)

        Scan.doors.find { it.type == Door.Type.BLOOD }?.let {
            if (it.rooms.any { it.owner!!.type == Room.Type.BLOOD }) return@let null

            it.rooms.find { it.owner!!.doors.size == 1 && !it.owner.rushRoom && it.owner.shape == Room.Shape.S1x1 }?.let {
                it.owner!!.type = Room.Type.BLOOD
            }
        }

        SpecialColumn.updateSpecialColumn()

        // only send the event after the entire map item has been scanned to make sure that all data has been collected.
        if (updatedRooms.isNotEmpty()) mc.execute { MapEvents.ON_UPDATED_ROOMS.invoker().updatedRooms(updatedRooms) }
    }

    private fun initializeSizes(colors: ByteArray): Boolean {
        val (greenStart, greenLength) = run {
            var start = -1
            var length = 0

            for (i in colors.indices) {
                if (colors[i].toInt() == 30) {
                    if (length++ == 0) start = i
                } else {
                    if (length >= 16) return@run start to length
                    length = 0
                }
            }

            return@run start to length
        }

        if (greenLength != 16 && greenLength != 18) return false

        val (start, center, size) = when (Scoreboard.floor.number) {
            0 -> Triple(Vec2i(22, 22), Vec2i(-137, -137), Vec2i(4, 4))
            1 -> Triple(Vec2i(22, 11), Vec2i(-137, -121), Vec2i(4, 5))
            2, 3 -> Triple(Vec2i(11, 11), Vec2i(-121, -121), Vec2i(5, 5))
            else -> {
                val start = Vec2i((greenStart and 127) % (greenLength + 4), (greenStart shr 7) % (greenLength + 4))

                val extra = Vec2i(if (start.x == 5) 1 else 0, if (start.z == 5) 1 else 0)
                val size = Vec2i(5, 5).add(extra)
                val center = Vec2i(-121, -121).add(Vec2i(extra.x * 16, extra.z * 16))

                Triple(start, center, size)
            }
        }

        roomSize = greenLength
        startCoords = start
        mapCenter = center
        mapSize = size

        if (((Scoreboard.floor.number == 6 || Scoreboard.floor.number == 5) && size.x == 6 && size.z == 6) ||
            (Scoreboard.floor.number == 4 && size.x == 6 && size.z == 5)) {
            SpecialColumn.column = 5
        }

        return true
    }

    private fun updatePlayerDecorations(packet: ClientboundMapItemDataPacket) {
        val iter = Scoreboard.dungeonTeammatesNoSelf.iterator()

        packet.decorations.getOrNull()?.forEach { decor ->
            if (decor.type.value() == MapDecorationTypes.FRAME.value()) return@forEach

            lateinit var player: Scoreboard.DungeonPlayer
            do {
                if (!iter.hasNext()) return@forEach
                player = iter.next()
            } while (player.isDead)

            player.mapPos = Vec2i(decor.x.toInt(), decor.y.toInt())
            player.yaw = decor.rot() * 360 / 16.0F
        }
    }

    private fun updateRoomTiles(colors: ByteArray) {
        val rs = roomSize!!
        val sc = startCoords!!
        val ms = mapSize!!

        val rooms = Array(ms.x) { Array(ms.z) { -1 } }
        var roomIndex = 0
        for (i in 0 until ms.x) {
            for (j in 0 until ms.z) {
                val idx = Vec2i(i, j).multiply(rs + 4).add(sc).mapIndex()

                if (colors.size <= idx) continue

                if (colors[idx].toInt() != 0) {
                    rooms[i][j] = roomIndex++
                }
            }
        }

        var changed: Boolean
        do {
            changed = false

            for (a in 0..4) {
                for (b in 0..5) {
                    val door = rs + 1 + a * (rs + 4)
                    val midRoom = b * (rs + 4) + 1

                    val next = a + 1

                    val doorIndex = sc.add(Vec2i(door, midRoom)).mapIndex()
                    if (
                        colors.size > doorIndex &&
                        rooms.size > next && rooms[next].size > b &&
                        rooms[next][b] != rooms[a][b] &&
                        colors[doorIndex].toInt() != 0
                    ) {
                        rooms[next][b] = rooms[a][b]
                        changed = true
                    }

                    val doorIndex2 = sc.add(Vec2i(midRoom, door)).mapIndex()
                    if (
                        colors.size > doorIndex2 &&
                        rooms.size > b && rooms[b].size > next &&
                        rooms[b][next] != rooms[b][a] &&
                        colors[doorIndex2].toInt() != 0
                    ) {
                        rooms[b][next] = rooms[b][a]
                        changed = true
                    }
                }
            }
        } while (changed)

        // debug printing only works for f7 because it expects symmetrical map
        /*
        for (i in 0 until rooms.size) {
            for (j in 0 until rooms.size) {
                print("${rooms[j][i]}".padStart(3))
            }
            println()
        }
        */

        data class Unique(val id: Int, val coords: Vec2i, val tiles: MutableList<Vec2i>)
        val uniques = mutableListOf<Unique>()
        rooms.forEachIndexed { i, it ->
            it.forEachIndexed { j, roomID ->
                if (roomID == -1) return@forEachIndexed
                (uniques.find { unique -> unique.id == roomID } ?: run {
                    val added = Unique(roomID, Vec2i(i, j), mutableListOf())
                    uniques.add(added)
                    added
                }).tiles.add(Vec2i(i, j))
            }
        }

        val fullyLoadedRooms = mutableListOf<Room>()
        uniques.forEach { unique ->
            val idx = unique.coords.multiply(rs + 4).add(sc).mapIndex()
            val type = when (val color = colors[idx].toInt()) {
                18 -> Room.Type.BLOOD
                30 -> Room.Type.ENTRANCE
                85 -> Room.Type.UNKNOWN
                63 -> Room.Type.NORMAL
                62 -> Room.Type.TRAP
                66 -> Room.Type.PUZZLE
                74 -> Room.Type.CHAMPION
                82 -> Room.Type.FAIRY
                else -> {
                    mc.execute { Chat.send("color: $color coords: ${unique.coords}") }
                    return@forEach
                }
            }

            val shape =
                if (type == Room.Type.UNKNOWN) Room.Shape.UNKNOWN
                else when (unique.tiles.size) {
                    1 -> Room.Shape.S1x1
                    2 -> Room.Shape.S2x1
                    3 -> {
                        val (a, b, c) = unique.tiles
                        if ((a.x == b.x && a.x == c.x) || (a.z == b.z && a.z == c.z)) Room.Shape.S3x1 else Room.Shape.SL
                    }
                    4 -> {
                        // just copied over the same logic as in 3 because if there's 3 in a row it's definitely not 2x2
                        val (a, b, c) = unique.tiles
                        if ((a.x == b.x && a.x == c.x) || (a.z == b.z && a.z == c.z)) Room.Shape.S4x1 else Room.Shape.S2x2
                    }
                    else -> throw IllegalStateException("Found a room with more than 4 tiles: ${unique.tiles.size}")
                }

            synchronized(Scan.rooms) {
                var shouldUpdateRotation = false
                val found = unique.tiles.firstNotNullOfOrNull { tile ->
                    val room = Scan.roomsList[tile.roomListIndex()].owner ?: return@firstNotNullOfOrNull null
                    unique.tiles.forEach {
                        Scan.roomsList[it.roomListIndex()].owner?.let { owner ->
                            if (owner != room) {
                                owner.doors.addAll(room.doors)
                                Scan.rooms.remove(owner)
                            }
                        }

                        if (!room.places.contains(it)) {
                            room.roomTile(it.multiply(32).add(-185, -185))
                            shouldUpdateRotation = true
                        }
                    }
                    room
                }

                if (found != null) {
                    if ((type != Room.Type.UNKNOWN && found.type == Room.Type.UNKNOWN) ||
                        (shape != Room.Shape.UNKNOWN && found.shape == Room.Shape.UNKNOWN)
                    ) {
                        found.type = type
                        found.shape = shape
                        shouldUpdateRotation = true
                    }

                    if (shouldUpdateRotation && Scan.updateRotationOffShape(found) && found.data != null) {
                        fullyLoadedRooms.add(found)
                    }

                    return@forEach
                }

                val room = Room(type, shape)
                Scan.rooms.add(room)
                unique.tiles.forEach { room.roomTile(it.multiply(32).add(-185, -185)) }
                if (type != Room.Type.UNKNOWN) Scan.updateRotationOffShape(room)
            }
        }

        if (rooms.isNotEmpty()) MapEvents.ON_LOADED_ROOMS.invoker().loadedRooms(fullyLoadedRooms)
    }

    private fun updateRoomState(colors: ByteArray): List<Room.StateUpdated> {
        val updatedRooms = mutableListOf<Room.StateUpdated>()

        val sc = startCoords!!.add(roomSize!! / 2, roomSize!! / 2)
        val tile = roomSize!! + 4
        Scan.rooms.forEach { room ->
            val placementColor = fun(placement: Vec2i): Byte {
                val center = sc.add(placement.multiply(tile))
                val mapIndex = center.mapIndex()
                if (colors.size <= mapIndex) {
                    Chat.send("send this to mi0: $mapIndex => ${colors.size}")
                    return 0
                }

                return colors[mapIndex]
            }

            val topLeft = room.places.minWith { a, b -> a.x * 1000 + a.z - b.x * 1000 - b.z }
            var placement = topLeft
            var color = placementColor(placement)
            if (color == 0.toByte()) {
                // the room isn't on the map yet, and although we know its top placement
                // that's not enough because only the close part is shown, let's try all placements
                val newColor = room.places.firstNotNullOfOrNull {
                    val color = placementColor(it)
                    if (color == 0.toByte()) return@firstNotNullOfOrNull null
                    return@firstNotNullOfOrNull Pair(it, color)
                }
                if (newColor != null) {
                    placement = newColor.first
                    color = newColor.second
                }
            }

            val updated = room.updateState(placement, color.toInt()) ?: return@forEach
            updatedRooms.add(updated)
        }

        return updatedRooms
    }

    private fun scanDoors(colors: ByteArray) {
        val handleDoor = fun(pos: Vec2i, place: Vec2i, offset: Vec2i, color: Byte) {
            val intColor = color.toInt()
            val type = when (intColor) {
                // locked wither or locked fairy, fairy
                119, 82 -> Door.Type.WITHER
                // normal, fairy, puzzle, normal but unopened, trap, champion
                63, 66, 85, 62, 74 -> Door.Type.NORMAL
                // blood, stays the same color even when unlocked
                18 -> Door.Type.BLOOD
                // if it's not on the map no point in creating a door
                0 -> return
                // I don't think there's any other colors that we're supposed to handle
                else -> throw IllegalStateException("found a color for a door that doesn't exist: ${color.toInt()}")
            }

            val rooms = listOfNotNull(
                Scan.roomsList[place.roomListIndex()],
                Scan.roomsList[place.add(offset).roomListIndex()],
            ).filter {
                it.owner != null
            }.toMutableList()

            val door = Scan.doors.find { it.pos == pos } ?: let {
                val door = Door(pos, type, rooms)
                Scan.doors.add(door)
                door
            }

            if (type == Door.Type.WITHER && door.type != Door.Type.WITHER) {
                door.type = Door.Type.WITHER
                door.locked = true
            }

            if (type == Door.Type.WITHER || type == Door.Type.BLOOD) {
                door.rooms.forEach { it.owner!!.rushRoom = true }
            }

            if (type == Door.Type.NORMAL || intColor == 82) {
                door.locked = false
            }

            if (intColor == 18) {
                door.locked = !DoorEsp.bloodOpened
            }
        }

        val rs = roomSize ?: return
        val hrs = rs / 2
        val sc = startCoords!!.add(Vec2i(hrs, hrs))

        for (a in 0..4) {
            for (b in 0..5) {
                val door = hrs + a * (rs + 4)
                val midRoom = b * (rs + 4)

                val coordsDoor = Scan.topLeftRoom.x + 16 + (a * 32)
                val coordsMidRoom = Scan.topLeftRoom.z + (b * 32)

                // the room index is the index of the gap between 2 rooms, if there's a door the gap has 0 as a color
                // we need that to make sure that we're looking at a door and not just a connection between tiles in
                // a bigger room
                val doorIndex = sc.add(Vec2i(door, midRoom)).mapIndex()
                val roomIndex = sc.add(Vec2i(door, midRoom - hrs + 1)).mapIndex()
                if (colors.size > doorIndex && colors[roomIndex] == 0.toByte()) {
                    handleDoor(Vec2i(coordsDoor, coordsMidRoom), Vec2i(a, b), Vec2i(1, 0), colors[doorIndex])
                }

                val doorIndex2 = sc.add(Vec2i(midRoom, door)).mapIndex()
                val roomIndex2 = sc.add(Vec2i(midRoom - hrs + 1, door)).mapIndex()
                if (colors.size > doorIndex2 && colors[roomIndex2] == 0.toByte()) {
                    handleDoor(Vec2i(coordsMidRoom, coordsDoor), Vec2i(b, a), Vec2i(0, 1), colors[doorIndex2])
                }
            }
        }
    }

    val ChunkLoad = ClientChunkEvents.Load { _, _ ->
        if (LocationUtils.currentArea == Island.Dungeon && DungeonListener.floor != null) {
            shouldScan = true
            if (mapSize == null) {
                mapSize = when (DungeonListener.floor!!.floorNumber) {
                    0 -> Vec2i(4, 4)
                    1 -> Vec2i(4, 5)
                    2, 3 -> Vec2i(5, 5)
                    else -> Vec2i(6, 6)
                }
            }
        }
    }

    fun calculateMapSize(): Vec2i =
        if (mapSize != null) mapSize!! else Vec2i(6, 6)

    private var playerRoom: Room.Tile? = null
    private var playerTile: Vec2i? = null
    private var playerOutOfBounds: Boolean = false

    private fun updatePlayerRoom() {
        val player = mc.player ?: return

        val oldPlayerOutOfBounds = playerOutOfBounds
        val oldPlayerRoom = playerRoom

        scanPlayerRoom()

        if (playerRoom == null) {
            playerOutOfBounds = false
        } else {
            playerRoom?.let { playerRoom ->
                playerOutOfBounds = playerRoom.owner?.height == null || playerRoom.owner.height!! < player.y ||
                        playerRoom.owner.floorHeight == null || playerRoom.owner.floorHeight!! > player.y
            }
        }

        if (oldPlayerOutOfBounds != playerOutOfBounds || oldPlayerRoom != playerRoom) {
            val roomEntered = if (playerOutOfBounds) null else playerRoom
            MapEvents.ON_PLAYER_ENTER_ROOM.invoker().enterRoom(roomEntered)
        }
    }

    private fun scanPlayerRoom() {
        if (LocationUtils.currentArea != Island.Dungeon) {
            playerRoom = null
            playerOutOfBounds = false
            return
        }

        val player = mc.player
        if (player == null) {
            playerRoom = null
            playerOutOfBounds = false
            return
        }

        val playerPosition = Vec2i(player.blockX, player.blockZ)

        val moduloX = (player.blockX + 201) % 32
        val moduloZ = (player.blockZ + 201) % 32
        if (moduloX == 0 || moduloZ == 0) return

        val newPlayerTile = playerPosition.add(201, 201).divide(32)
        if (newPlayerTile == playerTile) return

        val newPlayerRoom = roomIn(playerPosition)
        if (newPlayerRoom == null) {
            playerRoom = null
            playerOutOfBounds = false
            return
        }

        playerRoom = newPlayerRoom
    }

    fun roomIn(pos: Vec2i): Room.Tile? {
        val index = pos.index()
        if (index !in 0..35) return null
        return Scan.roomsList[pos.index()]
    }

    fun roomPlayerIn(): Room.Tile? =
        if (playerOutOfBounds) null else playerRoom

    fun roomPlayerInNoBoundsCheck(): Room.Tile? =
        playerRoom

    fun roomPlayerInCheckName(name: String): Boolean =
        playerRoom?.owner?.data?.name == name
}