package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 聊天系统通用输入对话框
 * 统一风格的输入对话框
 */
public class ChatInputDialog extends Screen {
    
    private final Screen parent;
    private final Component title;
    private final Component hint;
    private final Consumer<String> onConfirm;
    private final Runnable onCancel;
    private final int maxLength;
    
    private EditBox inputField;
    private Button confirmButton;
    private Button cancelButton;
    
    // 颜色主题 - 与 ChatScreen 保持一致
    private static final int COLOR_BACKGROUND = 0xCC000000;
    private static final int COLOR_PANEL_BG = 0xFF16213E;
    private static final int COLOR_ACCENT = 0xFF0F3460;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;
    
    private static final int DIALOG_WIDTH = 300;
    private static final int DIALOG_HEIGHT = 120;
    
    public ChatInputDialog(Screen parent, Component title, Component hint, 
                          Consumer<String> onConfirm, Runnable onCancel) {
        this(parent, title, hint, onConfirm, onCancel, 100);
    }
    
    public ChatInputDialog(Screen parent, Component title, Component hint,
                          Consumer<String> onConfirm, Runnable onCancel, int maxLength) {
        super(title);
        this.parent = parent;
        this.title = title;
        this.hint = hint;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.maxLength = maxLength;
    }
    
    @Override
    protected void init() {
        var font = nn(this.font);
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        
        // 输入框
        EditBox inputFieldTmp = new EditBox(font, dialogX + 20, dialogY + 45, DIALOG_WIDTH - 40, 20, nn(hint));
        this.inputField = nn(inputFieldTmp);
        this.inputField.setMaxLength(maxLength);
        this.inputField.setValue("");
        this.inputField.setFocused(true);
        this.addRenderableWidget(nn(this.inputField));
        
        // 确认按钮
        this.confirmButton = nn(Button.builder(nn(Component.translatable("chat.simukraft.confirm")), btn -> {
            String value = inputField.getValue().trim();
            if (!value.isEmpty()) {
                onConfirm.accept(value);
                if (this.minecraft != null) {
                    this.minecraft.setScreen(parent);
                }
            }
        })
        .bounds(dialogX + DIALOG_WIDTH - 90, dialogY + DIALOG_HEIGHT - 35, 70, 20)
        .build());
        this.addRenderableWidget(this.confirmButton);
        
        // 取消按钮
        this.cancelButton = nn(Button.builder(nn(Component.translatable("chat.simukraft.cancel")), btn -> {
            onCancel.run();
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
        safeGuiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + 30, COLOR_ACCENT);
        
        // 标题
        int titleWidth = font.width(nn(title));
        safeGuiGraphics.drawString(font, nn(title), dialogX + (DIALOG_WIDTH - titleWidth) / 2, dialogY + 10, COLOR_TEXT_PRIMARY);
        
        // 提示文本
        safeGuiGraphics.drawString(font, nn(hint), dialogX + 20, dialogY + 35, COLOR_TEXT_SECONDARY);
        
        // 渲染组件
        super.render(safeGuiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) { // Enter
            confirmButton.onPress();
            return true;
        }
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
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }
}
