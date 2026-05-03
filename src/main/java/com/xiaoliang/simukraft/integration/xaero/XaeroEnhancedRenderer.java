package com.xiaoliang.simukraft.integration.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;

/**
 * Xaero World Map 增强渲染器。
 * 当 Xaero's World Map 可用时，利用其已渲染的地图纹理提供更高质量的地形显示。
 * <p>
 * 此类仅在 {@link com.xiaoliang.simukraft.integration.ModIntegrationManager#isXaeroWorldMapPresent()}
 * 返回 true 时才会被调用（通过完全限定名引用），确保在 Xaero 不存在时不会加载此类。
 * <p>
 * 所有 Xaero API 调用均包裹在 try-catch 中，渲染失败时返回 false，
 * 调用方会降级到自有渲染系统。
 */
public class XaeroEnhancedRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private XaeroEnhancedRenderer() {
    }

    /**
     * 使用 Xaero World Map 的纹理渲染地形。
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
    public static boolean renderXaeroTerrain(GuiGraphics guiGraphics,
                                              int startX, int startY, int width, int height,
                                              int centerX, int centerY,
                                              double offsetX, double offsetY,
                                              double zoomLevel,
                                              int startChunkX, int startChunkZ,
                                              int visibleChunksX, int visibleChunksZ) {
        try {
            xaero.map.WorldMapSession session = xaero.map.WorldMapSession.getCurrentSession();
            if (session == null || !session.isUsable()) return false;

            xaero.map.MapProcessor mapProcessor = session.getMapProcessor();
            if (mapProcessor == null) return false;

            synchronized (mapProcessor.renderThreadPauseSync) {
                if (mapProcessor.isRenderingPaused()) return false;
                if (!mapProcessor.isMapWorldUsable()) return false;
                if (!mapProcessor.getMapSaveLoad().isRegionDetectionComplete()) return false;

                xaero.map.world.MapDimension currentDim = mapProcessor.getMapWorld().getCurrentDimension();
                if (currentDim == null) return false;

                final int caveLayer = Integer.MAX_VALUE;

                int startBlockX = startChunkX << 4;
                int startBlockZ = startChunkZ << 4;
                int endBlockX = (startChunkX + visibleChunksX) << 4;
                int endBlockZ = (startChunkZ + visibleChunksZ) << 4;

                int minRegX = startBlockX >> 9;
                int maxRegX = endBlockX >> 9;
                int minRegZ = startBlockZ >> 9;
                int maxRegZ = endBlockZ >> 9;

                int viewCenterBlockX = (startBlockX + endBlockX) / 2;
                int viewCenterBlockZ = (startBlockZ + endBlockZ) / 2;
                int viewCenterRegX = viewCenterBlockX >> 9;
                int viewCenterRegZ = viewCenterBlockZ >> 9;

                xaero.map.region.LeveledRegion.setComparison(
                        viewCenterRegX, viewCenterRegZ, 0, viewCenterRegX, viewCenterRegZ);

                guiGraphics.flush();
                double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
                int scissorX = (int) (startX * guiScale);
                int scissorY = (int) ((Minecraft.getInstance().getWindow().getScreenHeight()
                        - (startY + height) * guiScale));
                int scissorW = (int) (width * guiScale);
                int scissorH = (int) (height * guiScale);

                com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
                com.mojang.blaze3d.platform.GlStateManager._scissorBox(scissorX, scissorY, scissorW, scissorH);

                try {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

                    xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider rendererProvider =
                            mapProcessor.getMultiTextureRenderTypeRenderers();
                    xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer withLightRenderer =
                            rendererProvider.getRenderer(
                                    (t) -> RenderSystem.setShaderTexture(0, t),
                                    xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                                    xaero.map.graphics.CustomRenderTypes.MAP);
                    xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer noLightRenderer =
                            rendererProvider.getRenderer(
                                    (t) -> RenderSystem.setShaderTexture(0, t),
                                    xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                                    xaero.map.graphics.CustomRenderTypes.MAP);

                    org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();

                    java.util.List<xaero.map.region.MapRegion> regionsNeedingLoad = new java.util.ArrayList<>();

                    boolean isUploadingPaused = mapProcessor.isUploadingPaused();
                    boolean pauseRequests = xaero.map.WorldMap.pauseRequests;
                    xaero.map.region.LayeredRegionManager layeredRegions = currentDim.getLayeredMapRegions();

                    for (int regX = minRegX; regX <= maxRegX; regX++) {
                        for (int regZ = minRegZ; regZ <= maxRegZ; regZ++) {
                            xaero.map.region.MapRegion region =
                                    mapProcessor.getLeafMapRegion(caveLayer, regX, regZ, false);
                            if (region == null) {
                                region = mapProcessor.getLeafMapRegion(caveLayer, regX, regZ,
                                        mapProcessor.regionExists(caveLayer, regX, regZ));
                            }
                            if (region == null) continue;

                            if (!isUploadingPaused && !pauseRequests) {
                                layeredRegions.bumpLoadedRegion(region);
                            }

                            synchronized (region) {
                                if (region.canRequestReload_unsynced()) {
                                    region.calculateSortingDistance();
                                    xaero.map.misc.Misc.addToListOfSmallest(10, regionsNeedingLoad, region);
                                }
                            }

                            if (!region.hasTextures()) continue;

                            for (int tx = 0; tx < 8; tx++) {
                                for (int tz = 0; tz < 8; tz++) {
                                    xaero.map.region.texture.RegionTexture<?> regionTexture =
                                            region.getTexture(tx, tz);
                                    if (regionTexture == null) continue;
                                    int textureId = regionTexture.getGlColorTexture();
                                    if (textureId == -1) continue;

                                    int texBlockX = (regX << 9) + (tx << 6);
                                    int texBlockZ = (regZ << 9) + (tz << 6);

                                    double screenLeft = centerX + offsetX + texBlockX * zoomLevel;
                                    double screenTop = centerY + offsetY + texBlockZ * zoomLevel;
                                    double screenRight = screenLeft + 64.0 * zoomLevel;
                                    double screenBottom = screenTop + 64.0 * zoomLevel;

                                    if (screenRight < startX || screenLeft > startX + width
                                            || screenBottom < startY || screenTop > startY + height) {
                                        continue;
                                    }

                                    float x0 = (float) screenLeft;
                                    float y0 = (float) screenTop;
                                    float tileW = (float) (screenRight - screenLeft);
                                    float tileH = (float) (screenBottom - screenTop);

                                    boolean hasLight = regionTexture.getTextureHasLight();
                                    xaero.map.gui.GuiMap.renderTexturedModalRectWithLighting3(
                                            matrix, x0, y0, tileW, tileH, textureId,
                                            hasLight, hasLight ? withLightRenderer : noLightRenderer);
                                }
                            }
                        }
                    }

                    xaero.lib.client.graphics.shader.LibShaders.WORLD_MAP.setBrightness(1.0f);
                    xaero.lib.client.graphics.shader.LibShaders.WORLD_MAP.setWithLight(true);
                    rendererProvider.draw(withLightRenderer);
                    xaero.lib.client.graphics.shader.LibShaders.WORLD_MAP.setWithLight(false);
                    rendererProvider.draw(noLightRenderer);

                    handleRegionLoadRequests(mapProcessor, regionsNeedingLoad, pauseRequests);

                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                } finally {
                    com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
                }
            }

            return true;
        } catch (Throwable t) {
            LOGGER.debug("Simukraft: Xaero enhanced render failed: {}", t.getMessage());
            return false;
        }
    }

    /**
     * 处理 Xaero 区域加载请求。
     */
    private static void handleRegionLoadRequests(xaero.map.MapProcessor mapProcessor,
                                                  java.util.List<xaero.map.region.MapRegion> regionsNeedingLoad,
                                                  boolean pauseRequests) {
        try {
            xaero.map.region.LeveledRegion<?> nextToLoad =
                    mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
            boolean shouldRequest;
            if (nextToLoad != null) {
                shouldRequest = nextToLoad.shouldAllowAnotherRegionToLoad();
            } else {
                shouldRequest = true;
            }
            shouldRequest = shouldRequest
                    && mapProcessor.getAffectingLoadingFrequencyCount() < 16;

            if (shouldRequest && !pauseRequests) {
                int loadRequested = 0;
                for (int i = 0; i < regionsNeedingLoad.size() && loadRequested < 1; i++) {
                    xaero.map.region.MapRegion regionToLoad = regionsNeedingLoad.get(i);
                    if (regionToLoad != nextToLoad || regionsNeedingLoad.size() <= 1) {
                        synchronized (regionToLoad) {
                            if (regionToLoad.canRequestReload_unsynced()) {
                                if (regionToLoad.getLoadState() == 2) {
                                    regionToLoad.requestRefresh(mapProcessor);
                                } else {
                                    mapProcessor.getMapSaveLoad().requestLoad(regionToLoad, "SimukraftMap");
                                }
                                if (loadRequested == 0) {
                                    mapProcessor.getMapSaveLoad().setNextToLoadByViewing(regionToLoad);
                                }
                                loadRequested++;
                                if (regionToLoad.getLoadState() == 4) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Simukraft: Xaero region load request failed: {}", t.getMessage());
        }
    }
}
