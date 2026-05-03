package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 聊天系统通用确认对话框
 * 统一风格的确认对话框
 */
public class ChatConfirmDialog extends Screen {
    
    private final Screen parent;
    private final Component message;
    private final Consumer<Boolean> onResult;
    
    private Button confirmButton;
    private Button cancelButton;
    
    // 颜色主题 - 与 ChatScreen 保持一致
    private static final int COLOR_BACKGROUND = 0xCC000000;
    private static final int COLOR_PANEL_BG = 0xFF16213E;
    private static final int COLOR_ACCENT = 0xFF0F3460;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;
    
    private static final int DIALOG_WIDTH = 320;
    private static final int DIALOG_HEIGHT = 140;
    
    public ChatConfirmDialog(Screen parent, Component title, Component message, Consumer<Boolean> onResult) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onResult = onResult;
    }
    
    @Override
    protected void init() {
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        
        // 确认按钮
        this.confirmButton = nn(Button.builder(nn(Component.translatable("chat.simukraft.confirm")), btn -> {
            onResult.accept(true);
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        })
        .bounds(dialogX + DIALOG_WIDTH - 90, dialogY + DIALOG_HEIGHT - 35, 70, 20)
        .build());
        this.addRenderableWidget(this.confirmButton);
        
        // 取消按钮
        this.cancelButton = nn(Button.builder(nn(Component.translatable("chat.simukraft.cancel")), btn -> {
            onResult.accept(false);
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        })
        .bounds(dialogX + 20, dialogY + DIALOG_HEIGHT - 35, 70, 20)
        .build());
        this.addRenderableWidget(this.cancelButton);
    }
    
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphics safeGuiGraphics = nn(guiGraphics);
        var font = nn(this.font);
        // 渲染半透明背景
        safeGuiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        
        // 对话框背景
        safeGuiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, COLOR_PANEL_BG);
        
        // 标题栏
        safeGuiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + 35, COLOR_ACCENT);
        
        // 标题
        int titleWidth = font.width(nn(title));
        safeGuiGraphics.drawString(font, nn(title), dialogX + (DIALOG_WIDTH - titleWidth) / 2, dialogY + 12, COLOR_TEXT_PRIMARY);
        
        // 消息文本（自动换行）
        int maxLineWidth = DIALOG_WIDTH - 40;
        String msg = safeString(nn(message).getString());
        int lineY = dialogY + 50;
        
        // 简单处理：如果消息太长，分段显示
        if (font.width(msg) > maxLineWidth) {
            StringBuilder currentLine = new StringBuilder();
            for (char c : msg.toCharArray()) {
                String candidate = safeString(currentLine.toString() + c);
                if (font.width(candidate) > maxLineWidth) {
                    safeGuiGraphics.drawString(font, safeString(currentLine.toString()), dialogX + 20, lineY, COLOR_TEXT_SECONDARY);
                    lineY += 12;
                    currentLine = new StringBuilder();
                }
                currentLine.append(c);
            }
            if (currentLine.length() > 0) {
                safeGuiGraphics.drawString(font, safeString(currentLine.toString()), dialogX + 20, lineY, COLOR_TEXT_SECONDARY);
            }
        } else {
            safeGuiGraphics.drawString(font, nn(message), dialogX + 20, lineY, COLOR_TEXT_SECONDARY);
        }
        
        // 渲染组件
        super.render(safeGuiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            cancelButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
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
