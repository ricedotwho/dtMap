package com.ricedotwho.dtmap.utils.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.odtheking.odin.clickgui.GuiTheme
import com.odtheking.odin.clickgui.settings.RenderableSetting
import com.odtheking.odin.clickgui.settings.impl.label
import com.odtheking.odin.clickgui.ui.AnimatedVisibility
import com.odtheking.odin.clickgui.ui.Gap
import com.odtheking.odin.clickgui.ui.Pill
import com.odtheking.odin.clickgui.ui.SettingRow
import com.odtheking.odin.clickgui.ui.textCentered
import com.odtheking.odin.utils.Color.Companion.brighter
import com.odtheking.odin.utils.Color.Companion.darker
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.roundedOutline
import com.odtheking.odin.utils.render.roundedRect
import com.odtheking.odin.utils.ui.compose.Box
import com.odtheking.odin.utils.ui.compose.Column
import com.odtheking.odin.utils.ui.compose.OnDismiss
import com.odtheking.odin.utils.ui.compose.Pointer
import com.odtheking.odin.utils.ui.compose.clickable
import com.odtheking.odin.utils.ui.compose.drawBehind
import com.odtheking.odin.utils.ui.compose.offset
import com.odtheking.odin.utils.ui.compose.size
import com.odtheking.odin.utils.ui.compose.width
import com.ricedotwho.dtmap.utils.lerp

class MultiBoolSetting<E : Enum<E>>(
    name: String,
    val type: Class<E>,
    override val default: MutableList<E> = mutableListOf(),
    val options: List<E> = type.enumConstants.asList(),
    desc: String = ""
) : RenderableSetting<MutableList<E>>(name, desc), Saving {

    override var value: MutableList<E> by mutableStateOf(default)

    @Composable
    override fun Content() {
        var expanded by remember { mutableStateOf(false) }
        OnDismiss { expanded = false }

        Column {
            SettingRow { Pill(getDisplayText(), onClick = { expanded = !expanded }) }
            AnimatedVisibility(expanded) {
                Column {
                    Gap(1)
                    Column {
                        options.forEachIndexed { index, option ->
                            Option(option, last = index == options.lastIndex) {
                                if (value.contains(option)) {
                                    value.remove(option)
                                } else {
                                    value.add(option)
                                }
                            }
                        }
                    }
                        .width(LIST_WIDTH).offset({ LIST_INSET })
                        .drawBehind { graphics -> graphics.roundedRect(x, y, right, bottom, GuiTheme.surface.rgba, GuiTheme.RADIUS) }
                    Gap(LIST_INSET)
                }.width(GuiTheme.ROW_WIDTH)
            }
        }
    }

    private fun getDisplayText(): String {
        val builder = StringBuilder()
        for (option in value) {
            if (builder.isNotEmpty()) builder.append(", ")
            builder.append(option.label)
        }
        val temp = builder.toString()
        return temp.ifEmpty { "None" }
    }

    @Composable
    private fun Option(option: E, last: Boolean, onClick: () -> Unit) = Box().size(LIST_WIDTH, OPTION_HEIGHT)
        .clickable(onClick = onClick).drawBehind { graphics ->
            if (!last) graphics.fill(x + SEPARATOR_INSET, bottom, right - SEPARATOR_INSET, bottom + 1, Colors.MINECRAFT_DARK_GRAY.rgba)
            if (Pointer.isOver(x, y, width, height)) graphics.roundedOutline(x, y, right, bottom + 1, GuiTheme.accent.rgba, 1.5f, GuiTheme.RADIUS)
            else if (value.contains(option)) graphics.roundedOutline(x, y, right, bottom + 1, SELECTED, 1.5f, GuiTheme.RADIUS)
            graphics.textCentered(option.label, x, y, right, bottom)
        }

    override fun write(gson: Gson): JsonElement {
        val boolArray = JsonArray()
        for (key in value) {
            boolArray.add(key.name)
        }
        return boolArray
    }

    override fun read(element: JsonElement, gson: Gson) {
        value.clear()
        (element as JsonArray).forEach { element ->
            val e = element.asString!!
            value.add(options.find { option -> option.name == e }!!)
        }
    }

    private companion object {
        const val OPTION_HEIGHT = 16
        const val LIST_INSET = 4
        const val LIST_WIDTH = GuiTheme.ROW_WIDTH - LIST_INSET * 2
        const val SEPARATOR_INSET = 10
        val SELECTED = Colors.MINECRAFT_GREEN.rgba
    }
}