package com.xiaoliang.simukraft.integration.ftb;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/**
 * FTB Chunks 增强渲染器。
 * 当 FTB Chunks 可用时，利用其已渲染的地图纹理提供地形显示。
 * <p>
 * 此类仅在 {@link com.xiaoliang.simukraft.integration.ModIntegrationManager#isFTBChunksPresent()}
 * 返回 true 时才会被调用（通过完全限定名引用），确保在 FTB Chunks 不存在时不会加载此类。
 * <p>
 * 所有 FTB Chunks API 调用均包裹在 try-catch 中，渲染失败时返回 false，
 * 调用方会降级到自有渲染系统。
 */
@SuppressWarnings("null")
public class FTBEnhancedRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FTBEnhancedRenderer() {
    }

    /**
     * 使用 FTB Chunks 的纹理渲染地形。
     *
     * @param guiGraphics    GuiGraphics
     * @param startX         绘制区域左上角 X
     * @param startY         绘制区域左上角 Y
     * @param width          绘制区域宽度
     * @param height         绘制区域高度
     * @param centerX        屏幕中心 X
     * @param centerY        屏幕中心 Y
     * @param offsetX        平移偏移 X（像素）
     * @param offsetY        平移偏移 Y（像素）
     * @param zoomLevel      缩放级别
     * @param startChunkX    可见区块起始 X
     * @param startChunkZ    可见区块起始 Z
     * @param visibleChunksX 可见区块数 X
     * @param visibleChunksZ 可见区块数 Z
     * @return true 如果渲染成功；false 如果应降级到自有渲染
     */
    public static boolean renderFTBTerrain(GuiGraphics guiGraphics,
                                            int startX, int startY, int width, int height,
                                            int centerX, int centerY,
                                            double offsetX, double offsetY,
                                            double zoomLevel,
                                            int startChunkX, int startChunkZ,
                                            int visibleChunksX, int visibleChunksZ) {
        try {
            java.util.Optional<dev.ftb.mods.ftbchunks.client.map.MapManager> optManager =
                    dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance();
            if (optManager.isEmpty()) return false;

            dev.ftb.mods.ftbchunks.client.map.MapManager mapManager = optManager.get();

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return false;

            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = mc.level.dimension();
            dev.ftb.mods.ftbchunks.client.map.MapDimension dimension = mapManager.getDimension(dimKey);
            if (dimension == null) return false;

            int startBlockX = startChunkX << 4;
            int startBlockZ = startChunkZ << 4;
            int endBlockX = (startChunkX + visibleChunksX) << 4;
            int endBlockZ = (startChunkZ + visibleChunksZ) << 4;

            int minRegX = startBlockX >> 9;
            int maxRegX = endBlockX >> 9;
            int minRegZ = startBlockZ >> 9;
            int maxRegZ = endBlockZ >> 9;

            guiGraphics.flush();
            double guiScale = mc.getWindow().getGuiScale();
            int scissorX = (int) (startX * guiScale);
            int scissorY = (int) ((mc.getWindow().getScreenHeight() - (startY + height) * guiScale));
            int scissorW = (int) (width * guiScale);
            int scissorH = (int) (height * guiScale);

            com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
            com.mojang.blaze3d.platform.GlStateManager._scissorBox(scissorX, scissorY, scissorW, scissorH);

            try {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

                for (int regX = minRegX; regX <= maxRegX; regX++) {
                    for (int regZ = minRegZ; regZ <= maxRegZ; regZ++) {
                        dev.ftb.mods.ftbchunks.client.map.MapRegion region =
                                dimension.getRegion(dev.ftb.mods.ftblibrary.math.XZ.of(regX, regZ));

                        if (!region.isMapImageLoaded()) {
                            region.getRenderedMapImageTextureId();
                            if (!region.isMapImageLoaded()) continue;
                        }

                        int textureId = region.getRenderedMapImageTextureId();
                        if (textureId == -1) continue;

                        double regionWorldX = regX * 512.0;
                        double regionWorldZ = regZ * 512.0;

                        double screenLeft = centerX + offsetX + regionWorldX * zoomLevel;
                        double screenTop = centerY + offsetY + regionWorldZ * zoomLevel;
                        double regionPixels = 512.0 * zoomLevel;

                        if (screenLeft + regionPixels < startX || screenLeft > startX + width
                                || screenTop + regionPixels < startY || screenTop > startY + height) {
                            continue;
                        }

                        int filter = regionPixels * guiScale < 512.0
                                ? org.lwjgl.opengl.GL11.GL_LINEAR
                                : org.lwjgl.opengl.GL11.GL_NEAREST;

                        RenderSystem.bindTextureForSetup(textureId);
                        RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                                org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, filter);
                        RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                                org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, filter);

                        RenderSystem.setShaderTexture(0, textureId);
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        RenderSystem.enableBlend();

                        Matrix4f matrix = guiGraphics.pose().last().pose();
                        BufferBuilder buf = Tesselator.getInstance().getBuilder();
                        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                        float x0 = (float) screenLeft;
                        float y0 = (float) screenTop;
                        float x1 = (float) (screenLeft + regionPixels);
                        float y1 = (float) (screenTop + regionPixels);

                        buf.vertex(matrix, x0, y1, 0).uv(0, 1).endVertex();
                        buf.vertex(matrix, x1, y1, 0).uv(1, 1).endVertex();
                        buf.vertex(matrix, x1, y0, 0).uv(1, 0).endVertex();
                        buf.vertex(matrix, x0, y0, 0).uv(0, 0).endVertex();

                        BufferUploader.drawWithShader(buf.end());
                        RenderSystem.disableBlend();
                    }
                }

                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } finally {
                com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
            }

            return true;
        } catch (Throwable t) {
            LOGGER.debug("Simukraft: FTB enhanced render failed: {}", t.getMessage());
            return false;
        }
    }
}
