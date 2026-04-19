package com.xiaoliang.simukraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xiaoliang.simukraft.world.CityUpgradeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 升级节点画布类，基于CityMapCanvas修改
 */
@SuppressWarnings("null")
public class UpgradeCanvas extends AbstractWidget {
    // 地图相关字段
    private double zoomLevel = 4.0; // 缩放级别
    private double offsetX = 0; // 地图偏移X
    private double offsetY = 0; // 地图偏移Y
    private BlockPos cityCorePos; // 城市核心位置

    // 鼠标拖拽相关
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    // 悬浮信息
    private MapMarker hoveredMarker = null;
    // 选中的标记
    private MapMarker selectedMarker = null;

    // 标记列表
    private final List<MapMarker> markers = new ArrayList<>();
    
    /**
     * 地图标记类
     */
    public static class MapMarker {
        private final BlockPos pos; // 真实坐标
        private final int color; // 标记颜色
        private final String hoverText; // 悬浮文本
        
        public MapMarker(BlockPos pos, int color, String hoverText) {
            this.pos = pos;
            this.color = color;
            this.hoverText = hoverText;
        }
        
        public BlockPos getPos() {
            return pos;
        }
        
        public int getColor() {
            return color;
        }
        
        public String getHoverText() {
            return hoverText;
        }
    }

    public UpgradeCanvas(int x, int y, int width, int height, Screen parentScreen, BlockPos cityCorePos, int cityLevel) {
        super(x, y, width, height, Component.empty());
        this.cityCorePos = cityCorePos != null ? cityCorePos : new BlockPos(0, 0, 0);

        // 初始化地图中心位置为城市核心
        int coreCX = this.cityCorePos.getX() >> 4;
        int coreCZ = this.cityCorePos.getZ() >> 4;
        double chunkSize = 16 * zoomLevel;
        this.offsetX = -coreCX * chunkSize;
        this.offsetY = -coreCZ * chunkSize;

        // 加载城市升级节点
        loadCityUpgradeMarkers(cityLevel);
    }
    
    /**
     * 加载城市升级节点
     */
    private void loadCityUpgradeMarkers(int cityLevel) {
        CityUpgradeManager upgradeManager = CityUpgradeManager.getInstance();
        List<CityUpgradeManager.CityUpgrade> upgrades = upgradeManager.getAllUpgrades();
        clearMarkers();
    
        // 升级节点展开在城市核心附近，每个节点沿 Z 轴按 16 个方块间距致入 BlockPos
        for (int i = 0; i < upgrades.size(); i++) {
            CityUpgradeManager.CityUpgrade upgrade = upgrades.get(i);
            int level = upgrade.level();
            String name = upgrade.name();
    
            int color;
            if (level <= cityLevel) {
                color = 0xFF0000FF; // 已完成：蓝色
            } else if (level == cityLevel + 1) {
                color = 0xFF00FF00; // 可升级：绳色
            } else {
                color = 0xFFFF0000; // 未解锁：红色
            }
    
            String hoverText = level + ":" + name;
            // 节点从城市核心点开始，沿 Z 轴每级展开 4 个区块（16 格 * 4）
            int baseX = cityCorePos.getX();
            int baseZ = cityCorePos.getZ() + i * 64;
            addMarker(new MapMarker(new BlockPos(baseX, 0, baseZ), color, hoverText));
        }
    }
    
    /**
     * 添加标记
     */
    public void addMarker(MapMarker marker) {
        markers.add(marker);
    }
    
    /**
     * 移除标记
     */
    public boolean removeMarker(MapMarker marker) {
        return markers.remove(marker);
    }
    
    /**
     * 清空所有标记
     */
    public void clearMarkers() {
        markers.clear();
    }
    
    /**
     * 获取所有标记
     */
    public List<MapMarker> getMarkers() {
        return new ArrayList<>(markers);
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制画布背景（画框）
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x80000000);
        
        // 计算地图绘制区域
        int mapStartX = getX() + 10;
        int mapStartY = getY() + 30;
        int mapEndX = getX() + width - 10;
        int mapEndY = getY() + height - 10;
        int mapWidth = mapEndX - mapStartX;
        int mapHeight = mapEndY - mapStartY;
        
        // 绘制地图边框
        guiGraphics.fill(mapStartX - 2, mapStartY - 2, mapEndX + 2, mapEndY + 2, 0xFFFFFFFF);
        guiGraphics.fill(mapStartX - 1, mapStartY - 1, mapEndX + 1, mapEndY + 1, 0x80000000);
        
        // 绘制标题
        guiGraphics.drawString(nn(Minecraft.getInstance().font), nn(Component.translatable("gui.city_upgrade.title")), getX() + 5, getY() + 5, 0xFFFFFF);
        
        // 绘制地图内容
        renderMap(guiGraphics, mapStartX, mapStartY, mapWidth, mapHeight, mouseX, mouseY);
        
        // 绘制悬浮信息
        renderHoverInfo(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 绘制地图内容：世界地图底层 + 升级节点叠加
     */
    private void renderMap(GuiGraphics guiGraphics, int startX, int startY, int width, int height, int mouseX, int mouseY) {
        int centerX = startX + width / 2;
        int centerY = startY + height / 2;
        double chunkSize = 16 * zoomLevel;

        // 重置悬浮状态
        hoveredMarker = null;


        renderWorldMapTerrain(guiGraphics, startX, startY, width, height, centerX, centerY, chunkSize);

        // 2. 采集所有标记屏幕坐标
        List<double[]> allMarkerScreenPos = new ArrayList<>();
        List<MapMarker> allMapMarkers = new ArrayList<>();
        for (MapMarker marker : markers) {
            int markerChunkX = marker.getPos().getX() >> 4;
            int markerChunkZ = marker.getPos().getZ() >> 4;
            double sx = centerX + offsetX + markerChunkX * chunkSize + (marker.getPos().getX() & 15) * zoomLevel;
            double sy = centerY + offsetY + markerChunkZ * chunkSize + (marker.getPos().getZ() & 15) * zoomLevel;
            allMarkerScreenPos.add(new double[]{sx, sy});
            allMapMarkers.add(marker);
        }

        // 3. 继线（在标记图层之下）
        int canvasLeft = startX, canvasRight = startX + width;
        int canvasTop = startY, canvasBottom = startY + height;
        if (allMarkerScreenPos.size() > 1) {
            for (int i = 0; i < allMarkerScreenPos.size() - 1; i++) {
                double[] from = allMarkerScreenPos.get(i);
                double[] to   = allMarkerScreenPos.get(i + 1);
                int[] clipped = clipLine((int)from[0], (int)from[1], (int)to[0], (int)to[1],
                        canvasLeft, canvasTop, canvasRight, canvasBottom);
                if (clipped != null) {
                    drawLine(guiGraphics, clipped[0], clipped[1], clipped[2], clipped[3], 0xFFFFFFFF);
                }
            }
        }

        // 4. 标记点
        for (int i = 0; i < allMapMarkers.size(); i++) {
            MapMarker marker = allMapMarkers.get(i);
            double sx = allMarkerScreenPos.get(i)[0];
            double sy = allMarkerScreenPos.get(i)[1];
            if (sx >= startX - 3 && sx <= startX + width + 3 && sy >= startY - 3 && sy <= startY + height + 3) {
                if (marker == selectedMarker) {
                    guiGraphics.fill((int)(sx - 5), (int)(sy - 5), (int)(sx + 5), (int)(sy + 5), 0xFFFFFFFF);
                }
                guiGraphics.fill((int)(sx - 3), (int)(sy - 3), (int)(sx + 3), (int)(sy + 3), marker.getColor());
                guiGraphics.fill((int)(sx - 2), (int)(sy - 2), (int)(sx + 2), (int)(sy + 2), 0xFF4080FF);
                if (mouseX >= sx - 3 && mouseX <= sx + 3 && mouseY >= sy - 3 && mouseY <= sy + 3) {
                    hoveredMarker = marker;
                }
            }
        }
    }


    private void renderWorldMapTerrain(GuiGraphics guiGraphics, int startX, int startY,
                                       int width, int height,
                                       int centerX, int centerY, double chunkSize) {
        try {
            xaero.map.WorldMapSession session = xaero.map.WorldMapSession.getCurrentSession();
            if (session == null) return;
            xaero.map.MapProcessor mapProcessor = session.getMapProcessor();
            if (mapProcessor == null) return;
            if (mapProcessor.isRenderingPaused()) return;
            if (!mapProcessor.getMapSaveLoad().isRegionDetectionComplete()) return;

            int caveLayer = mapProcessor.getCurrentCaveLayer();

            int visibleChunksX = (int) Math.ceil(width  / chunkSize) + 2;
            int visibleChunksY = (int) Math.ceil(height / chunkSize) + 2;
            int startChunkX = (int) Math.floor((-offsetX - width  / 2.0) / chunkSize);
            int startChunkZ = (int) Math.floor((-offsetY - height / 2.0) / chunkSize);

            int startTCX = startChunkX >> 2;
            int startTCZ = startChunkZ >> 2;
            int endTCX   = (startChunkX + visibleChunksX + 3) >> 2;
            int endTCZ   = (startChunkZ + visibleChunksY + 3) >> 2;
            double tcScreenSize = chunkSize * 4.0;

            guiGraphics.flush();

            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int scissorX = (int)(startX * guiScale);
            int scissorY = (int)(Minecraft.getInstance().getWindow().getScreenHeight() - (startY + height) * guiScale);
            int scissorW = (int)(width  * guiScale);
            int scissorH = (int)(height * guiScale);
            com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
            com.mojang.blaze3d.platform.GlStateManager._scissorBox(scissorX, scissorY, scissorW, scissorH);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder vb = tesselator.getBuilder();
            org.joml.Matrix4f matrix = nn(guiGraphics.pose().last().pose());

            int lastRegX = Integer.MIN_VALUE, lastRegZ = Integer.MIN_VALUE;
            xaero.map.region.MapRegion lastRegion = null;

            for (int tcx = startTCX; tcx <= endTCX; tcx++) {
                for (int tcz = startTCZ; tcz <= endTCZ; tcz++) {
                    int regX = tcx >> 3, regZ = tcz >> 3;
                    int txInR = tcx & 7, tzInR = tcz & 7;

                    xaero.map.region.MapRegion region;
                    if (regX == lastRegX && regZ == lastRegZ) {
                        region = lastRegion;
                    } else {
                        try {
                            region = mapProcessor.getLeafMapRegion(caveLayer, regX, regZ, false);
                            if (region == null) {
                                region = mapProcessor.getLeafMapRegion(caveLayer, regX, regZ,
                                        mapProcessor.regionExists(caveLayer, regX, regZ));
                            }
                        } catch (Throwable t) {
                            region = null;
                        }
                        if (region != null && !mapProcessor.isUploadingPaused() && region.isLoaded()) {
                            nn(mapProcessor.getMapWorld().getCurrentDimension())
                                    .getLayeredMapRegions().bumpLoadedRegion(region);
                        }
                        // 请求加载（模仿 GuiMap：prioritize=true + canRequestReload_unsynced 完整状态检查）
                       if (region != null) {
                            synchronized (region) {
                               if (region.canRequestReload_unsynced()) {
                                    mapProcessor.getMapSaveLoad().requestLoad(region, "UpgradeCanvas");
                                }
                            }
                        }
                        lastRegX = regX;
                        lastRegZ = regZ;
                        lastRegion = region;
                    }
                    if (region == null) continue;

                    xaero.map.region.MapTileChunk tc = region.getChunk(txInR, tzInR);
                    if (tc == null) continue;
                    xaero.map.region.texture.LeafRegionTexture leaf = tc.getLeafTexture();
                    if (leaf == null) continue;
                    int texId = leaf.getGlColorTexture();
                    if (texId == -1) continue;

                    int chunkBaseX = tcx << 2, chunkBaseZ = tcz << 2;
                    double sL = centerX + offsetX + chunkBaseX * chunkSize;
                    double sT = centerY + offsetY + chunkBaseZ * chunkSize;
                    double sR = sL + tcScreenSize, sB = sT + tcScreenSize;

                    double cL = Math.max(sL, startX), cT = Math.max(sT, startY);
                    double cR = Math.min(sR, startX + width), cB = Math.min(sB, startY + height);
                    if (cR <= cL || cB <= cT) continue;

                    float u0 = (float)((cL - sL) / tcScreenSize), u1 = (float)((cR - sL) / tcScreenSize);
                    float v0 = (float)((cT - sT) / tcScreenSize), v1 = (float)((cB - sT) / tcScreenSize);
                    float x0 = (float)cL, y0 = (float)cT, x1 = (float)cR, y1 = (float)cB;

                    RenderSystem.setShaderTexture(0, texId);
                    vb.begin(nn(VertexFormat.Mode.QUADS), nn(DefaultVertexFormat.POSITION_TEX));
                    vb.vertex(matrix, x0, y1, 0).uv(u0, v1).endVertex();
                    vb.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
                    vb.vertex(matrix, x1, y0, 0).uv(u1, v0).endVertex();
                    vb.vertex(matrix, x0, y0, 0).uv(u0, v0).endVertex();
                    BufferUploader.drawWithShader(nn(vb.end()));
                }
            }

            com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
            RenderSystem.disableBlend();
        } catch (Throwable t) {
            // 世界地图不可用时静默失败
        }
    }
    
    /**
     * 使用Cohen-Sutherland算法裁剪直线
     * @return 裁剪后的直线坐标数组 [x1, y1, x2, y2]，如果直线完全在画布外则返回null
     */
    private int[] clipLine(int x1, int y1, int x2, int y2, int left, int top, int right, int bottom) {
        // 区域码定义
        final int LEFT = 1;   // 0001
        final int RIGHT = 2;  // 0010
        final int BOTTOM = 4; // 0100
        final int TOP = 8;    // 1000
        
        // 计算区域码
        int code1 = computeCode(x1, y1, left, top, right, bottom);
        int code2 = computeCode(x2, y2, left, top, right, bottom);
        
        // 循环裁剪
        while (true) {
            if ((code1 | code2) == 0) {
                // 直线完全在画布内
                return new int[]{x1, y1, x2, y2};
            } else if ((code1 & code2) != 0) {
                // 直线完全在画布外
                return null;
            } else {
                // 直线部分在画布内
                int codeOut = code1 != 0 ? code1 : code2;
                int x = 0, y = 0;
                
                // 计算直线与画布边界的交点
                if ((codeOut & TOP) != 0) {
                    x = x1 + (x2 - x1) * (top - y1) / (y2 - y1);
                    y = top;
                } else if ((codeOut & BOTTOM) != 0) {
                    x = x1 + (x2 - x1) * (bottom - y1) / (y2 - y1);
                    y = bottom;
                } else if ((codeOut & RIGHT) != 0) {
                    y = y1 + (y2 - y1) * (right - x1) / (x2 - x1);
                    x = right;
                } else if ((codeOut & LEFT) != 0) {
                    y = y1 + (y2 - y1) * (left - x1) / (x2 - x1);
                    x = left;
                }
                
                // 更新直线端点
                if (codeOut == code1) {
                    x1 = x;
                    y1 = y;
                    code1 = computeCode(x1, y1, left, top, right, bottom);
                } else {
                    x2 = x;
                    y2 = y;
                    code2 = computeCode(x2, y2, left, top, right, bottom);
                }
            }
        }
    }
    
    /**
     * 计算点的区域码
     */
    private int computeCode(int x, int y, int left, int top, int right, int bottom) {
        int code = 0;
        
        if (x < left) {
            code |= 1;   // LEFT
        } else if (x > right) {
            code |= 2;   // RIGHT
        }
        
        if (y < top) {
            code |= 8;   // TOP
        } else if (y > bottom) {
            code |= 4;   // BOTTOM
        }
        
        return code;
    }
    
    /**
     * 使用fill方法绘制一条直线
     */
    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // 计算线的方向和长度
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        // 使用Bresenham算法绘制直线
        while (true) {
            // 绘制当前点
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            
            // 检查是否到达终点
            if (x1 == x2 && y1 == y2) {
                break;
            }
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
    
    /**
     * 绘制悬浮信息
     */
    private void renderHoverInfo(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 优先显示标记的悬浮信息
        if (hoveredMarker != null) {
            // 构建悬浮文本
            String hoverText = hoveredMarker.getHoverText();
            
            // 将文本按换行符分割成多行
            String[] lines = hoverText.split("\\n");
            
            // 计算每行的宽度，找到最宽的一行
            int maxWidth = 0;
            for (String line : lines) {
                int lineWidth = Minecraft.getInstance().font.width(line);
                if (lineWidth > maxWidth) {
                    maxWidth = lineWidth;
                }
            }
            
            // 计算总高度
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int totalHeight = lines.length * lineHeight;
            
            // 计算悬浮框位置，避免超出屏幕
            int boxX = mouseX + 10;
            int boxY = mouseY - totalHeight - 5;
            
            // 调整位置，避免超出屏幕底部
            if (boxY < 0) {
                boxY = mouseY + 15;
            }
            
            // 绘制黄色悬浮框
            guiGraphics.fill(boxX - 2, boxY - 2, boxX + maxWidth + 2, boxY + totalHeight + 2, 0xFFFFAA00);
            guiGraphics.fill(boxX - 1, boxY - 1, boxX + maxWidth + 1, boxY + totalHeight + 1, 0xFFFFEEAA);
            
            // 逐行绘制文本
            int currentY = boxY;
            for (String line : lines) {
                guiGraphics.drawString(Minecraft.getInstance().font, line, boxX, currentY, 0x000000);
                currentY += lineHeight;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) {
        // 实现叙述逻辑
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理左键点击标记
        if (button == 0) {
            // 检查点击位置是否在画布内容区域（排除右侧面板区域）
            if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
                // 检查点击位置是否在右侧面板内（右侧面板宽度为屏幕宽度的1/3）
                int screenWidth = Minecraft.getInstance().screen.width;
                int panelWidth = screenWidth / 3;
                
                // 如果点击位置在右侧面板区域，不处理点击事件，让按钮处理
                if (mouseX >= screenWidth - panelWidth) {
                    return false;
                }
                
                // 检查是否点击了标记
                MapMarker clickedMarker = getMarkerAtPosition(mouseX, mouseY);
                if (clickedMarker != null) {
                    // 切换选中状态
                    selectedMarker = (selectedMarker == clickedMarker) ? null : clickedMarker;
                    return true;
                } else {
                    // 检查点击位置是否在画布内容区域（排除边框）
                    int mapStartX = getX() + 10;
                    int mapStartY = getY() + 30;
                    int mapEndX = getX() + width - 10;
                    int mapEndY = getY() + height - 10;
                    if (mouseX >= mapStartX && mouseX <= mapEndX && mouseY >= mapStartY && mouseY <= mapEndY) {
                        // 点击空白区域，取消选中状态
                        if (selectedMarker != null) {
                            selectedMarker = null;
                            return true;
                        }
                        // 处理拖拽开始
                        isDragging = true;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                        return true;
                    }
                }
            }
        }
        // 没有处理点击事件，让其他组件处理
        return false;
    }
    
    /**
     * 获取指定位置的标记
     */
    private MapMarker getMarkerAtPosition(double mouseX, double mouseY) {
        // 计算地图中心点
        int startX = getX() + 10;
        int startY = getY() + 30;
        int mapWidth = width - 20;
        int mapHeight = height - 40;
        int centerX = startX + mapWidth / 2;
        int centerY = startY + mapHeight / 2;
        double chunkSize = 16 * zoomLevel;
        
        for (MapMarker marker : markers) {
            // 将标记的真实坐标转换为屏幕坐标
            int markerChunkX = marker.getPos().getX() >> 4;
            int markerChunkZ = marker.getPos().getZ() >> 4;
            
            double markerScreenX = centerX + offsetX + markerChunkX * chunkSize + (marker.getPos().getX() & 15) * zoomLevel;
            double markerScreenY = centerY + offsetY + markerChunkZ * chunkSize + (marker.getPos().getZ() & 15) * zoomLevel;
            
            // 检查鼠标是否在标记范围内
            if (Math.abs(mouseX - markerScreenX) <= 3 && Math.abs(mouseY - markerScreenY) <= 3) {
                return marker;
            }
        }
        return null;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 处理左键拖拽结束
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 处理左键拖拽
        if (button == 0 && isDragging) {
            offsetX += mouseX - lastMouseX;
            offsetY += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 处理滚轮缩放
        double oldZoom = zoomLevel;
        
        if (delta > 0) {
            zoomLevel = Math.min(zoomLevel + 0.5, 8.0);
        } else {
            zoomLevel = Math.max(zoomLevel - 0.5, 4.0);
        }
        
        // 调整偏移量，使缩放中心在鼠标位置
        if (oldZoom != zoomLevel) {
            int mapStartX = getX() + 10;
            int mapStartY = getY() + 30;
            int centerX = mapStartX + width / 2;
            int centerY = mapStartY + height / 2;
            
            double mouseOffsetX = mouseX - centerX;
            double mouseOffsetY = mouseY - centerY;
            
            double scaleFactor = zoomLevel / oldZoom;
            offsetX = mouseOffsetX - (mouseOffsetX - offsetX) * scaleFactor;
            offsetY = mouseOffsetY - (mouseOffsetY - offsetY) * scaleFactor;
        }
        
        return true;
    }
    
    // Getter和Setter方法
    public double getZoomLevel() {
        return zoomLevel;
    }
    
    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = Math.max(4.0, Math.min(8.0, zoomLevel));
    }
    
    public double getOffsetX() {
        return offsetX;
    }
    
    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }
    
    public double getOffsetY() {
        return offsetY;
    }
    
    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }
    
    public BlockPos getCityCorePos() {
        return cityCorePos;
    }
    
    public void setCityCorePos(BlockPos cityCorePos) {
        this.cityCorePos = cityCorePos;
        // 重置地图中心到城市核心
        if (cityCorePos != null) {
            offsetX = 0;
            offsetY = 0;
        }
    }
    
    /**
     * 获取选中的标记
     */
    public MapMarker getSelectedMarker() {
        return selectedMarker;
    }
    
    /**
     * 设置选中的标记
     */
    public void setSelectedMarker(MapMarker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

}
