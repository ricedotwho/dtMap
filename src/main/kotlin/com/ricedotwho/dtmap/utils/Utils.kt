package com.ricedotwho.dtmap.utils

import com.mojang.blaze3d.vertex.PoseStack
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.CustomRenderType
import com.odtheking.odin.utils.render.PrimitiveRenderer
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

inline val Entity.renderX: Double
    get() =
        xo + (x - xo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

inline val Entity.renderY: Double
    get() =
        yo + (y - yo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

inline val Entity.renderZ: Double
    get() =
        zo + (z - zo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

fun Any?.equalsAny(vararg options: Any?): Boolean =
    options.any { this == it }

fun Player.clickSlot(containerId: Int, slotIndex: Int, button: Int = 0, clickType: ContainerInput = ContainerInput.PICKUP) {
    mc.gameMode?.handleContainerInput(containerId, slotIndex, button, clickType, this)
}

private val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
private val numberRegex = Regex("^[0-9]+$")
fun romanToInt(s: String): Int {
    return if (s == "") 0
    else if (s.matches(numberRegex)) s.toInt()
    else {
        var result = 0
        for (i in 0 until s.length - 1) {
            val current = romanMap[s[i]] ?: 0
            val next = romanMap[s[i + 1]] ?: 0
            result += if (current < next) -current else current
        }
        result + (romanMap[s.last()] ?: 0)
    }
}

fun BlockPos.toAABB(): AABB =
    AABB(this)

fun BlockPos.box(): AABB {
    val shape = mc.level!!.getBlockState(this).getShape(mc.level!!, this)
    return (if (shape.isEmpty) Shapes.block().bounds() else shape.bounds()).move(this)
}

private fun resolveQuadRenderType(depth: Boolean, fullyOpaque: Boolean) = when {
    depth && fullyOpaque -> CustomRenderType.QUADS_OPAQUE
    depth -> CustomRenderType.QUADS_TRANSLUCENT
    fullyOpaque -> CustomRenderType.QUADS_ESP
    else -> CustomRenderType.QUADS_TRANSLUCENT_ESP
}

private fun RenderExtractEvent.cameraRelativePose(offset: Vec3 = Vec3.ZERO): PoseStack {
    val camera = mc.gameRenderer.mainCamera().position()
    val poseStack = context.poseStack()
    poseStack.pushPose()
    poseStack.translate(offset.x - camera.x, offset.y - camera.y, offset.z - camera.z)
    return poseStack
}

fun RenderExtractEvent.filledShape(shape: VoxelShape, color: Color, depth: Boolean = false) {
    val renderType = resolveQuadRenderType(depth, color.alphaFloat >= 0.999f)

    val poseStack = cameraRelativePose()
    submitGeometry(poseStack, renderType, depth) { pose, buffer ->
        shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            PrimitiveRenderer.addChainedFilledBoxVertices(
                pose, buffer,
                minX.toFloat(), minY.toFloat(), minZ.toFloat(),
                maxX.toFloat(), maxY.toFloat(), maxZ.toFloat(),
                color.redFloat, color.greenFloat, color.blueFloat, color.alphaFloat
            )
        }
    }
    poseStack.popPose()
}

private fun RenderExtractEvent.submitGeometry(poseStack: PoseStack, renderType: RenderType, depth: Boolean, renderer: SubmitNodeCollector.CustomGeometryRenderer) {
    val collector = context.submitNodeCollector()
    if (depth) return collector.submitCustomGeometry(poseStack, renderType, renderer)
    collector.submitCustom(SubmitRenderPhases.SEE_THROUGH_NAME_TAGS, NoDepthSubmit(poseStack.last().copy(), renderType, renderer))
}
