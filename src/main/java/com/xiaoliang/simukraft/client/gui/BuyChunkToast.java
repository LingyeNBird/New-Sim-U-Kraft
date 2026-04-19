package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nonnull;
import java.util.Objects;

public class BuyChunkToast extends AbstractWidget {
    private final ChunkPos chunkPos;
    private final double cost;
    private final boolean success;
    private final String errorMessage;
    private int animationTime = 0;
    private final int totalAnimationTime = 3000; // 总动画时间（毫秒）
    private boolean isVisible = true;
    private final int slideInTime = 300; // 滑入时间（毫秒）
    private final int displayTime = 2000; // 显示时间（毫秒）
    private final int slideOutTime = 700; // 滑出时间（毫秒）
    private final int originalX; // 原始X坐标（屏幕外）
    private final int originalY; // 原始Y坐标（屏幕外）
    private final int targetX; // 目标X坐标（屏幕内）

    public BuyChunkToast(int x, int y, ChunkPos chunkPos, double cost, boolean success, String errorMessage) {
        super(x, y, 200, 50, Component.empty());
        this.chunkPos = chunkPos;
        this.cost = cost;
        this.success = success;
        this.errorMessage = errorMessage;
        
        // 设置目标坐标（屏幕内）
        this.targetX = x;
        
        // 设置原始坐标（屏幕外，从右下角滑入）
        this.originalX = x + this.width;
        this.originalY = y;
        
        // 初始位置设置为屏幕外
        this.setX(originalX);
        this.setY(originalY);
        
        // 购买成功时
        if (success) {
            // 播放进入音效
            playUiSound(SoundEvents.UI_TOAST_IN);
            // 播放音效
            playUiSound(SoundEvents.UI_TOAST_IN);
        } else {
            // 播放进入音效
            playUiSound(SoundEvents.UI_TOAST_IN);
        }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics safeGuiGraphics = nn(guiGraphics);
        var font = nn(Minecraft.getInstance().font);

        if (!isVisible) {
            return;
        }

        // 保持完全不透明，不再使用淡入淡出效果
        int bgAlpha = 128; // 半透明背景
        int backgroundColor = (bgAlpha << 24) | 0x333333; // ARGB格式
        
        // 绘制背景
        safeGuiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
        
        // 绘制边框
        int borderColor = (bgAlpha << 24) | 0x000000; // 黑色边框
        safeGuiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor); // 上边框
        safeGuiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor); // 下边框
        safeGuiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor); // 左边框
        safeGuiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor); // 右边框
        
        // 绘制标题
        Component title;
        int titleColor;
        int textAlpha = 255; // 完全不透明文字
        
        if (success) {
            // 购买成功
            title = Component.translatable("gui.buy_chunk_toast.title");
            titleColor = (textAlpha << 24) | 0x00FF00; // 绿色标题
            
            // 绘制区块位置
            String chunkCoords = safeString(String.format("(%d, %d)", chunkPos.x, chunkPos.z));
            Component coords = nn(Component.literal(chunkCoords));
            int coordsColor = (textAlpha << 24) | 0xFFFFFF; // 白色区块坐标
            safeGuiGraphics.drawString(font, coords, this.getX() + 10, this.getY() + 25, coordsColor);
            
            // 绘制花费金额
            String costText = safeString(String.format("$%.2f", cost));
            Component costComponent = nn(Component.literal(costText));
            int costColor = (textAlpha << 24) | 0x00FF00; // 绿色花费金额
            safeGuiGraphics.drawString(font, costComponent, this.getX() + 10, this.getY() + 40, costColor);
        } else {
            // 购买失败
            title = nn(Component.translatable("gui.buy_chunk_toast.failed"));
            titleColor = (textAlpha << 24) | 0xFF0000; // 红色标题
            
            // 绘制错误信息
            Component errorComponent = errorMessage != null && !errorMessage.isEmpty()
                    ? nn(Component.translatable(safeString(errorMessage)))
                    : nn(Component.empty());
            int errorColor = (textAlpha << 24) | 0xFFFFFF; // 白色错误信息
            safeGuiGraphics.drawString(font, errorComponent, this.getX() + 10, this.getY() + 25, errorColor);
        }
        
        safeGuiGraphics.drawString(font, nn(title), this.getX() + 10, this.getY() + 10, titleColor);
    }

    @Override
    public void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) {
        // 不需要旁白
    }

    /**
     * 更新动画
     */
    public void update() {
        animationTime++;
        
        // 计算当前时间（毫秒）
        int currentTime = animationTime * 16; // 假设每帧16毫秒
        
        // 1. 滑入阶段
        if (currentTime < slideInTime) {
            // 从屏幕外滑入到目标位置
            float progress = (float) currentTime / slideInTime;
            int x = (int) (originalX + (targetX - originalX) * progress);
            this.setX(x);
        }
        // 2. 显示阶段
        else if (currentTime < slideInTime + displayTime) {
            // 停留在目标位置
            this.setX(targetX);
        }
        // 3. 滑出阶段
        else if (currentTime < totalAnimationTime) {
            // 从目标位置滑出到屏幕外
            float progress = (float) (currentTime - slideInTime - displayTime) / slideOutTime;
            int x = (int) (targetX + (originalX - targetX) * progress);
            this.setX(x);
        }
        // 4. 动画结束
        else {
            isVisible = false;
            // 播放退出音效
            playUiSound(SoundEvents.UI_TOAST_OUT);
        }
    }

    /**
     * 检查弹窗是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 重置动画时间，延长显示时间
     */
    public void resetAnimation() {
        animationTime = 0;
        // 确保弹窗可见
        isVisible = true;
    }

    private static void playUiSound(SoundEvent soundEvent) {
        Minecraft.getInstance().getSoundManager().play(
                nn(SimpleSoundInstance.forUI(nn(soundEvent), 1.0F))
        );
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(String value) {
        return nn(value);
    }
}
