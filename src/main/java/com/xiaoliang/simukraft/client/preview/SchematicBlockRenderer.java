package com.xiaoliang.simukraft.client.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = Simukraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@SuppressWarnings({"null", "deprecation"})
public class SchematicBlockRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x();
        double camY = mc.gameRenderer.getMainCamera().getPosition().y();
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z();

        renderBuildingPreview(mc, poseStack, camX, camY, camZ);
        renderImmediateBlocks(mc, poseStack, camX, camY, camZ);
    }

    /**
     * 建筑预览：使用预构建的 VBO 一次性绘制所有 MODEL 方块，
     * 仅对 ENTITYBLOCK_ANIMATED 方块（床、箱子等）回退到 renderSingleBlock。
     */
    private static void renderBuildingPreview(Minecraft mc, PoseStack poseStack,
                                               double camX, double camY, double camZ) {
        PreviewMesh mesh = BuildingPreviewManager.getCachedMesh();
        if (mesh == null || mesh.isEmpty()) return;

        BlockPos buildOrigin = BuildingPreviewManager.getMeshBuildOrigin();
        BlockPos currentOrigin = BuildingPreviewManager.getPreviewOrigin();
        double ox = currentOrigin.getX() - buildOrigin.getX();
        double oy = currentOrigin.getY() - buildOrigin.getY();
        double oz = currentOrigin.getZ() - buildOrigin.getZ();

        poseStack.pushPose();
        poseStack.translate(ox - camX, oy - camY, oz - camZ);
        Matrix4f projection = RenderSystem.getProjectionMatrix();

        drawVBO(mesh.solidBuffer(), RenderType.solid(), poseStack, projection);
        drawVBO(mesh.cutoutBuffer(), RenderType.cutoutMipped(), poseStack, projection);
        drawVBO(mesh.translucentBuffer(), RenderType.translucent(), poseStack, projection);

        poseStack.popPose();

        List<SchematicBlockData> entityBlocks = mesh.entityBlocks();
        if (entityBlocks.isEmpty()) return;

        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(
                Tesselator.getInstance().getBuilder());
        for (SchematicBlockData block : entityBlocks) {
            poseStack.pushPose();
            poseStack.translate(
                    block.pos().getX() + ox - camX,
                    block.pos().getY() + oy - camY,
                    block.pos().getZ() + oz - camZ);
            mc.getBlockRenderer().renderSingleBlock(
                    block.blockState(), poseStack, buf,
                    block.packedLight(), OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        buf.endBatch();
    }

    /**
     * 农田预览和区域选择仍使用 renderSingleBlock 逐方块渲染（方块数量少）。
     */
    private static void renderImmediateBlocks(Minecraft mc, PoseStack poseStack,
                                               double camX, double camY, double camZ) {
        List<SchematicBlockData> farmland = FarmlandAreaPreviewManager.getPreviewBlocks();
        List<SchematicBlockData> area = AreaSelectionManager.getPreviewBlocks();
        if (farmland.isEmpty() && area.isEmpty()) return;

        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(
                Tesselator.getInstance().getBuilder());
        try {
            renderBlockList(mc, poseStack, buf, farmland, camX, camY, camZ);
            renderBlockList(mc, poseStack, buf, area, camX, camY, camZ);
        } finally {
            PreviewRenderState.clearRenderingState();
        }
        buf.endBatch();
    }

    private static void renderBlockList(Minecraft mc, PoseStack poseStack,
                                         MultiBufferSource.BufferSource buf,
                                         List<SchematicBlockData> blocks,
                                         double camX, double camY, double camZ) {
        if (blocks.isEmpty()) return;
        PreviewRenderState.setRenderingBlocks(blocks);
        for (SchematicBlockData block : blocks) {
            PreviewRenderState.setCurrentRenderingPos(block.pos());
            poseStack.pushPose();
            poseStack.translate(
                    block.pos().getX() - camX,
                    block.pos().getY() - camY,
                    block.pos().getZ() - camZ);
            mc.getBlockRenderer().renderSingleBlock(
                    block.blockState(), poseStack, buf,
                    block.packedLight(), OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static void drawVBO(VertexBuffer vbo, RenderType renderType,
                                 PoseStack poseStack, Matrix4f projection) {
        if (vbo == null) return;

        renderType.setupRenderState();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            // 禁用雾效 - 将雾的范围设为极大值，相当于禁用
            RenderSystem.setShaderFogStart(1000000.0F);
            RenderSystem.setShaderFogEnd(10000000.0F);

            if (shader.CHUNK_OFFSET != null) {
                shader.CHUNK_OFFSET.set(0.0f, 0.0f, 0.0f);
            }
            vbo.bind();
            vbo.drawWithShader(poseStack.last().pose(), projection, shader);
            VertexBuffer.unbind();
        }

        renderType.clearRenderState();
    }
}
