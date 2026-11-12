package com.ricedotwho.dtmap.config.dungeon

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.features.Module

@AlwaysActive
object SoloClearConfig : Module(
    name = "dtSoloClearing",
    description = "downtime other"
) {
    val soloClearing by SelectorSetting("Solo Clearing", SoloClearing.AUTO, desc = "")
    val soloClearHudInsight by BooleanSetting("Hud Insight Peek", false, desc = "")
    val soloClearingInstaJoin by BooleanSetting("Insta Join On Menu Open", false, desc = "")
    val soloClearingOnlyShowCheckMark by BooleanSetting("Only show checkmarks", false, desc = "only show checkmarks when soloclearing")
    val soloClearingInstantRoomUpdate by BooleanSetting("Instant room update", true, desc = "")
    val soloClearingDoorEsp by BooleanSetting("Door Esp", false, desc = "")
}

enum class SoloClearing {
    NO,
    YES,
    AUTO
}