package com.ricedotwho.dtmap.config

import com.ricedotwho.dtmap.gui.Setting
import com.ricedotwho.dtmap.gui.Tab
import com.ricedotwho.dtmap.utils.opacity
import java.awt.Color

@Tab("Esp")
object C2Esp {
    @Setting("Door Esp", combo = ["Off", "Bad", "Good", "Legit"])
    var doorEsp = 0

    @Setting("Width", min = 0.0, max = 25.0)
    var doorEspWidth = 5.0f

    @Setting("Locked Color")
    var doorEspColor = Color.RED

    @Setting("Filled")
    var doorEspColorFilled = Color.RED.opacity(0.2f)

    @Setting("Openable Color")
    var doorOpenableColor = Color.GREEN

    @Setting("Filled")
    var doorOpenableColorFilled = Color.GREEN.opacity(0.2f)

    @Setting("Fairy Color")
    var doorFairyColor = Color(244, 19, 139)

    @Setting("Filled")
    var doorFairyColorFilled = doorFairyColor.opacity(0.2f)

    @Setting("Mimic", lineBefore = true)
    val mimic = "Mimic"

    @Setting("Mimic ESP")
    var mimicESP = false

    @Setting("Legit")
    var mimicLegit = true

    @Setting("Fill")
    var mimicEspFillColor = Color.RED.opacity(0.4f)

    @Setting("Outline")
    var mimicEspOutlineColor = Color.RED.opacity(0.0f)

    @Setting("Width", min = 1.0, max = 15.0)
    var mimicEspOutlineWidth = 3.0f

    @Setting("Key ESP", lineBefore = true)
    var keyEsp = false

    @Setting("Color")
    var keyColor = Color.RED

    @Setting("Tracer")
    var keyTracer = false

    @Setting("Width", min = 0.0, max = 5.0)
    var keyTracerWidth = 1.0f

    @Setting("Color")
    var keyTracerColor = Color.RED
}