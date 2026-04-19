package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.SubmitFirstWorldModeSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

@SuppressWarnings("null")
public class FirstWorldModeSelectionScreen extends Screen {
    private boolean allowClose = false;

    public FirstWorldModeSelectionScreen() {
        super(Component.translatable("gui.first_world_mode.title"));
    }

    @Override
    @SuppressWarnings("null")
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 150;
        int gap = 12;

        Button normalButton = Objects.requireNonNull(Button.builder(
                        Component.translatable("gui.first_world_mode.normal"),
                        button -> submit(false))
                .pos(centerX - buttonWidth - gap / 2, centerY + 30)
                .size(buttonWidth, 20)
                .build());
        this.addRenderableWidget(normalButton);

        Button expertButton = Objects.requireNonNull(Button.builder(
                        Component.translatable("gui.first_world_mode.expert"),
                        button -> submit(true))
                .pos(centerX + gap / 2, centerY + 30)
                .size(buttonWidth, 20)
                .build());
        this.addRenderableWidget(expertButton);
    }

    private void submit(boolean enableExpertMode) {
        NetworkManager.sendToServer(new SubmitFirstWorldModeSelectionPacket(enableExpertMode));
        allowClose = true;
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 70, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.first_world_mode.subtitle"),
                centerX, centerY - 48, 0xBFBFBF);

        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.first_world_mode.normal.desc"),
                centerX, centerY - 8, 0x55FF55);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.first_world_mode.expert.desc"),
                centerX, centerY + 8, 0xFFAA55);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (!allowClose) {
            return;
        }
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
