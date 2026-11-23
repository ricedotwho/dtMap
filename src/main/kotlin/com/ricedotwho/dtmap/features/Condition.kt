package com.ricedotwho.dtmap.features

import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.utils.DungeonMessages

enum class Condition(val predicate: () -> Boolean) {
    ALWAYS({ true }),
    BOSS({ DungeonMessages.inBoss }),
    F7_BOSS({ DungeonMessages.inBoss && Scoreboard.floor.number == 7 }),
    CLEAR({ LocationUtils.currentArea == Island.Dungeon && (DungeonMessages.seenDungeonStart || Scoreboard.stats.elapsedTime != "0s") && !DungeonMessages.inBoss }),
    BEFORE_MORT({ LocationUtils.currentArea == Island.Dungeon && !(DungeonMessages.seenDungeonStart || Scoreboard.stats.elapsedTime != "0s") && !DungeonMessages.inBoss }),
    ALT({ DtMap.keybindShowHud.isDown })
}