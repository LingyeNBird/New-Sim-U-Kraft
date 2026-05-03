package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 消息提示界面
 * 用于显示操作结果提示
 */
public class MessageScreen extends Screen {
    private final Screen parent;
    private final Component message;
    private final Component detail;
    private int displayTime = 0;
    private static final int AUTO_CLOSE_TIME = 60; // 3秒后自动关闭

    public MessageScreen(Screen parent, Component message, Component detail) {
        super(message);
        this.parent = parent;
        this.message = message;
        this.detail = detail;
    }

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 确定按钮
        this.addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.button.confirm")), button -> onClose())
                .pos(centerX - 50, centerY + 40)
                .size(100, 20)
                .build()));
    }

    @Override
    public void tick() {
        super.tick();
        displayTime++;
        if (displayTime >= AUTO_CLOSE_TIME) {
            onClose();
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 绘制主消息
        guiGraphics.drawCenteredString(nn(this.font), nn(message), centerX, centerY - 20, 0x55FF55);

        // 绘制详细信息
        guiGraphics.drawCenteredString(nn(this.font), nn(detail), centerX, centerY + 5, 0xAAAAAA);

        // 绘制自动关闭提示
        int remaining = (AUTO_CLOSE_TIME - displayTime) / 20 + 1;
        guiGraphics.drawCenteredString(nn(this.font),
                nn(Component.literal("(" + remaining + "秒后自动关闭)")),
                centerX, centerY + 25, 0x888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
