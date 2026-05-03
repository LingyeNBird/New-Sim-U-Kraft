package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.client.preview.FarmlandAreaPreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class SelectAreaScreen extends Screen {
    private final BlockPos farmlandBoxPos;
    private final Button[] areaButtons;

    // 可选的区域大小 - 只保留10*10及以下的按钮
    private final int[] AREA_SIZES = {4, 6, 8, 10};

    // 当前悬停的区域大小
    private int hoveredAreaSize = -1;

    // 上一次玩家朝向
    private Direction lastPlayerFacing = Direction.NORTH;

    public SelectAreaScreen(BlockPos pos) {
        super(Component.translatable("gui.select_area.title"));
        this.farmlandBoxPos = pos;
        this.areaButtons = new Button[AREA_SIZES.length];

        // 获取玩家当前朝向
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            lastPlayerFacing = player.getDirection();
        }

        // 启动预览，显示当前已选择的区域
        int currentSize = FarmlandData.getSelectedAreaSize(farmlandBoxPos);
        if (currentSize > 0) {
            FarmlandAreaPreviewManager.startPreview(farmlandBoxPos, currentSize, lastPlayerFacing);
        }
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
        int startY = 80;
        int buttonSpacing = 25;
        
        // 创建区域大小选择按钮
        for (int i = 0; i < AREA_SIZES.length; i++) {
            final int areaSize = AREA_SIZES[i];
            areaButtons[i] = this.addRenderableWidget(nn(Button.builder(
                            nn(Component.literal(areaSize + "x" + areaSize)),
                            button -> selectArea(areaSize))
                    .bounds(centerX - 40, startY + i * buttonSpacing, 80, 20)
                    .build()));
        }

        // 更新按钮状态
        updateButtonStates();
    }

    private void updateButtonStates() {
        int selectedAreaSize = FarmlandData.getSelectedAreaSize(farmlandBoxPos);
        
        // 重置所有按钮颜色
        for (Button button : areaButtons) {
            if (button != null) {
                String buttonText = button.getMessage().getString();
                if (buttonText.endsWith(" ✓")) {
                    buttonText = buttonText.substring(0, buttonText.length() - 2);
                }
                button.setMessage(nn(Component.literal(safeString(buttonText)).withStyle(style -> style.withColor(0xFFFFFF))));
            }
        }
        
        // 高亮已选择的区域大小
        for (int i = 0; i < AREA_SIZES.length; i++) {
            if (AREA_SIZES[i] == selectedAreaSize && areaButtons[i] != null) {
                areaButtons[i].setMessage(nn(Component.literal(AREA_SIZES[i] + "x" + AREA_SIZES[i] + " ✓").withStyle(style -> style.withColor(0x55FF55))));
            }
        }
    }

    private void selectArea(int areaSize) {
        FarmlandData.setSelectedArea(farmlandBoxPos, areaSize);
        updateButtonStates();

        // 更新预览
        FarmlandAreaPreviewManager.startPreview(farmlandBoxPos, areaSize);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    nn(Component.translatable("message.simukraft.area.selected", areaSize, areaSize)
                            .withStyle(style -> style.withColor(0x55FF55))),
                    false
            );
        }

        // 自动返回主界面
        this.onClose();
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 黑色半透明背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);

        // 白色标题
        int titleColor = 0xFFFFFF;
        Component title = Component.translatable("gui.select_area.title").withStyle(style -> style.withColor(titleColor));
        guiGraphics.drawCenteredString(nn(this.font), nn(title), this.width / 2, 30, titleColor);

        // 提示文字
        int textColor = 0xFFF5F5A0;
        Component hint = Component.translatable("gui.select_area.hint").withStyle(style -> style.withColor(textColor));
        guiGraphics.drawCenteredString(nn(this.font), nn(hint), this.width / 2, 50, textColor);

        // 预览提示
        Component previewHint = Component.translatable("gui.select_area.preview_hint").withStyle(style -> style.withColor(0xAAAAAA));
        guiGraphics.drawCenteredString(nn(this.font), nn(previewHint), this.width / 2, 65, 0xAAAAAA);

        // 朝向提示
        var player = Minecraft.getInstance().player;
        Direction currentFacing = player != null ? player.getDirection() : Direction.NORTH;
        Component facingHint = Component.translatable("gui.select_area.current_facing", getDirectionName(currentFacing))
            .withStyle(style -> style.withColor(0xAAAAAA));
        guiGraphics.drawCenteredString(nn(this.font), nn(facingHint), this.width / 2, 78, 0xAAAAAA);

        // 检查鼠标悬停并更新预览
        checkHoverAndUpdatePreview(mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    /**
     * 获取方向的本地化名称
     */
    private String getDirectionName(Direction direction) {
        return switch (direction) {
            case NORTH -> safeString(Component.translatable("direction.north").getString());
            case SOUTH -> safeString(Component.translatable("direction.south").getString());
            case EAST -> safeString(Component.translatable("direction.east").getString());
            case WEST -> safeString(Component.translatable("direction.west").getString());
            default -> safeString(Component.translatable("direction.unknown").getString());
        };
    }

    /**
     * 检查鼠标悬停并更新预览
     */
    private void checkHoverAndUpdatePreview(int mouseX, int mouseY) {
        int newHoveredSize = -1;

        for (int i = 0; i < AREA_SIZES.length; i++) {
            if (areaButtons[i] != null && areaButtons[i].isMouseOver(mouseX, mouseY)) {
                newHoveredSize = AREA_SIZES[i];
                break;
            }
        }

        // 获取当前玩家朝向
        var player = Minecraft.getInstance().player;
        Direction currentFacing = player != null ? player.getDirection() : Direction.NORTH;

        // 如果悬停的区域大小或朝向发生变化，更新预览
        if (newHoveredSize != hoveredAreaSize || currentFacing != lastPlayerFacing) {
            hoveredAreaSize = newHoveredSize;
            lastPlayerFacing = currentFacing;

            if (hoveredAreaSize > 0) {
                FarmlandAreaPreviewManager.startPreview(farmlandBoxPos, hoveredAreaSize, lastPlayerFacing);
            } else {
                // 如果没有悬停任何按钮，显示当前已选择的区域
                int currentSize = FarmlandData.getSelectedAreaSize(farmlandBoxPos);
                if (currentSize > 0) {
                    FarmlandAreaPreviewManager.startPreview(farmlandBoxPos, currentSize, lastPlayerFacing);
                } else {
                    FarmlandAreaPreviewManager.stopPreview();
                }
            }
        }
    }

    @Override
    public void onClose() {
        // 关闭界面时停止预览
        FarmlandAreaPreviewManager.stopPreview();
        Minecraft.getInstance().setScreen(new FarmlandBoxScreen(farmlandBoxPos));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
