package com.ricedotwho.dtmap.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap.zero
import com.ricedotwho.dtmap.features.map.Room
import com.ricedotwho.dtmap.features.map.Scan
import com.ricedotwho.dtmap.features.map.Scoreboard
import com.ricedotwho.dtmap.utils.Colours
import net.minecraft.client.gui.GuiGraphicsExtractor

object PuzzleHud {
    fun render(context: GuiGraphicsExtractor, example: Boolean): Pair<Int, Int> {
        if (example) {
            val a = maxOf(
                context.textDim("Puzzles(3):", 0, 0, Colors.WHITE).first,
                context.textDim("Tic Tac Toe", 0, mc.font.lineHeight + 2, Colours.GREEN).first,
                context. textDim("Quiz", 0, (mc.font.lineHeight + 2) * 2, Colours.RED).first,
                context.textDim("???", 0, (mc.font.lineHeight + 2) * 3, Colours.RED).first
            )
            return a to mc.font.lineHeight * 4
        } else if (LocationUtils.currentArea == Island.Dungeon) {
            var width = context.textDim("Puzzles(${Scoreboard.stats.puzzleCount}):", 0, 0, Colours.WHITE).first

            var undiscoveredPuzzleCount: Int

            if (SoloClear.legit()) {
                Scoreboard.stats.puzzles.forEachIndexed { index, puzzle ->
                    val height = (index + 1) * (mc.font.lineHeight + 2)

                    val color = if (puzzle.status == Scoreboard.PuzzleStatus.Completed) Colours.GREEN else Colours.RED
                    width = maxOf(context.textDim(puzzle.displayName, 0, height, color).first, width)
                }

                undiscoveredPuzzleCount = Scoreboard.stats.puzzleCount - Scoreboard.stats.puzzles.size
            } else {
                Scan.puzzles.forEachIndexed { index, puzzle ->
                    val height = (index + 1) * (mc.font.lineHeight + 2)

                    val color = if (puzzle.state == Room.State.GREEN || puzzle.state == Room.State.CLEARED) Colours.GREEN else Colours.RED
                    val name = if (puzzle.data == null) "???" else puzzle.data!!.name
                    width = maxOf(context.textDim(name, 0, height, color).first, width)
                }

                undiscoveredPuzzleCount = Scoreboard.stats.puzzleCount - Scan.puzzles.size
            }

            for (index in Scoreboard.stats.puzzleCount - undiscoveredPuzzleCount until Scoreboard.stats.puzzleCount) {
                val height = (index + 1) * (mc.font.lineHeight + 2)
                context.textDim("???", 0, height, Colours.RED)
            }

            return width to (Scoreboard.stats.puzzleCount + 1) * mc.font.lineHeight
        } else {
            return zero
        }
    }
}