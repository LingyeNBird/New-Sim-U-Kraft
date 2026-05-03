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

public class CityInfoScreen extends AbstractTransitionScreen {
    private final BlockPos cityCorePos;
    private final String cityName;
    private final int population;
    private final String mayorName;

    public CityInfoScreen(BlockPos cityCorePos, String cityName, int population, String mayorName) {
        super(Component.translatable("gui.city_info.title"));
        this.cityCorePos = cityCorePos;
        this.cityName = cityName;
        this.population = population;
        this.mayorName = mayorName;
        playOpenSound();
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 创建返回按钮
        Button backButton = nn(Button.builder(
            nn(Component.translatable("gui.city_info.back")),
            button -> this.closeScreen()
        ).pos(centerX - 50, centerY + 80).size(100, 20).build());
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
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_info.title")), centerX - 50, centerY - 60, 0xFFFFFF);
        
        // 渲染城市基本信息
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_info.name", cityName)), centerX - 100, centerY - 30, 0xAAAAAA);
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_info.population", population)), centerX - 100, centerY - 10, 0xAAAAAA);
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_info.mayor", mayorName)), centerX - 100, centerY + 10, 0xAAAAAA);
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_info.core_pos", cityCorePos.getX(), cityCorePos.getY(), cityCorePos.getZ())), centerX - 100, centerY + 30, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
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
