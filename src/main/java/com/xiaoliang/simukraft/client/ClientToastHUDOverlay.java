package com.xiaoliang.simukraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ClientToastHUDOverlay implements IGuiOverlay {
    public static final ClientToastHUDOverlay INSTANCE = new ClientToastHUDOverlay();
    private static final ResourceLocation W1_TEXTURE = ResourceLocation.parse("simukraft:textures/gui/w1.png");
    private static final ResourceLocation W2_TEXTURE = ResourceLocation.parse("simukraft:textures/gui/w2.png");
    private static final ResourceLocation G1_TEXTURE = ResourceLocation.parse("simukraft:textures/gui/g1.png");
    
    // 存储每个玩家的toast显示状态
    private static final Map<UUID, ToastInfo> playerToasts = new HashMap<>();
    
    private static class ToastInfo {
        final ResourceLocation texture;
        long startTime;
        final long duration;
        final com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade;
        
        ToastInfo(ResourceLocation texture, long duration, com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade) {
            this.texture = texture;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.upgrade = upgrade;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }
        
        void reset() {
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * 获取展开动画进度（0.0表示完全收起，1.0表示完全展开）
         */
        float getExpandProgress() {
            // 展开动画持续500毫秒
            long animationDuration = 500;
            long currentTime = System.currentTimeMillis() - startTime;
            
            if (currentTime >= animationDuration) {
                // 动画结束，完全展开
                return 1.0f;
            }
            
            // 使用缓动函数，让动画更流畅
            float progress = (float) currentTime / animationDuration;
            // 使用三次缓动函数：progress = progress * progress * (3 - 2 * progress)，这是一个常用的缓动函数，让动画先快后慢
            return progress * progress * (3 - 2 * progress);
        }
    }
    
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        if (mc.player == null) {
            return;
        }
        
        UUID playerId = mc.player.getUUID();
        ToastInfo toastInfo = playerToasts.get(playerId);
        
        if (toastInfo != null) {
            if (toastInfo.isExpired()) {
                // 移除过期的toast
                playerToasts.remove(playerId);
            } else {
                // 原图片尺寸
                int textureWidth = 256;
                int textureHeight = 64;
                
                // 使用固定的16:9宽高比
                double aspectRatio = 16.0 / 9.0;
                
                // 宽度优先，占满屏幕宽度
                int displayWidth = screenWidth;
                // 按16:9比例计算高度
                int displayHeight = (int) Math.round(displayWidth / aspectRatio);
                
                // 垂直居中
                int xPos = 0;
                int yPos = (screenHeight - displayHeight) / 2;
                
                // 计算屏幕中心位置
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                
                // 获取展开动画进度
                float expandProgress = toastInfo.getExpandProgress();
                
                // 保存当前矩阵，准备应用展开动画
                guiGraphics.pose().pushPose();
                
                // 1. 全透明背景，不绘制任何背景色
                // 移除半透明背景，实现全透明效果
                
                // 2. 应用缩放变换：从中心向四周缓慢展开
                guiGraphics.pose().translate(centerX, centerY, 0);
                guiGraphics.pose().scale(expandProgress, expandProgress, 1.0F);
                guiGraphics.pose().translate(-centerX, -centerY, 0);
                
                // 3. 绘制图片，使用展开进度控制
                guiGraphics.blit(
                        Objects.requireNonNull(toastInfo.texture),
                        xPos, yPos, 
                        displayWidth, displayHeight, 
                        0.0F, 0.0F, 
                        textureWidth, textureHeight, 
                        textureWidth, textureHeight
                );
                
                // 4. 绘制文本：在居中处绘制大字体（升级名称）和中字体（升级描述）
                var font = Objects.requireNonNull(Minecraft.getInstance().font);
                
                // 获取升级信息，用于显示文本
                String largeText = "未知";
                String mediumText = "";
                if (toastInfo.upgrade != null) {
                    largeText = toastInfo.upgrade.name();
                    mediumText = toastInfo.upgrade.description();
                }
                
                // 基于屏幕高度计算垂直偏移量
                // 大字体位于中轴线上方，偏移量为屏幕高度的10%
                int largeTextOffset = (int) (screenHeight * 0.10);
                // 中字体位于中轴线下方，偏移量为屏幕高度的5%
                int mediumTextOffset = (int) (screenHeight * 0.05);
                
                // 5. 大字体文本：升级名称
                // 计算大字体文本宽度
                int largeTextWidth = font.width(Objects.requireNonNull(largeText));
                // 大字体文本位置
                int largeTextX = centerX;
                int largeTextY = centerY - largeTextOffset;
                
                // 缩放大字体：放大4倍
                float largeScale = 4.0F;
                // 移动到文本中心位置
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(largeTextX, largeTextY, 0);
                guiGraphics.pose().scale(largeScale, largeScale, 1.0F);
                // 绘制大字体文本（使用描边效果）
                guiGraphics.drawString(font, largeText, -largeTextWidth / 2, 0, 0xFFFFFF, true);
                guiGraphics.pose().popPose();
                
                // 6. 中字体文本：升级描述
                // 计算中字体文本宽度
                int mediumTextWidth = font.width(Objects.requireNonNull(mediumText));
                // 中字体文本位置
                int mediumTextX = centerX;
                int mediumTextY = centerY + mediumTextOffset;
                
                // 缩放中字体：放大3倍
                float mediumScale = 3.0F;
                // 移动到文本中心位置
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(mediumTextX, mediumTextY, 0);
                guiGraphics.pose().scale(mediumScale, mediumScale, 1.0F);
                // 绘制中字体文本（使用描边效果）
                guiGraphics.drawString(font, mediumText, -mediumTextWidth / 2, 0, 0xFFFFFF, true);
                guiGraphics.pose().popPose();
                
                // 恢复矩阵
                guiGraphics.pose().popPose();
            }
        }
    }
    
    /**
     * 显示toast图片
     */
    public static void showToast(String type, int upgradeLevel, UUID playerId) {
        ResourceLocation texture;
        switch (type.toLowerCase()) {
            case "w1":
                texture = W1_TEXTURE;
                break;
            case "w2":
                texture = W2_TEXTURE;
                break;
            case "g1":
                texture = G1_TEXTURE;
                break;
            default:
                return;
        }
        
        // 获取升级信息，用于显示文本
        com.xiaoliang.simukraft.world.CityUpgradeManager upgradeManager = com.xiaoliang.simukraft.world.CityUpgradeManager.getInstance();
        com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade = upgradeManager.getUpgrade(upgradeLevel);
        
        // 播放完成音效
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        mc.getSoundManager().play(Objects.requireNonNull(SimpleSoundInstance.forUI(Objects.requireNonNull(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 1.0F, 1.0F)));
        
        // 显示toast
        ToastInfo toastInfo = playerToasts.get(playerId);
        if (toastInfo != null) {
            // 如果已有toast，重置时间
            toastInfo.reset();
        } else {
            // 创建新的toast，显示3秒
            playerToasts.put(playerId, new ToastInfo(texture, 3000, upgrade));
        }
    }
    
    /**
     * 显示toast图片（兼容旧版本）
     */
    public static void showToast(String type, UUID playerId) {
        showToast(type, 0, playerId);
    }
    
    /**
     * 清除指定玩家的toast
     */
    public static void clearToast(UUID playerId) {
        playerToasts.remove(playerId);
    }
}
