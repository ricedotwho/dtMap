package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.darker
import com.ricedotwho.dtmap.config.MapConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents

// this is mostly used to track types of 1x1 rooms & whether the special column is fully scanned so we can mark the
// map as fully scanned.
object SpecialColumn {
    var column = -1

    // discoveredFullSpecialColumn is the count of tiles not in the special column but in the column right before it,
    // it's used to determine whether the entire special column is discovered so we can do guesses that require that
    // knowledge
    var discoveredFullSpecialColumn = 0

    var columnRoomCount = 0

    // discovered1x1s is only increased when we know for sure it's a 1x1, it's a legit property that we don't scan for.
    var discovered1x1s = 0

    var opened1x1s = mutableSetOf<Room>()

    fun register() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            column = -1
            discoveredFullSpecialColumn = 0
            discovered1x1s = 0
            opened1x1s.clear()
            columnRoomCount = 0
        }
    }

    fun updateSpecialColumn() {
        // handle all the 1x1s on the map - if there's rooms that are either in the 1x1 column or have just 1 door
        // then we should consider them 1x1s and should try to guess what they are based off that.

        for (x in 0 until DungeonMap.mapSize!!.x) {
            for (z in 0 until DungeonMap.mapSize!!.z) {
                val roomListIndex = Vec2i(x, z).roomListIndex()
                val room = Scan.roomsList[roomListIndex]
                val owner = room.owner ?: continue
                if (owner.tiles.size != 1) continue
                if (owner.type == Room.Type.BLOOD || owner.type == Room.Type.ENTRANCE) continue

                if (owner.state == Room.State.UNOPENED) {
                    if (x == column) {
                        if (!owner.isKnown1x1) {
                            owner.isKnown1x1 = true
                            discovered1x1s++
                            columnRoomCount++
                        }
                        continue
                    }
                    val checkRoom = fun(offsetX: Int, offsetZ: Int): Boolean {
                        val offset = Scan.roomsList[Vec2i(offsetX, offsetZ).roomListIndex()]

                        val owner = offset.owner ?: return false
                        if (owner.state == Room.State.UNDISCOVERED) return false
                        if (owner.state == Room.State.UNOPENED && Vec2i(offsetX, offsetZ) != owner.entryTile) return false

                        return true
                    }

                    if (x != DungeonMap.mapSize!!.x - 1 && x + 1 != column && !checkRoom(x + 1, z)) continue
                    if (x != 0 && !checkRoom(x - 1, z)) continue
                    if (z != DungeonMap.mapSize!!.z - 1 && !checkRoom(x, z + 1)) continue
                    if (z != 0 && !checkRoom(x, z - 1)) continue
                    // now we know that all the other rooms are seen &
                    if (!owner.isKnown1x1) {
                        owner.isKnown1x1 = true
                        discovered1x1s++
                    }
                } else if (owner.state != Room.State.UNDISCOVERED && owner.state != Room.State.UNOPENED &&
                    (owner.type == Room.Type.CHAMPION || owner.type == Room.Type.TRAP || owner.type == Room.Type.PUZZLE)
                ) {
                    opened1x1s.add(owner)
                }
            }
        }

        if (discovered1x1s == Scoreboard.stats.puzzleCount + 2) {
            discoveredFullSpecialColumn = DungeonMap.mapSize!!.z
        } else if (column != -1) {
            var discovered = DungeonMap.mapSize!!.z
            for (z in 0 until DungeonMap.mapSize!!.z) {
                val tile = Scan.roomsList[Vec2i(column - 1, z).roomListIndex()]
                val owner = tile.owner ?: continue
                if (owner.isKnown1x1) continue
                if ((owner.state == Room.State.UNOPENED && owner.type != Room.Type.BLOOD) || owner.state == Room.State.UNDISCOVERED) discovered--
            }
            discoveredFullSpecialColumn = discovered
        }
    }

    fun roomColorGuess(room: Room): Array<Color> {
        // if there's a special column it must have at least 1 room in it.
        val specialColumnRoomCount = if (column != -1 && columnRoomCount == 0) 1 else columnRoomCount

        val mapSizeZ = DungeonMap.mapSize?.z ?: 6
        if (room.specialTile) {
            val specialSize = mapSizeZ - discoveredFullSpecialColumn + specialColumnRoomCount
            return if (specialSize <= Scoreboard.stats.puzzleCount || discovered1x1s - specialColumnRoomCount >= 2) {
                arrayOf(MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (opened1x1s.any { it.type == Room.Type.TRAP && !it.specialTile }) {
                arrayOf(MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (specialSize == Scoreboard.stats.puzzleCount + 1) {
                arrayOf(MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (discovered1x1s - specialColumnRoomCount == 1) {
                arrayOf(MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (opened1x1s.any { it.type == Room.Type.CHAMPION }) {
                arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier))
            } else {
                arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
            }
        }

        val nonspecialPuzzles = opened1x1s.count { it.type == Room.Type.PUZZLE && !it.specialTile }
        val totalPuzzles = specialColumnRoomCount + nonspecialPuzzles
        if (totalPuzzles == Scoreboard.stats.puzzleCount) {
            return if (opened1x1s.any { it.type == Room.Type.TRAP }) {
                arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (opened1x1s.any { it.type == Room.Type.CHAMPION }) {
                arrayOf(MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier))
            } else {
                arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier), MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier))
            }
        }

        // yellow is generated first so if the special column has puzzles + 1 then the 1x1 on the map is
        // yellow.
        if (!room.specialTile && specialColumnRoomCount == Scoreboard.stats.puzzleCount + 1) {
            return arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier))
        }

        // if we've opened both trap & yellow then we know the 1x1 is a puzzle
        if (opened1x1s.count { it.type == Room.Type.CHAMPION || it.type == Room.Type.TRAP } == 2) {
            return arrayOf(MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
        }

        // if we've opened puzzle count + 1 then we're left with just 1 unopened room, and we can guess
        // whether it's trap or yellow (can't be a puzzle we already have a check for whether both trap
        // and yellow are open and that returns early)
        if (opened1x1s.size == Scoreboard.stats.puzzleCount + 1) {
            if (opened1x1s.none { it.type == Room.Type.TRAP }) {
                return arrayOf(MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier))
            } else if (opened1x1s.none { it.type == Room.Type.CHAMPION }) {
                return arrayOf(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier))
            }
        }

        val result = mutableListOf(MapConfig.puzzleRoomColor.darker(MapConfig.darkenMultiplier))
        if (opened1x1s.none { it.type == Room.Type.TRAP }) {
            result.add(MapConfig.trapRoomColor.darker(MapConfig.darkenMultiplier))
        }

        if (opened1x1s.none { it.type == Room.Type.CHAMPION }) {
            result.add(MapConfig.championRoomColor.darker(MapConfig.darkenMultiplier))
        }

        return result.toTypedArray()
    }
}