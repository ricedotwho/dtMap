package com.ricedotwho.dtmap.gui

import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Location
import imgui.ImGui
import imgui.ImGuiIO
import imgui.flag.ImGuiWindowFlags
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.resources.Identifier
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.math.sign


// mostly copied over from BladeMasterGabe's practical-config
object Hud : ImGuiHandler.RenderInterface("DtMapHud") {
    private val components = mutableListOf<Component>()

    enum class Condition(val displayName: String, val predicate: () -> Boolean) {
        Always("Always", { true }),
        Boss("In Dungeons Boss", { DungeonMessages.inBoss }),
        F7Boss("In F7 Boss", { DungeonMessages.inBoss && Scoreboard.floor.number == 7 }),
        Clear("In Clear", { Location.island == Location.Island.Dungeon && (DungeonMessages.seenDungeonStart || Scoreboard.stats.elapsedTime != "0s") && !DungeonMessages.inBoss }),
        BeforeMort("Before Dungeon Start", { Location.island == Location.Island.Dungeon && !DungeonMessages.seenDungeonStart }),
        Alt("Hud Insight", { DtMap.keybindShowHud.isDown }),
        HideOverlaySecrets("Hide Action Bar Secrets", { false })
    }

    enum class Type {
        Dungeon,
        Other
    }

    abstract class Component {
        internal val staticRenderConditions: MutableList<Condition>
        internal val allowedStaticRenderConditions: MutableList<Condition>

        internal val dungeonClasses: MutableList<Scoreboard.DungeonClass>

        internal val identifier: String
        internal var x: Double
        internal var y: Double
        internal val type: Type
        internal var scale: Float

        constructor(
            identifier: String,
            x: Double, y: Double,
            type: Type,
            scale: Float = 1.0f,
            staticRenderConditions: MutableList<Condition> = mutableListOf(Condition.Boss, Condition.Clear, Condition.Alt),
            allowedStaticRenderConditions: MutableList<Condition> = when (type) {
                Type.Dungeon -> mutableListOf(Condition.Boss, Condition.F7Boss, Condition.Clear, Condition.BeforeMort, Condition.Alt)
                Type.Other -> mutableListOf(Condition.Always, Condition.Alt)
            },
            dungeonClasses: MutableList<Scoreboard.DungeonClass> = if (type == Type.Dungeon) Scoreboard.DungeonClass.entries.subList(0, 5).toMutableList() else mutableListOf()
        ) {
            this.identifier = identifier
            this.x = x
            this.y = y
            this.type = type
            this.scale = scale
            this.staticRenderConditions = staticRenderConditions
            this.allowedStaticRenderConditions = allowedStaticRenderConditions
            this.dungeonClasses = dungeonClasses

            components.add(this)
        }

        internal fun position(context: GuiGraphicsExtractor): Pair<Int, Int> =
            position(context.guiWidth(), context.guiHeight())

        internal fun position(width: Int, height: Int) =
            Pair((x * width).toInt(), (y * height).toInt())

        internal fun shouldRender(): Boolean {
            val playerClass = Scoreboard.dungeonPlayer?.clazz ?: Scoreboard.DungeonClass.Unknown
            if (
                type == Type.Dungeon &&
                playerClass != Scoreboard.DungeonClass.Unknown &&
                dungeonClasses.none { it == Scoreboard.dungeonPlayer?.clazz }
            ) {
                return staticRenderConditions.find { it == Condition.BeforeMort }?.predicate() ?: false
            }

            return staticRenderConditions.any { it.predicate() }
        }

        abstract fun render(context: GuiGraphicsExtractor)
        abstract fun example(context: GuiGraphicsExtractor)
        abstract fun bounds(): Pair<Double, Double>

        internal fun offsetBounds(context: GuiGraphicsExtractor): Pair<Int, Int> =
            offsetBounds(context.guiWidth(), context.guiHeight())

        internal open fun offsetBounds(width: Int, height: Int): Pair<Int, Int> =
            Pair(0, 0)

        internal fun internalBounds(): Pair<Double, Double> {
            val bounds = bounds()
            return Pair(bounds.first * scale, bounds.second * scale)
        }

        internal fun internalRender(context: GuiGraphicsExtractor, example: Boolean) {
            if (mc.gui.tabList.visible || (!example && !shouldRender())) return

            val pose = context.pose()

            pose.pushMatrix()
            pose.translate(x.toFloat() * context.guiWidth(), y.toFloat() * context.guiHeight())
            pose.scale(scale)

            if (example) example(context)
            else render(context)

            pose.popMatrix()
        }
    }

    private val configPath: Path = FabricLoader.getInstance().configDir.resolve("dtmap/hud")
    private fun save(component: Component) {
        if (!configPath.exists()) configPath.createDirectories()
        val file = configPath.resolve(component.identifier)
        val writer = file.bufferedWriter()
        writer.write("x:${component.x}\n")
        writer.write("y:${component.y}\n")
        writer.write("scale:${component.scale}\n")
        writer.write("conditions:${component.staticRenderConditions.joinToString(",") { it.name }}\n")

        if (component.dungeonClasses.isNotEmpty()) writer.write("dungeon_classes:${component.dungeonClasses.joinToString(",") { it.name }}\n")

        writer.close()
    }

    private fun load(component: Component) {
        val file = configPath.resolve(component.identifier)
        if (!file.exists()) return
        val reader = file.bufferedReader()
        reader.readLines().forEach {
            val spl = it.split(":")
            if (spl.size != 2) return@forEach
            when (spl[0]) {
                "x" -> component.x = spl[1].toDouble()
                "y" -> component.y = spl[1].toDouble()
                "scale" -> component.scale = spl[1].toFloat()
                "conditions" -> {
                    component.staticRenderConditions.clear()
                    component.staticRenderConditions.addAll(spl[1].split(",").mapNotNull { modifier -> Condition.entries.find { it.name == modifier } })
                }
                "dungeon_classes" -> {
                    component.dungeonClasses.clear()
                    component.dungeonClasses.addAll(spl[1].split(",").mapNotNull { klass -> Scoreboard.DungeonClass.entries.find { it.name == klass }})
                }
            }
        }
        reader.close()
    }

    fun register() {
        components.forEach(::load)
        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP, Identifier.fromNamespaceAndPath("dtmap", "hud"), Renderer)
    }

    private var selected: Component? = null
    private var openedOptions: Component? = null
    private var isDragging = false

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        if (selected != null) selected!!.scale += sign(verticalAmount).toFloat() * 0.02f
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val x = click.x()
        val y = click.y()

        if (clickedInOpenedOptions(x, y)) return super.mouseClicked(click, doubled)

        val clicked = components.find {
            val bounds = it.internalBounds()
            var (posX, posY) = it.position(minecraft.window.guiScaledWidth, minecraft.window.guiScaledHeight)
            val offset = it.offsetBounds(minecraft.window.guiScaledWidth, minecraft.window.guiScaledHeight)
            posX += offset.first
            posY += offset.second

            posX <= x && posX + bounds.first >= x && posY <= y && posY + bounds.second >= y
        }

        selected = clicked
        if (selected == null || selected != openedOptions) openedOptions = null

        if (click.button() == 1) {
            openedOptions = if (clicked == openedOptions) null else clicked
            return super.mouseClicked(click, doubled)
        }

        if (selected != null) isDragging = true

        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (isDragging && selected != null) {
            checkNotNull(minecraft)
            val window = minecraft!!.window
            selected!!.x += offsetX / window.guiScaledWidth
            selected!!.y += offsetY / window.guiScaledHeight
        }

        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        isDragging = false
        return super.mouseReleased(click)
    }

    private val Renderer = HudElement { context, _ ->
        components.forEach { it.internalRender(context, false) }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks)
        val width = context.guiWidth()
        val height = context.guiHeight()

        context.verticalLine(width / 2, 0, height, -0xffbbbc)
        context.horizontalLine(0, width, height / 2, -0xffbbbc)

        components.forEach { it.internalRender(context, true) }

        if (selected != null) {
            val bounds = selected!!.internalBounds()
            val (posX, posY) = selected!!.position(context)
            val offset = selected!!.offsetBounds(context)
            context.outline(posX + offset.first, posY + offset.second, bounds.first.toInt(), bounds.second.toInt(), Color.RED.rgb)
        }
    }

    override fun isPauseScreen(): Boolean =
        false

    override fun onClose() {
        components.forEach(::save)
        selected = null
        openedOptions = null
        super.onClose()
    }

    private val openedOptionsSize = Pair(500f, 600f)

    private fun clickedInOpenedOptions(x: Double, y: Double): Boolean {
        if (openedOptions == null) return false

        val (posX, posY) = selectedWindowPosition()
        val (sizeX, sizeY) = openedOptionsSize

        val scale = minecraft!!.window.guiScale
        val scaledX = x * scale
        val scaledY = y * scale
        return (scaledX >= posX && scaledX <= posX + sizeX && scaledY >= posY && scaledY <= posY + sizeY)
    }

    private fun selectedWindowPosition(): Pair<Float, Float> {
        val opened = openedOptions ?: return Pair(0f, 0f)

        val viewPort = ImGui.getMainViewport()
        val (boundsX, boundsY) = opened.offsetBounds(viewPort.size.x.toInt(), viewPort.size.y.toInt())
        val pos = opened.position(viewPort.sizeX.toInt(), viewPort.sizeY.toInt())
        val (sizeX, sizeY) = openedOptionsSize

        val scale = minecraft!!.window.guiScale
        var x = viewPort.pos.x + pos.first + boundsX.toFloat() * scale - sizeX
        var y = viewPort.pos.y + pos.second + boundsY.toFloat() * scale

        if (y < viewPort.pos.y) y = viewPort.pos.y
        if (y + 600f > viewPort.pos.y + viewPort.size.y) y = viewPort.pos.y + viewPort.size.y - sizeY

        if (viewPort.pos.x > x) {
            val (boundsSizeX, _) = opened.internalBounds()
            x += boundsSizeX.toFloat() * scale + sizeX
        }

        return Pair(x, y)
    }

    override fun render(io: ImGuiIO) {
        val opened = openedOptions ?: return

        val (x, y) = selectedWindowPosition()
        val (sizeX, sizeY) = openedOptionsSize
        ImGui.setNextWindowPos(x, y)
        ImGui.setNextWindowSize(sizeX, sizeY)

        val flags = ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse
        if (ImGui.begin("DtMapHud", flags)) {
            opened.allowedStaticRenderConditions.forEach { condition ->
                val active = opened.staticRenderConditions.find { it == condition }
                if (ImGui.checkbox(condition.displayName, active != null)) {
                    if (active != null) opened.staticRenderConditions.remove(active)
                    else opened.staticRenderConditions.add(condition)
                }
            }

            if (opened.type == Type.Dungeon) {
                if (ImGui.beginCombo("##classes", opened.dungeonClasses.joinToString(", ") { it.name })) {
                    Scoreboard.DungeonClass.entries.subList(0, 5).forEachIndexed { index, klass ->
                        ImGui.pushID(index)
                        val active = opened.dungeonClasses.find { it.name == klass.name }
                        if (ImGui.checkbox(klass.name, active != null)) {
                            if (active != null) opened.dungeonClasses.remove(active)
                            else opened.dungeonClasses.add(klass)
                        }
                        ImGui.popID()
                    }
                    ImGui.endCombo()
                }
                ImGui.sameLine()
                ImGui.labelText("##classeslabel", "Classes")
            }
        }
        ImGui.end()
    }
}