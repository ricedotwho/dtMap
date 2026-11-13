package com.ricedotwho.dtmap.features.map

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.ricedotwho.dtmap.DtMap
import com.ricedotwho.dtmap.DtMap.zero
import com.ricedotwho.dtmap.config.dungeon.MapConfig
import com.ricedotwho.dtmap.config.dungeon.ScoreCalculation
import com.ricedotwho.dtmap.features.Leap
import com.ricedotwho.dtmap.features.SoloClear
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.MapImageLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object MapRenderer {
    private val selfMarker = Identifier.fromNamespaceAndPath("dtmap", "map/self_marker.png")
    val cross = Identifier.fromNamespaceAndPath("dtmap", "map/cross.png")
    val greenCheck = Identifier.fromNamespaceAndPath("dtmap", "map/green_check.png")
    val whiteCheck = Identifier.fromNamespaceAndPath("dtmap", "map/white_check.png")
    val questionMark = Identifier.fromNamespaceAndPath("dtmap", "map/question.png")

    fun render(context: GuiGraphicsExtractor, example: Boolean) : Pair<Int, Int> {
        if (example) {
            val matrices = context.pose()
            val roomsX = 116
            val roomsZ = 116
            val offset = MapConfig.backgroundSize.toInt()
            if (MapConfig.imageSelection != "No image" && MapConfig.imageSelection.isNotEmpty()) {
                val imageLocation = MapImageLoader.getImageIdentifier(MapConfig.imageSelection)
                if (imageLocation != null) {
                    val alpha = MapConfig.imageAlpha.coerceIn(0, 255)
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
                    context.fill(0, 0, roomsX + offset * 2, roomsZ + offset * 2, MapConfig.backgroundColour.rgba)
                }
            } else {
                context.fill(0, 0, roomsX + offset * 2, roomsZ + offset * 2, MapConfig.backgroundColour.rgba)
            }

            context.centeredText(mc.font, "MAP", roomsX/2 + offset, roomsZ/2 + offset - mc.font.lineHeight, java.awt.Color.WHITE.rgb)

            if (MapConfig.scoreCalculation == ScoreCalculation.MAP_TIED) {
                matrices.pushMatrix()
                matrices.translate(roomsX / 2f + offset, roomsZ + offset * 2f + 3f)
                Score.example(context)
                matrices.popMatrix()
            }

            val startX = if (MapConfig.scoreCalculation == ScoreCalculation.MAP_TIED) mc.font.lineHeight * 2 + 5 else 0
            return (116.0 + MapConfig.backgroundSize * 2).toInt() to (startX + 116.0 + MapConfig.backgroundSize * 2).toInt()
        } else {
            if (!MapConfig.enabled || LocationUtils.currentArea != Island.Dungeon) return zero

            val textFactor = 1 / MapConfig.textScaling
            val matrices = context.pose()

            matrices.pushMatrix()

            val mapSize = DungeonMap.calculateMapSize()
            val roomsX = mapSize.x * 16 + (mapSize.x - 1) * 4
            val roomsZ = mapSize.z * 16 + (mapSize.z - 1) * 4
            val offset = MapConfig.backgroundSize.toInt() * 2

            val imageLocation = MapImageLoader.getImageIdentifier(MapConfig.imageSelection)
            if (imageLocation != null) {
                context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    imageLocation,
                    0, 0,
                    0f, 0f,
                    roomsX + offset, roomsZ + offset,
                    roomsX + offset, roomsZ + offset,
                    0x00FFFFFF or (MapConfig.imageAlpha shl 24)
                )
            } else {
                context.fill(0, 0, roomsX + offset, roomsZ + offset, MapConfig.backgroundColour.rgba)
            }

            matrices.translate(MapConfig.backgroundSize, MapConfig.backgroundSize)

            val legitMode = SoloClear.legit(MapConfig.legitMode)

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
                val title = mc.gui.screen()?.title?.string
                val renderNames = DtMap.keybindShowHud.isDown || Leap.holdingLeap || title == "Spirit Leap" || title == "Teleport to Player"

                val renderPlayerHead = fun(player: Scoreboard.DungeonPlayer) {
                    if (player.isDead) return

                    val pos = player.mapRenderPosition()
                    matrices.pushMatrix()
                    matrices.translate(pos.first - 2, pos.second - 2)

                    if (renderNames) {
                        matrices.pushMatrix()
                        matrices.scale(MapConfig.playerNamesScaling)
                        context.centeredText(mc.font, player.name, 0, 8, MapConfig.playerNameColor.rgba)
                        matrices.popMatrix()
                    }

                    matrices.rotate(Math.toRadians(180.0 + player.mapRenderYaw()).toFloat())

                    val self = player.entity == mc.player
                    val backgroundColor = if (MapConfig.classOutlineColour) player.clazz.color else if (self) MapConfig.playerHeadOwnBackground else MapConfig.playerHeadBackground
                    if (MapConfig.playerHeadBackgroundSize != 0 && backgroundColor.alpha != 0 && !(self && MapConfig.playerMCMapPointer)) {
                        val size = 5 + MapConfig.playerHeadBackgroundSize
                        context.fill(-size, -size, size, size, backgroundColor.rgba)
                    }

                    if (self && MapConfig.playerMCMapPointer) context.blit(RenderPipelines.GUI_TEXTURED, selfMarker, 5, 5, 10f, 10f, -10, -10, 10, 10)
                    else if (player.skin != null) PlayerFaceExtractor.extractRenderState(context, player.skin!!, -5, -5, 10)
                    matrices.popMatrix()
                }

                if (!MapConfig.playerHeadDrawOwnLast) Scoreboard.dungeonTeammates.forEach(renderPlayerHead)
                else {
                    Scoreboard.dungeonTeammatesNoSelf.forEach(renderPlayerHead)
                    Scoreboard.dungeonPlayer?.let(renderPlayerHead)
                }
            }

            if (MapConfig.scoreCalculation == ScoreCalculation.MAP_TIED) {
                matrices.pushMatrix()
                matrices.translate(roomsX / 2f, roomsZ + MapConfig.backgroundSize + 3)
                Score.actualRender(context)
                matrices.popMatrix()
            }

            // once for the background bullshit
            matrices.popMatrix()

            val startX = if (MapConfig.scoreCalculation == ScoreCalculation.MAP_TIED) mc.font.lineHeight * 2 + 5 else 0
            return (116.0 + MapConfig.backgroundSize * 2).toInt() to (startX + 116.0 + MapConfig.backgroundSize * 2).toInt()
        }
    }
}