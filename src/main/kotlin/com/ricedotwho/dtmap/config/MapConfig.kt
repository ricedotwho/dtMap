package com.ricedotwho.dtmap.config

import com.mojang.blaze3d.Blaze3D
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.features.BloodRush
import com.ricedotwho.dtmap.features.Condition
import com.ricedotwho.dtmap.features.PuzzleHud
import com.ricedotwho.dtmap.features.map.MapRenderer
import com.ricedotwho.dtmap.features.map.Score
import com.ricedotwho.dtmap.utils.Colours
import com.ricedotwho.dtmap.utils.MapImageLoader
import com.ricedotwho.dtmap.utils.dtHUD

object MapConfig : Module(
    name = "dtMap",
    description = "downtime map",
    category = DtMap.dtMapCat,
) {

    // settings
    val legitMode by BooleanSetting("Legit", true, desc = "")
    val odinWS by BooleanSetting("Listen to Odin websocket", true, desc = "")
    val scoreCalculation by SelectorSetting("Score Calculation", ScoreCalculation.NONE, desc = "")
    val scoreMaxBonusForMissingSecrets by BooleanSetting("Max bonus for missing/needed", false, desc = "")
    val scoreNeededSecretsInsteadOfMissing by BooleanSetting("Replace missing with needed", false, desc = "")
    val scoreMissingSecretsCalculatePaul by BooleanSetting("Missing secrets calculate paul", false, desc = "")

    val bloodRushInstaclearNotification by BooleanSetting("Blood Rush Instaclear Sound", false, desc = "")

    val score270MessageEnabled by BooleanSetting("270 Message", false, desc = "")
    val score270Message by StringSetting("270 Text", "270", placeholder = "270", desc = "").withDependency { score270MessageEnabled }
    val score300MessageEnabled by BooleanSetting("300 Message", false, desc = "")
    val score300Message by StringSetting("300 Text", "300", placeholder = "300", desc = "").withDependency { score300MessageEnabled }
    val scorePaul by SelectorSetting("Paul", AutoDE.AUTO, desc = "")

    // room text
    val textScaling by NumberSetting("Text Scaling ", 0.45F, 0.01..1.5, increment = 0.01, desc = "")
    val textCenter by BooleanSetting("Center Text", true, desc = "")
    val undiscoveredQuestionMarks by BooleanSetting("Uncovered Question Marks", false, desc = "")

    // huds

    val puzzleHud by dtHUD("dtPuzzle Hud", default = mutableListOf(Condition.BEFORE_MORT, Condition.ALT)) {
        PuzzleHud.render(this, it)
    }

    val map by dtHUD("dtMap", default = mutableListOf(Condition.BEFORE_MORT, Condition.ALT, Condition.CLEAR)) {
        MapRenderer.render(this, it)
    }

    val score by dtHUD("dtScore", default = mutableListOf(Condition.CLEAR, Condition.ALT)) {
        Score.render(this, it)
    }

    val rushHud by dtHUD("dtRush Hud", default = mutableListOf(Condition.ALT)) {
        BloodRush.render(this, it)
    }

    // class colors
    val classColours by DropdownSetting("Class Colours", desc = "")
    val archerColour by ColorSetting("Archer Colour", Color(255, 170, 0), desc = "").withDependency { classColours }
    val mageColor by ColorSetting("Mage Colour", Color(85, 255, 255), desc = "").withDependency { classColours }
    val berserkColor by ColorSetting("Berserk Colour", Color(255, 85, 85), desc = "").withDependency { classColours }
    val tankColor by ColorSetting("Tank Colour", Color(0, 170, 0), desc = "").withDependency { classColours }
    val healerColor by ColorSetting("Healer Colour", Color(255, 170, 0), desc = "").withDependency { classColours }
    val classOutlineColour by BooleanSetting("Use class outline colour", true, desc = "").withDependency { classColours }
    val playerHeadBackground by ColorSetting("Outline Colour", Colours.BLACK, desc = "").withDependency { classColours }
    val playerHeadOwnBackground by ColorSetting("Own Outline Colour", Colors.MINECRAFT_RED.withAlpha(0.7f), allowAlpha = true, desc = "").withDependency { classColours }
    val playerHeadBackgroundSize by NumberSetting("Outline Size", 1, 0.01..5.0, increment = 0.01, desc = "").withDependency { classColours }


    val playerHeadDrawOwnLast by BooleanSetting("Draw Own Last", false, desc = "")
    val playerMCMapPointer by BooleanSetting("MC Map Pointer", false, desc = "")
    val playerNamesScaling by NumberSetting("Name Scaling", 0.75F, 0.01..1.5, increment = 0.01, desc = "")
    val playerNameColor by ColorSetting("Player Name Colour", Color(70, 70, 70), desc = "")

    // rooms additions
    val roomAdditionsPrince by BooleanSetting("Render Prince", false, desc = "")

    // colours
    val doorColours by DropdownSetting("Door Colours", desc = "")
    val doorThicknessF by NumberSetting("Door Thickness", 9F, 0.01..10.0, increment = 0.01, desc = "").withDependency { doorColours }
    val unopenedDoorColor by ColorSetting("Unopened Door", Color(30, 30, 30), desc = "").withDependency { doorColours }
    val bloodDoorColor by ColorSetting("Blood Door", Color(255, 0, 0), desc = "").withDependency { doorColours }
    val witherDoorColor by ColorSetting("Wither Door", Color(0, 0, 0), desc = "").withDependency { doorColours }
    val normalDoorColor by ColorSetting("Normal Door", Color(107, 58, 17), desc = "").withDependency { doorColours }
    val puzzleDoorColor by ColorSetting("Puzzle Door", Color(117, 0, 133), desc = "").withDependency { doorColours }
    val championDoorColor by ColorSetting("Champion Door", Color(254, 223, 0), desc = "").withDependency { doorColours }
    val trapDoorColor by ColorSetting("Trap Door", Color(216, 127, 51), desc = "").withDependency { doorColours }
    val entranceDoorColor by ColorSetting("Entry Door", Color(20, 133, 0), desc = "").withDependency { doorColours }
    val fairyDoorColor by ColorSetting("Fairy Door", Color(244, 19, 139), desc = "").withDependency { doorColours }
    val rareDoorColor by ColorSetting("Rare Door", Color(255, 203, 89), desc = "").withDependency { doorColours }

    val roomColours by DropdownSetting("Room Colours", desc = "")
    val darkenMultiplier by NumberSetting("Darken Multiplier", 0.4F, 0.01..1.0, increment = 0.01, desc = "").withDependency { roomColours }
    val unopenedRoomColor by ColorSetting("Unopened Room", Color(30, 30, 30), desc = "").withDependency { roomColours }
    val bloodRoomColor by ColorSetting("Blood Room", Color(255, 0, 0), desc = "").withDependency { roomColours }
    val normalRoomColor by ColorSetting("Normal Room", Color(107, 58, 17), desc = "").withDependency { roomColours }
    val puzzleRoomColor by ColorSetting("Puzzle Room", Color(117, 0, 133), desc = "").withDependency { roomColours }
    val championRoomColor by ColorSetting("Champion Room", Color(254, 223, 0), desc = "").withDependency { roomColours }
    val trapRoomColor by ColorSetting("Trap Room", Color(216, 127, 51), desc = "").withDependency { roomColours }
    val entranceRoomColor by ColorSetting("Entry Room", Color(20, 133, 0), desc = "").withDependency { roomColours }
    val fairyRoomColor by ColorSetting("Fairy Room", Color(244, 19, 139), desc = "").withDependency { roomColours }
    val rareRoomColor by ColorSetting("Rare Room", Color(255, 203, 89), desc = "").withDependency { roomColours }
    val mimicRoomColor by ColorSetting("Mimic Room", Color(186, 66, 52), desc = "").withDependency { roomColours }

    // render

    val backgroundColour by ColorSetting("Background Colour", Color(0, 0, 0, 70F / 255F), allowAlpha = true, desc = "")
    val backgroundSize by NumberSetting("Background Size", 5F, 0..100, 0.25F, desc = "")
    val imageFolder by ActionSetting("Folder", desc = "") {
        Blaze3D.openPath(MapImageLoader.imagesPath)
    }
    val imageSelection by StringSetting("Image Selection", "", desc = "", placeholder = "")
    val imageAlpha by NumberSetting("Image Alpha", 255, 0..255, desc = "")
}

enum class AutoDE {
    AUTO,
    ENABLED,
    DISABLED
}

enum class ScoreCalculation {
    NONE,
    MAP_TIED,
    STANDALONE
}