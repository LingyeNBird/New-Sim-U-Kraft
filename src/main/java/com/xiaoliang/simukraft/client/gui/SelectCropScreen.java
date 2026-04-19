package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class SelectCropScreen extends Screen {
    private final BlockPos farmlandBoxPos;
    private Button wheatButton;
    private Button potatoButton;
    private Button carrotButton;
    private Button melonButton;
    private Button pumpkinButton;
    
    public SelectCropScreen(BlockPos pos) {
        super(Component.translatable("gui.select_crop.title"));
        this.farmlandBoxPos = pos;
    }

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    @Override
    protected void init() {
        super.init();

        // 返回按钮
        this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.button.back")),
                        button -> this.onClose())
                .bounds(5, 5, 45, 20)
                .build()));

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 小麦按钮
        wheatButton = this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.crop.wheat")),
                        button -> selectCrop("wheat"))
                .bounds(centerX - 100, centerY - 40, 80, 20)
                .build()));

        // 马铃薯按钮
        potatoButton = this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.crop.potato")),
                        button -> selectCrop("potato"))
                .bounds(centerX - 100, centerY - 10, 80, 20)
                .build()));

        // 胡萝卜按钮
        carrotButton = this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.crop.carrot")),
                        button -> selectCrop("carrot"))
                .bounds(centerX - 100, centerY + 20, 80, 20)
                .build()));

        // 西瓜按钮
        melonButton = this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.crop.melon")),
                        button -> selectCrop("melon"))
                .bounds(centerX - 100, centerY + 50, 80, 20)
                .build()));

        // 南瓜按钮
        pumpkinButton = this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.crop.pumpkin")),
                        button -> selectCrop("pumpkin"))
                .bounds(centerX + 20, centerY - 40, 80, 20)
                .build()));

        // 更新按钮状态
        updateButtonStates();
    }

    private void updateButtonStates() {
        String selectedCrop = FarmlandData.getSelectedCrop(farmlandBoxPos);

        // 重置所有按钮颜色
        wheatButton.setMessage(nn(Component.translatable("gui.crop.wheat").withStyle(style -> style.withColor(0xFFFFFF))));
        potatoButton.setMessage(nn(Component.translatable("gui.crop.potato").withStyle(style -> style.withColor(0xFFFFFF))));
        carrotButton.setMessage(nn(Component.translatable("gui.crop.carrot").withStyle(style -> style.withColor(0xFFFFFF))));
        melonButton.setMessage(nn(Component.translatable("gui.crop.melon").withStyle(style -> style.withColor(0xFFFFFF))));
        pumpkinButton.setMessage(nn(Component.translatable("gui.crop.pumpkin").withStyle(style -> style.withColor(0xFFFFFF))));

        // 高亮已选择的作物
        if ("wheat".equals(selectedCrop)) {
            wheatButton.setMessage(nn(Component.translatable("gui.crop.wheat.selected").withStyle(style -> style.withColor(0x55FF55))));
        } else if ("potato".equals(selectedCrop)) {
            potatoButton.setMessage(nn(Component.translatable("gui.crop.potato.selected").withStyle(style -> style.withColor(0x55FF55))));
        } else if ("carrot".equals(selectedCrop)) {
            carrotButton.setMessage(nn(Component.translatable("gui.crop.carrot.selected").withStyle(style -> style.withColor(0x55FF55))));
        } else if ("melon".equals(selectedCrop)) {
            melonButton.setMessage(nn(Component.translatable("gui.crop.melon.selected").withStyle(style -> style.withColor(0x55FF55))));
        } else if ("pumpkin".equals(selectedCrop)) {
            pumpkinButton.setMessage(nn(Component.translatable("gui.crop.pumpkin.selected").withStyle(style -> style.withColor(0x55FF55))));
        }
    }

    private void selectCrop(String crop) {
        FarmlandData.setSelectedCrop(farmlandBoxPos, crop);
        updateButtonStates();

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    nn(Component.translatable("message.simukraft.crop.selected", getCropDisplayName(crop))
                            .withStyle(style -> style.withColor(0x55FF55))),
                    false
            );
        }

        // 自动返回主界面
        this.onClose();
    }

    private String getCropDisplayName(String crop) {
        return switch (crop) {
            case "wheat" -> safeString(Component.translatable("gui.crop.wheat").getString());
            case "potato" -> safeString(Component.translatable("gui.crop.potato").getString());
            case "carrot" -> safeString(Component.translatable("gui.crop.carrot").getString());
            case "melon" -> safeString(Component.translatable("gui.crop.melon").getString());
            case "pumpkin" -> safeString(Component.translatable("gui.crop.pumpkin").getString());
            default -> safeString(crop);
        };
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 黑色半透明背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);
        
        // 白色标题
        int titleColor = 0xFFFFFF;
        Component title = Component.translatable("gui.select_crop.title").withStyle(style -> style.withColor(titleColor));
        guiGraphics.drawCenteredString(nn(this.font), nn(title), this.width / 2, 30, titleColor);

        // 提示文字
        int textColor = 0xFFF5F5A0;
        Component hint = Component.translatable("gui.select_crop.hint").withStyle(style -> style.withColor(textColor));
        guiGraphics.drawCenteredString(nn(this.font), nn(hint), this.width / 2, 50, textColor);
        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new FarmlandBoxScreen(farmlandBoxPos));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
