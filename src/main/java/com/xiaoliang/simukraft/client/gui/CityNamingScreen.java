package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.CreateCityPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CityNamingScreen extends AbstractTransitionScreen {
    private EditBox cityNameField;
    private final BlockPos cityCorePos;
    private String errorMessage = "";

    public CityNamingScreen(BlockPos cityCorePos) {
        super(Component.translatable("gui.city_naming.title"));
        this.cityCorePos = cityCorePos;
        playOpenSound();
    }

    @Override
    protected void init() {
        super.init();
        var font = nn(this.font);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 创建文本输入框
        this.cityNameField = nn(new EditBox(
            font,
            centerX - 100,
            centerY - 10,
            200,
            20,
            nn(Component.translatable("gui.city_naming.city_name"))
        ));
        this.cityNameField.setMaxLength(20);
        this.cityNameField.setValue("");
        this.addWidget(this.cityNameField);
        
        // 创建确认按钮
        Button confirmButton = nn(Button.builder(
            nn(Component.translatable("gui.city_naming.confirm")),
            button -> this.onConfirm()
        ).pos(centerX - 50, centerY + 20).size(100, 20).build());
        this.addRenderableWidget(confirmButton);
        
        // 创建取消按钮
        Button cancelButton = nn(Button.builder(
            nn(Component.translatable("gui.city_naming.cancel")),
            button -> this.closeScreen()
        ).pos(centerX - 50, centerY + 45).size(100, 20).build());
        this.addRenderableWidget(cancelButton);
        
        this.setInitialFocus(nn(this.cityNameField));
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics safeGuiGraphics = nn(guiGraphics);
        var font = nn(this.font);
        super.render(safeGuiGraphics, mouseX, mouseY, partialTicks);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 渲染标题
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_naming.title")), centerX - 30, centerY - 100, 0xFFFFFF);

        // 渲染提示文本
        safeGuiGraphics.drawString(font, nn(Component.translatable("gui.city_naming.prompt")), centerX - 100, centerY - 35, 0xAAAAAA);
        
        // 渲染错误消息
        if (!this.errorMessage.isEmpty()) {
            safeGuiGraphics.drawString(font, nn(Component.translatable(this.errorMessage)), centerX - 100, centerY + 70, 0xFF5555);
        }
        
        // 渲染输入框
        this.cityNameField.render(safeGuiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) {
            this.onConfirm();
            return true;
        } else if (keyCode == 256) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onConfirm() {
        String cityName = this.cityNameField.getValue().trim();
        
        // 重置错误消息
        this.errorMessage = "";
        
        // 表单验证
        if (cityName.isEmpty()) {
            this.errorMessage = "gui.city_naming.error.empty";
        } else if (cityName.length() < 2) {
            this.errorMessage = "gui.city_naming.error.too_short";
        } else if (cityName.length() > 20) {
            this.errorMessage = "gui.city_naming.error.too_long";
        } else if (!cityName.matches("[a-zA-Z0-9\\u4e00-\\u9fa5\\s]+") || cityName.matches(".*\\s{2,}.*")) {
            this.errorMessage = "gui.city_naming.error.invalid_chars";
        }
        
        if (!this.errorMessage.isEmpty()) {
            // 显示错误消息，不关闭界面
            return;
        }
        
        // 验证通过，发送数据包
        CreateCityPacket packet = new CreateCityPacket(cityName, this.cityCorePos);
        
        // 使用NetworkManager发送数据包
        NetworkManager.INSTANCE.sendToServer(packet);
        
        this.closeScreen();
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
