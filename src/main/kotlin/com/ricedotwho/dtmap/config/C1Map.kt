package com.ricedotwho.dtmap.config

import com.ricedotwho.dtmap.gui.Setting
import com.ricedotwho.dtmap.gui.Tab
import com.ricedotwho.dtmap.utils.MapImageLoader
import com.ricedotwho.dtmap.utils.opacity
import net.minecraft.util.Util
import java.awt.Color

@Tab("Map")
object C1Map {
    @Setting("Toggle", lineAfter = true)
    var isOn = false

    @Setting("Legit Mode")
    var legitMode = false

    @Setting("Background Color", lineBefore = true)
    var backgroundColor = Color(0, 0, 0, 70)

    @Setting("Background Size", min = 0.0, max = 100.0)
    var backgroundSize = 5f

    @Setting("Folder")
    var imageFolder: Function<Unit> = {
        Util.getPlatform().openPath(MapImageLoader.imagesPath)
    }

    @Setting("Image Selection", sameLineBefore = true)
    var imageSelection = ""

    @Setting("Image Alpha", min = 0.0, max = 255.0)
    var imageAlpha = 255

    @Setting("Score Calculation", lineBefore = true, combo = ["None", "Map Tied", "Standalone"])
    var scoreCalculation = 0

    @Setting("Max bonus for missing/needed")
    var scoreMaxBonusForMissingSecrets = false

    @Setting("Replace missing with needed", sameLineBefore = true)
    var scoreNeededSecretsInsteadOfMissing = false

    @Setting("Missing secrets calculate paul")
    var scoreMissingSecretsCalculatePaul = false

    @Setting("270 Message Text", dontRenderName = true)
    var score270Message = "270"

    @Setting("270 Message", sameLineBefore = true)
    var score270MessageEnabled = false

    @Setting("300 Message Text", dontRenderName = true)
    var score300Message = "300"

    @Setting("300 Message", sameLineBefore = true)
    var score300MessageEnabled = false

    @Setting("Paul", combo = ["Auto", "Enable", "Disable"])
    var scorePaul = 0

    @Setting("Text Scaling", lineBefore = true, min = 0.0, max = 1.5)
    var textScaling = 0.45f

    @Setting("Center Text")
    var textCenter = true

    @Setting("Undiscovered Question marks")
    var undiscoveredQuestionMarks = false

    @Setting("Player Head", lineBefore = true)
    val playerHead = "Player Head"

    @Setting("Archer Color")
    var archerColor = Color(255, 170, 0)

    @Setting("Mage Color")
    var mageColor = Color(85, 255, 255)

    @Setting("Berserk Color")
    var berserkColor = Color(255, 85, 85)

    @Setting("Tank Color")
    var tankColor = Color(0, 170, 0)

    @Setting("Healer Color")
    var healerColor = Color(255, 85, 255)

    @Setting("Outline Color")
    var playerHeadBackground = Color.BLACK.opacity(0.7f)

    @Setting("Use class outline colour")
    var classOutlineColour = true

    @Setting("Own Outline Color")
    var playerHeadOwnBackground = Color.RED.opacity(0.7f)

    @Setting("Outline Size", min = 0.0, max = 5.0)
    var playerHeadBackgroundSize = 1

    @Setting("Draw Own Last")
    var playerHeadDrawOwnLast = false

    @Setting("MC Map Player Pointer")
    var playerMCMapPointer = false

    @Setting("Player Name Scaling", min = 0.0, max = 1.5)
    var playerNamesScaling = 0.75f

    @Setting("Player Name Color")
    var playerNameColor = Color(70, 70, 70)

    @Setting("Room additions", lineBefore = true)
    val roomAdditions = "Additional render to rooms."

    @Setting("Render Prince")
    var roomAdditionsPrince = false

    @Setting("Door Thickness", lineBefore = true, min = 0.0, max = 10.0)
    var doorThicknessF = 9f

    @Setting("Unopened Door")
    var unopenedDoorColor = Color(30, 30, 30)

    @Setting("Blood Door")
    var bloodDoorColor = Color.RED

    @Setting("Wither Door")
    var witherDoorColor = Color.BLACK

    @Setting("Normal Door")
    var normalDoorColor = Color(107, 58, 17)

    @Setting("Puzzle Door")
    var puzzleDoorColor = Color(117, 0, 133)

    @Setting("Champion Door")
    var championDoorColor = Color(254, 223, 0)

    @Setting("Trap Door")
    var trapDoorColor = Color(216, 127, 51)

    @Setting("Entrance Door")
    var entranceDoorColor = Color(20, 133, 0)

    @Setting("Fairy Door")
    var fairyDoorColor = Color(244, 19, 139)

    @Setting("Rare Door")
    var rareDoorColor = Color(255, 203, 89)

    @Setting("Darken Multiplier", min = 0.0, max = 1.0, lineBefore = true)
    var darkenMultiplier = 0.4f

    @Setting("Unopened Room")
    var unopenedRoomColor = Color(30, 30, 30)

    @Setting("Blood Room")
    var bloodRoomColor = Color(255, 0, 0)

    @Setting("Normal Room")
    var normalRoomColor = Color(107, 58, 17)

    @Setting("Puzzle Room")
    var puzzleRoomColor = Color(117, 0, 133)

    @Setting("Champion Room")
    var championRoomColor = Color(254, 223, 0)

    @Setting("Trap Room")
    var trapRoomColor = Color(216, 127, 51)

    @Setting("Entrance Room")
    var entranceRoomColor = Color(20, 133, 0)

    @Setting("Fairy Room")
    var fairyRoomColor = Color(244, 19, 139)

    @Setting("Rare Room")
    var rareRoomColor = Color(255, 203, 89)

    @Setting("Mimic Room")
    var mimicRoomColor = Color(186, 66, 52)

    @Setting("Blood Rush Instaclear Sound", lineBefore = true)
    var bloodRushInstaclearNotification = false

    @Setting("Debug", lineBefore = true)
    var debug = false

    @Setting("X", min = 0.0, max = 2560.0)
    var debugX = 500

    @Setting("Y", min = 0.0, max = 1440.0)
    var debugY = 500
}
