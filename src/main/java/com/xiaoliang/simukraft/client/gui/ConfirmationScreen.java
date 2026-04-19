package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConfirmationScreen extends Screen {
    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    private static final int BUTTON_WIDTH = 30;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_SPACING = 3;
    private static final int TOTAL_BUTTONS = 8;
    
    // 自定义按钮类，实现黑色背景和绿色高亮
    private static class ConfirmationButton extends AbstractWidget {
        private final char character;
        private final Runnable onClick;
        private boolean isPressed = false;
        
        public ConfirmationButton(int x, int y, int width, int height, char character, Runnable onClick) {
            super(x, y, width, height, nn(Component.literal(safeString(String.valueOf(character)))));
            this.character = character;
            this.onClick = onClick;
        }
        
        @Override
        public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 与面板相同的深灰色背景
            int backgroundColor = 0xCC333333; // 深灰色，与面板一致
            
            // 绘制按钮背景
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
            
            // 边框颜色：按下时为绿色，否则为白色
            int borderColor = isPressed ? 0xFF00FF00 : 0xFFFFFFFF; // 绿色或白色
            
            // 绘制边框
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            
            // 绘制字符（白色）
            int textColor = 0xFFFFFFFF; // 白色
            guiGraphics.drawCenteredString(
                nn(Minecraft.getInstance().font),
                nn(Component.literal(safeString(String.valueOf(character)))),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor
            );
        }
        
        @Override
        public void onClick(double mouseX, double mouseY) {
            this.isPressed = true;
            if (onClick != null) {
                onClick.run();
            }
        }
        
        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) {
            // 无障碍功能支持
        }
        
        public void resetPressedState() {
            this.isPressed = false;
        }
        
        public void setPressed(boolean pressed) {
            this.isPressed = pressed;
        }
    }
    
    private final List<Character> buttonChars;
    private final List<ConfirmationButton> buttons;
    private final Consumer<Boolean> callback;
    private final List<Character> expectedSequence;
    private final List<Character> pressedSequence;
    private boolean confirmed;
    private Component message;
    
    public ConfirmationScreen(Component title, Component message, Consumer<Boolean> callback) {
        super(title);
        this.message = message;
        this.callback = callback;
        this.confirmed = false;
        
        // 打乱的按钮字符：UIOPHJKL
        this.buttonChars = new ArrayList<>(Arrays.asList('U', 'I', 'O', 'P', 'H', 'J', 'K', 'L'));
        Collections.shuffle(this.buttonChars);
        
        this.buttons = new ArrayList<>();
        this.expectedSequence = Arrays.asList('O', 'K');
        this.pressedSequence = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 计算按钮总宽度
        int totalWidth = TOTAL_BUTTONS * BUTTON_WIDTH + (TOTAL_BUTTONS - 1) * BUTTON_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int buttonY = this.height / 2;
        
        // 创建按钮
        for (int i = 0; i < TOTAL_BUTTONS; i++) {
            final char currentChar = buttonChars.get(i);
            final int buttonIndex = i;
            
            ConfirmationButton button = new ConfirmationButton(
                startX + i * (BUTTON_WIDTH + BUTTON_SPACING),
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                currentChar,
                () -> onButtonClick(currentChar, buttonIndex)
            );
            
            this.addRenderableWidget(button);
            this.buttons.add(button);
        }
        
        // 添加取消按钮
        Button cancelButton = nn(Button.builder(
            nn(Component.translatable("gui.cancel")),
            btn -> onCancel()
        )
        .bounds(this.width / 2 - 50, this.height / 2 + 60, 100, 20)
        .build());
        
        this.addRenderableWidget(nn(cancelButton));
    }
    
    private void onButtonClick(char clickedChar, int buttonIndex) {
        // 添加按下的字符到序列
        pressedSequence.add(clickedChar);
        
        // 如果序列长度超过预期序列长度，移除最早的元素
        if (pressedSequence.size() > expectedSequence.size()) {
            pressedSequence.remove(0);
        }
        
        // 检查是否匹配预期序列
        if (pressedSequence.size() == expectedSequence.size()) {
            boolean matches = true;
            for (int i = 0; i < expectedSequence.size(); i++) {
                if (pressedSequence.get(i) != expectedSequence.get(i)) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                // 确认成功
                this.confirmed = true;
                if (callback != null) {
                    callback.accept(true);
                }
                this.onClose();
            }
        }
        
        // 更新按钮状态（可选：高亮显示按下的按钮）
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        // 重置所有按钮的按下状态
        for (ConfirmationButton button : buttons) {
            button.resetPressedState();
        }
        
        // 高亮显示最近按下的按钮
        if (!pressedSequence.isEmpty()) {
            char lastPressedChar = pressedSequence.get(pressedSequence.size() - 1);
            for (int i = 0; i < buttons.size(); i++) {
                if (buttonChars.get(i) == lastPressedChar) {
                    buttons.get(i).setPressed(true);
                    break;
                }
            }
        }
    }
    
    private void onCancel() {
        if (callback != null) {
            callback.accept(false);
        }
        this.onClose();
    }
    
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染半透明背景
        this.renderBackground(guiGraphics);
        
        // 渲染标题
        guiGraphics.drawCenteredString(
            nn(this.font),
            nn(this.title),
            this.width / 2,
            this.height / 2 - 60,
            0xFFFFFF
        );
        
        // 渲染消息
        if (this.message != null) {
            guiGraphics.drawCenteredString(
                nn(this.font),
                nn(this.message),
                this.width / 2,
                this.height / 2 - 40,
                0xCCCCCC
            );
        }
        
        // 渲染提示文本
        Component hintText = nn(Component.translatable("gui.confirmation.hint"));
        guiGraphics.drawCenteredString(
            nn(this.font),
            hintText,
            this.width / 2,
            this.height / 2 - 20,
            0xFFFF00
        );
        
        // 渲染当前按下的序列（可选）
        if (!pressedSequence.isEmpty()) {
            guiGraphics.drawCenteredString(
                nn(this.font),
                nn(Component.translatable("gui.confirmation.pressed_sequence", safeString(pressedSequence.toString()))),
                this.width / 2,
                this.height / 2 + 40,
                0x55FF55
            );
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void renderBackground(@Nonnull GuiGraphics guiGraphics) {
        // 渲染半透明黑色背景
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // 渲染一个居中的半透明面板
        int panelWidth = TOTAL_BUTTONS * BUTTON_WIDTH + (TOTAL_BUTTONS - 1) * BUTTON_SPACING + 40;
        int panelHeight = 150;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        
        // 面板背景（深灰色）
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC333333);
        
        // 面板边框
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFFFFFFFF);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void onClose() {
        // 如果用户直接关闭屏幕而没有确认，调用回调
        if (!confirmed && callback != null) {
            callback.accept(false);
        }
        super.onClose();
    }
    
    // 静态方法，方便调用
    public static void open(Component title, Component message, Consumer<Boolean> callback) {
        Minecraft.getInstance().setScreen(new ConfirmationScreen(title, message, callback));
    }
    
    public static void openForNPCRename(String npcName, Consumer<Boolean> callback) {
        Component title = Component.translatable("gui.confirmation.npc_rename.title");
        Component message = Component.translatable("gui.confirmation.npc_rename.message", safeString(npcName));
        open(title, message, callback);
    }
    
    public static void openForNPCDelete(String npcName, Consumer<Boolean> callback) {
        Component title = Component.translatable("gui.confirmation.npc_delete.title");
        Component message = Component.translatable("gui.confirmation.npc_delete.message", npcName);
        open(title, message, callback);
    }
    
    public static void openForCityDelete(net.minecraft.core.BlockPos cityCorePos, Consumer<Boolean> callback) {
        Component title = Component.translatable("gui.confirmation.city_delete.title");
        Component message = Component.translatable("gui.confirmation.city_delete.message");
        open(title, message, callback);
    }
    
    public static void openForOfficialRemove(String playerName, Consumer<Boolean> callback) {
        Component title = Component.translatable("gui.confirmation.official_remove.title");
        Component message = Component.translatable("gui.confirmation.official_remove.message", playerName);
        open(title, message, callback);
    }
}
