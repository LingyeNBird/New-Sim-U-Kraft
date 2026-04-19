package com.xiaoliang.simukraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class ChatNotificationHUD implements IGuiOverlay {
    public static final ChatNotificationHUD INSTANCE = new ChatNotificationHUD();
    
    private static final @Nonnull ResourceLocation ENVELOPE_TEXTURE = Objects.requireNonNull(ResourceLocation.parse("simukraft:textures/gui/e.png"));
    
    // 通知队列
    private final Queue<Notification> notificationQueue = new LinkedList<>();
    private Notification currentNotification = null;
    
    // 动画参数
    private static final int SLIDE_IN_DURATION = 300; // 滑入动画时长（毫秒）
    private static final int DISPLAY_DURATION = 3000; // 显示时长（毫秒）
    private static final int SLIDE_OUT_DURATION = 300; // 滑出动画时长（毫秒）
    private static final int NOTIFICATION_WIDTH = 180; // 通知宽度
    private static final int NOTIFICATION_HEIGHT = 55; // 通知高度（增加以容纳两行文字）
    private static final int ICON_SIZE = 24; // 图标大小
    private static final int PADDING = 8; // 内边距
    
    // 颜色
    private static final int COLOR_BACKGROUND = 0xFF16213E;
    private static final int COLOR_BORDER = 0xFFE94560;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    
    private static class Notification {
        int count; // 消息数量
        long startTime; // 开始时间
        State state; // 当前状态
        
        Notification(int count) {
            this.count = Math.min(count, 99); // 上限99
            this.startTime = System.currentTimeMillis();
            this.state = State.SLIDING_IN;
        }
        
        enum State {
            SLIDING_IN,   // 滑入中
            DISPLAYING,   // 显示中
            SLIDING_OUT   // 滑出中
        }
        
        void updateState() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (state == State.SLIDING_IN && elapsed >= SLIDE_IN_DURATION) {
                state = State.DISPLAYING;
                startTime = System.currentTimeMillis(); // 重置时间用于显示阶段
            } else if (state == State.DISPLAYING && elapsed >= DISPLAY_DURATION) {
                state = State.SLIDING_OUT;
                startTime = System.currentTimeMillis(); // 重置时间用于滑出阶段
            }
        }
        
        boolean isFinished() {
            if (state == State.SLIDING_OUT) {
                long elapsed = System.currentTimeMillis() - startTime;
                return elapsed >= SLIDE_OUT_DURATION;
            }
            return false;
        }
        
        // 获取当前X偏移量（用于滑入滑出动画）
        // 从右侧边框滑入：初始位置在屏幕右侧外（偏移为正），滑入到屏幕内（偏移为负）
        // 向右侧边框滑出：从屏幕内（偏移为负）滑向右侧外（偏移为正）
        float getXOffset() {
            long elapsed = System.currentTimeMillis() - startTime;
            switch (state) {
                case SLIDING_IN:
                    // 滑入：从右侧外（偏移=宽度）滑入到显示位置（偏移=-宽度）
                    float progress = Math.min(1.0f, (float) elapsed / SLIDE_IN_DURATION);
                    // 使用缓动函数
                    float eased = progress * progress * (3 - 2 * progress);
                    return NOTIFICATION_WIDTH - (NOTIFICATION_WIDTH * 2 * eased); // 从正到负
                case DISPLAYING:
                    return -NOTIFICATION_WIDTH; // 完全显示时的偏移
                case SLIDING_OUT:
                    // 滑出：从显示位置（偏移=-宽度）滑向右侧外（偏移=宽度）
                    progress = Math.min(1.0f, (float) elapsed / SLIDE_OUT_DURATION);
                    eased = progress * progress * (3 - 2 * progress);
                    return -NOTIFICATION_WIDTH + (NOTIFICATION_WIDTH * 2 * eased); // 从负到正
                default:
                    return 0;
            }
        }
    }
    
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        if (mc.player == null) {
            return;
        }
        
        // 更新当前通知状态
        if (currentNotification != null) {
            currentNotification.updateState();
            if (currentNotification.isFinished()) {
                currentNotification = null;
            }
        }
        
        // 如果没有当前通知，从队列中取一个
        if (currentNotification == null && !notificationQueue.isEmpty()) {
            currentNotification = notificationQueue.poll();
            // 播放提示音
            playNotificationSound();
        }
        
        // 渲染当前通知
        if (currentNotification != null) {
            renderNotification(guiGraphics, currentNotification, screenWidth, screenHeight);
        }
    }
    
    private void renderNotification(GuiGraphics guiGraphics, Notification notification, int screenWidth, int screenHeight) {
        // 计算位置（右上角，从右侧边框滑出滑入）
        float xOffset = notification.getXOffset();
        // 基础位置：屏幕右侧外（完全隐藏）
        int baseX = screenWidth - PADDING;
        // 加上偏移量：滑入时偏移为负（向左移动显示），滑出时偏移为正（向右移动隐藏）
        int x = baseX + (int) xOffset;
        int y = PADDING;
        
        // 保存矩阵状态
        guiGraphics.pose().pushPose();
        
        // 绘制背景
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 背景矩形
        guiGraphics.fill(x, y, x + NOTIFICATION_WIDTH, y + NOTIFICATION_HEIGHT, COLOR_BACKGROUND);
        // 边框
        guiGraphics.renderOutline(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, COLOR_BORDER);
        
        // 绘制信封图标
        RenderSystem.setShaderTexture(0, ENVELOPE_TEXTURE);
        int iconX = x + PADDING;
        int iconY = y + (NOTIFICATION_HEIGHT - ICON_SIZE) / 2;
        guiGraphics.blit(ENVELOPE_TEXTURE, iconX, iconY, ICON_SIZE, ICON_SIZE, 0, 0, 256, 256, 256, 256);
        
        // 绘制文字
        String text = "您有一条新消息";
        if (notification.count > 1) {
            text = "您有 " + notification.count + " 条新消息";
        }
        
        int textX = iconX + ICON_SIZE + PADDING;
        int textY = y + PADDING + 2;
        guiGraphics.drawString(Objects.requireNonNull(Minecraft.getInstance().font), text, textX, textY, COLOR_TEXT);
        
        // 绘制按键提示（第二行）
        String keyName = ChatKeyBindings.getOpenChatKey().getTranslatedKeyMessage().getString();
        String hintText = "按 \"" + keyName + "\" 查看";
        int hintTextY = textY + 12; // 第二行，间隔12像素
        guiGraphics.drawString(Objects.requireNonNull(Minecraft.getInstance().font), hintText, textX, hintTextY, COLOR_TEXT);
        
        // 恢复矩阵状态
        guiGraphics.pose().popPose();
    }
    
    private void playNotificationSound() {
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        if (mc.level != null) {
            mc.getSoundManager().play(
                Objects.requireNonNull(SimpleSoundInstance.forUI(Objects.requireNonNull(SoundEvents.NOTE_BLOCK_PLING.get()), 1.0F, 0.5F))
            );
        }
    }
    
    /**
     * 显示新消息通知
     * @param count 消息数量
     */
    public void showNotification(int count) {
        // 如果当前有正在显示的通知，合并数量
        if (currentNotification != null && currentNotification.state != Notification.State.SLIDING_OUT) {
            currentNotification.count = Math.min(currentNotification.count + count, 99);
            currentNotification.state = Notification.State.DISPLAYING;
            currentNotification.startTime = System.currentTimeMillis(); // 重置显示时间
            return;
        }
        
        // 如果队列中有通知，合并到最后一个
        if (!notificationQueue.isEmpty()) {
            Notification last = ((LinkedList<Notification>) notificationQueue).getLast();
            last.count = Math.min(last.count + count, 99);
            return;
        }
        
        // 创建新通知
        notificationQueue.offer(new Notification(count));
    }
    
    /**
     * 清除所有通知
     */
    public void clearNotifications() {
        notificationQueue.clear();
        currentNotification = null;
    }
}
