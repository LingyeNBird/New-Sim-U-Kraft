package com.xiaoliang.simukraft.client.map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 代表一个 512x512 方块区域的地图瓦片。
 * 管理数据层 ({@link SimuMapRegionData}) 和 GPU 纹理。
 * 参考 FTB Chunks 的 MapRegion 但完全独立。
 */
public class SimuMapRegion {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final int regionX;
    public final int regionZ;

    private SimuMapRegionData data;
    private NativeImage renderedImage;
    private int textureId = -1;
    private volatile boolean textureNeedsUpload = false;
    private volatile boolean imageLoaded = false;
    private long lastAccessTime;

    public SimuMapRegion(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 获取或创建区域数据。
     */
    public SimuMapRegionData getOrCreateData() {
        if (data == null) {
            data = new SimuMapRegionData(regionX, regionZ);
        }
        lastAccessTime = System.currentTimeMillis();
        return data;
    }

    /**
     * 直接设置区域数据（用于从磁盘加载时注入已反序列化的数据）。
     *
     * @param data 已填充好的区域数据，不得为 null
     */
    public void setData(SimuMapRegionData data) {
        this.data = data;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 获取区域数据（可能为 null）。
     */
    @Nullable
    public SimuMapRegionData getData() {
        if (data != null) {
            lastAccessTime = System.currentTimeMillis();
        }
        return data;
    }

    /**
     * 数据是否已加载。
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * 获取渲染图像（创建如果不存在）。
     */
    public NativeImage getOrCreateImage() {
        if (renderedImage == null) {
            renderedImage = new NativeImage(NativeImage.Format.RGBA, 512, 512, true);
            renderedImage.fillRect(0, 0, 512, 512, 0);
        }
        return renderedImage;
    }

    /**
     * 标记纹理需要上传到 GPU。
     */
    public void markTextureNeedsUpload() {
        textureNeedsUpload = true;
        imageLoaded = false;
    }

    /**
     * 获取 OpenGL 纹理 ID，并在需要时上传图像数据。
     * 必须在渲染线程调用。
     */
    public int getTextureId() {
        if (textureId == -1) {
            textureId = com.mojang.blaze3d.platform.TextureUtil.generateTextureId();
            com.mojang.blaze3d.platform.TextureUtil.prepareImage(textureId, 512, 512);
        }

        if (textureNeedsUpload && renderedImage != null) {
            textureNeedsUpload = false;
            if (RenderSystem.isOnRenderThreadOrInit()) {
                uploadNow();
            } else {
                Minecraft.getInstance().submit(this::uploadNow);
            }
        }

        return textureId;
    }

    private void uploadNow() {
        try {
            RenderSystem.bindTexture(textureId);
            synchronized (this) {
                if (renderedImage != null) {
                    renderedImage.upload(0, 0, 0, false);
                    imageLoaded = true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Simukraft: Failed to upload map region texture ({}, {})", regionX, regionZ, e);
        }
    }

    /**
     * 纹理是否已成功上传到 GPU。
     */
    public boolean isImageLoaded() {
        return imageLoaded;
    }

    /**
     * 上次访问时间。
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * 释放此区域占用的所有资源。
     */
    public void release() {
        synchronized (this) {
            if (renderedImage != null) {
                renderedImage.close();
                renderedImage = null;
            }
        }
        if (textureId != -1) {
            GlStateManager._deleteTexture(textureId);
            textureId = -1;
        }
        imageLoaded = false;
        data = null;
    }

    /**
     * 释放纹理但保留数据（用于节省显存）。
     */
    public void releaseTexture() {
        synchronized (this) {
            if (renderedImage != null) {
                renderedImage.close();
                renderedImage = null;
            }
        }
        if (textureId != -1) {
            GlStateManager._deleteTexture(textureId);
            textureId = -1;
        }
        imageLoaded = false;
    }

    /**
     * 仅释放内存数据。
     * 用于异步持久化完成后清理旧世界残留数据，避免再次触碰渲染线程资源。
     */
    public void discardData() {
        data = null;
    }

    /**
     * 到玩家的距离平方（用于排序）。
     */
    public double distToPlayer() {
        var player = Minecraft.getInstance().player;
        if (player == null) return Double.MAX_VALUE;
        double cx = regionX * 512.0 + 256.0;
        double cz = regionZ * 512.0 + 256.0;
        double dx = cx - player.getX();
        double dz = cz - player.getZ();
        return dx * dx + dz * dz;
    }

    /**
     * 区域字符串标识。
     */
    public String regionKey() {
        return regionX + "," + regionZ;
    }

    @Override
    public String toString() {
        return "SimuMapRegion[" + regionX + "," + regionZ + "]";
    }
}
