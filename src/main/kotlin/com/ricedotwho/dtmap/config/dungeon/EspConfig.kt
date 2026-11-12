package com.ricedotwho.dtmap.config.dungeon

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors

@AlwaysActive
object EspConfig : Module(
    name = "dtEsp",
    description = "downtime esp"
) {
    val doorEspCat by DropdownSetting("Door Esp", desc = "")
    val doorEsp by BooleanSetting("Door Esp Enabled", false, desc = "").withDependency { doorEspCat }
    val doorEspColor by ColorSetting("Locked Colour", Color(255, 0, 0), desc = "").withDependency { doorEspCat }
    val doorEspColorFilled by ColorSetting("Door Esp Fill", Color(255, 0, 0).withAlpha(0.2f), allowAlpha = true, desc = "").withDependency { doorEspCat }
    val doorOpenableColor by ColorSetting("Openable Color", Color(0, 255, 0), desc = "").withDependency { doorEspCat }
    val doorOpenableColorFilled by ColorSetting("Openable Color Fill", Color(0, 255, 0).withAlpha(0.2f), allowAlpha = true, desc = "").withDependency { doorEspCat }
    val doorEspOutlineWidth by NumberSetting("Door Outline Width", 3F, 0.5..5.0, 0.1, desc = "").withDependency { doorEspCat }

    // Mimic
    val mimicEspCat by DropdownSetting("Mimic Esp", desc = "")
    val mimicEsp by BooleanSetting("Mimic Esp Enabled", false, desc = "").withDependency { mimicEspCat }
    val mimicLegit by BooleanSetting("Mimic Legit", true, desc = "").withDependency { mimicEspCat }
    val mimicEspFillColor by ColorSetting("Mimic Fill", Colors.WHITE.withAlpha(0.4f), allowAlpha = true, desc = "").withDependency { mimicEspCat }
    val mimicEspOutlineColor by ColorSetting("Mimic Outline", Colors.WHITE, desc = "").withDependency { mimicEspCat }
    val mimicEspOutlineWidth by NumberSetting("Mimic Width", 3F, 0.5..5.0, 0.1, desc = "").withDependency { mimicEspCat }

    // Key
    val keyEspCat by DropdownSetting("Key Esp", desc = "")
    val keyEsp by BooleanSetting("Key Esp Enabled", false, desc = "").withDependency { keyEspCat }
    val keyColour by ColorSetting("Key Colour", Color(255, 0, 0), desc = "").withDependency { keyEspCat }
    val keyTracer by BooleanSetting("Key Tracer", false, desc = "").withDependency { keyEspCat }
    val keyWidth by NumberSetting("Key Width", 1F, 0.5..5.0, 0.1, desc = "").withDependency { keyEspCat }
    val keyTracerColour by ColorSetting("Key Tracer Colour", Color(255, 0, 0), desc = "").withDependency { keyEspCat }
    val keyPickupPling by BooleanSetting("Key Pickup Pling", false, desc = "").withDependency { keyEspCat }
    val keyPickupVolume by NumberSetting("Key Pickup Volume ", 1F, 0.1..2.0, desc = "").withDependency { keyEspCat }

    val redstoneKeyEsp by BooleanSetting("Redstone Key Esp", false, desc = "")
}