package com.ricedotwho.dtmap.features.map

import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.DtMap.mc
import com.ricedotwho.dtmap.config.C1Map
import com.ricedotwho.dtmap.features.Leap
import com.ricedotwho.dtmap.features.SoloClear
import com.ricedotwho.dtmap.gui.Hud
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.Location
import com.ricedotwho.dtmap.utils.MapImageLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.awt.Color

object MapRenderer : Hud.Component("Map", 0.1, 0.1, Hud.Type.Dungeon, 1.5f, staticRenderConditions = mutableListOf(Hud.Condition.BeforeMort, Hud.Condition.Alt, Hud.Condition.Clear)) {
    private val selfMarker = Identifier.fromNamespaceAndPath("dtmap", "map/self_marker.png")
    val cross = Identifier.fromNamespaceAndPath("dtmap", "map/cross.png")
    val greenCheck = Identifier.fromNamespaceAndPath("dtmap", "map/green_check.png")
    val whiteCheck = Identifier.fromNamespaceAndPath("dtmap", "map/white_check.png")
    val questionMark = Identifier.fromNamespaceAndPath("dtmap", "map/question.png")

    override fun render(context: GuiGraphicsExtractor) {
        if (!C1Map.isOn || Location.island != Location.Island.Dungeon) return

        val textFactor = 1 / C1Map.textScaling
        val matrices = context.pose()

        matrices.pushMatrix()

        val mapSize = DungeonMap.calculateMapSize()
        val roomsX = mapSize.x * 16 + (mapSize.x - 1) * 4
        val roomsZ = mapSize.z * 16 + (mapSize.z - 1) * 4
        val offset = C1Map.backgroundSize.toInt() * 2

        val imageLocation = MapImageLoader.getImageIdentifier(C1Map.imageSelection)
        if (imageLocation != null) {
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                imageLocation,
                0, 0,
                0f, 0f,
                roomsX + offset, roomsZ + offset,
                roomsX + offset, roomsZ + offset,
                0x00FFFFFF or (C1Map.imageAlpha shl 24)
            )
        } else {
            context.fill(0, 0, roomsX + offset, roomsZ + offset, C1Map.backgroundColor.rgb)
        }

        matrices.translate(C1Map.backgroundSize, C1Map.backgroundSize)

        val legitMode = SoloClear.legit(C1Map.legitMode)

        Scan.rooms.forEach { it.render(context) }
        Scan.doors.forEach {
            if (legitMode && !it.seen) return@forEach
            it.render(context)
        }

        matrices.pushMatrix()

        Scan.rooms.forEach { room ->
            if (room.type == Room.Type.ENTRANCE || room.type == Room.Type.BLOOD) {
                return@forEach
            }

            room.renderName(context, textFactor)
        }

        // once for the text
        matrices.popMatrix()

        if (!DungeonMessages.inBoss) {
            val title = mc.screen?.title?.string
            val renderNames = DtMap.keybindShowHud.isDown || Leap.holdingLeap || title == "Spirit Leap" || title == "Teleport to Player"

            val renderPlayerHead = fun(player: Scoreboard.DungeonPlayer) {
                if (player.isDead) return

                val pos = player.mapRenderPosition()
                matrices.pushMatrix()
                matrices.translate(pos.a - 2, pos.b - 2)

                if (renderNames) {
                    matrices.pushMatrix()
                    matrices.scale(C1Map.playerNamesScaling)
                    context.centeredText(mc.font, player.name, 0, 8, C1Map.playerNameColor.rgb)
                    matrices.popMatrix()
                }

                matrices.rotate(Math.toRadians(180.0 + player.mapRenderYaw()).toFloat())

                val self = player.entity == mc.player
                val backgroundColor = if (C1Map.classOutlineColour) player.clazz.color else if (self) C1Map.playerHeadOwnBackground else C1Map.playerHeadBackground
                if (C1Map.playerHeadBackgroundSize != 0 && backgroundColor.alpha != 0 && !(self && C1Map.playerMCMapPointer)) {
                    val size = 5 + C1Map.playerHeadBackgroundSize
                    context.fill(-size, -size, size, size, backgroundColor.rgb)
                }

                if (self && C1Map.playerMCMapPointer) context.blit(RenderPipelines.GUI_TEXTURED, selfMarker, 5, 5, 10f, 10f, -10, -10, 10, 10)
                else if (player.skin != null) PlayerFaceExtractor.extractRenderState(context, player.skin!!, -5, -5, 10)
                matrices.popMatrix()
            }

            if (!C1Map.playerHeadDrawOwnLast) Scoreboard.dungeonTeammates.forEach(renderPlayerHead)
            else {
                Scoreboard.dungeonTeammatesNoSelf.forEach(renderPlayerHead)
                Scoreboard.dungeonPlayer?.let(renderPlayerHead)
            }
        }

        if (C1Map.scoreCalculation == 1) {
            matrices.pushMatrix()
            matrices.translate(roomsX / 2f, roomsZ + C1Map.backgroundSize + 3)
            Score.actualRender(context, true)
            matrices.popMatrix()
        }

        // once for the background bullshit
        matrices.popMatrix()
    }

    override fun example(context: GuiGraphicsExtractor) {
        val matrices = context.pose()
        val roomsX = 116
        val roomsZ = 116
        val offset = C1Map.backgroundSize.toInt()
        if (C1Map.imageSelection != "No image" && C1Map.imageSelection.isNotEmpty()) {
            val imageLocation = MapImageLoader.getImageIdentifier(C1Map.imageSelection)
            if (imageLocation != null) {
                val alpha = C1Map.imageAlpha.coerceIn(0, 255)
                val color = (alpha shl 24) or 0x00FFFFFF
                context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    imageLocation,
                    0, 0,
                    0f, 0f,
                    roomsX + offset * 2, roomsZ + offset * 2,
                    roomsX + offset * 2, roomsZ + offset * 2,
                    color
                )
            } else {
                context.fill(0, 0, roomsX + offset * 2, roomsZ + offset * 2, C1Map.backgroundColor.rgb)
            }
        } else {
            context.fill(0, 0, roomsX + offset * 2, roomsZ + offset * 2, C1Map.backgroundColor.rgb)
        }

        context.centeredText(mc.font, "MAP", roomsX/2 + offset, roomsZ/2 + offset - mc.font.lineHeight, Color.WHITE.rgb)

        if (C1Map.scoreCalculation == 1) {
            matrices.pushMatrix()
            matrices.translate(roomsX / 2f + offset, roomsZ + offset * 2f + 3f)
            Score.actualExample(context)
            matrices.popMatrix()
        }
    }

    override fun bounds(): Pair<Double, Double> {
        val startX = if (C1Map.scoreCalculation == 1) mc.font.lineHeight * 2 + 5 else 0
        return Pair(116.0 + C1Map.backgroundSize * 2, startX + 116.0 + C1Map.backgroundSize * 2)
    }
}