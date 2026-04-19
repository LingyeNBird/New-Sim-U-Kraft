package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CityFinanceScreen extends AbstractTransitionScreen {
    private final BlockPos cityCorePos;

    public CityFinanceScreen(BlockPos cityCorePos) {
        super(Component.translatable("gui.city_finance.title"));
        this.cityCorePos = cityCorePos;
        playOpenSound();
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 创建返回按钮
        Button backButton = nn(Button.builder(
            nn(Component.translatable("gui.city_finance.back")),
            button -> this.closeScreen()
        ).pos(centerX - 50, centerY + 100).size(100, 20).build());
        this.addRenderableWidget(backButton);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics safeGuiGraphics = nn(guiGraphics);
        var font = nn(this.font);
        super.render(safeGuiGraphics, mouseX, mouseY, partialTicks);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 渲染标题
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_finance.title")), centerX - 50, centerY - 80, 0xFFFFFF);
        
        // 渲染功能区域占位符
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_finance.placeholder")), centerX - 80, centerY - 20, 0x888888);
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new CityManagementScreen(cityCorePos));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void playOpenSound() {
        Minecraft.getInstance().getSoundManager().play(
                nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.CITY_CORE_OPEN.get()), 1.0F))
        );
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }
}
