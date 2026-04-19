package com.xiaoliang.simukraft.client.gui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.xiaoliang.simukraft.client.preview.AreaSelectionManager;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 规划区域主界面 - 选择操作类型
 */
@OnlyIn(Dist.CLIENT)
public class PlanAreaScreen extends ModularUIGuiContainer {
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;
    private static final int SHORT_BUTTON_WIDTH_THRESHOLD = 60;
    private static final int SHORT_BUTTON_HEIGHT_THRESHOLD = 24;
    private static final int HOVER_BORDER_COLOR = 0xFFADD8E6;

    private static final String LONG_BUTTON_TEXTURE = "simukraft:textures/gui/long_button.png";
    private static final String BUTTON_TEXTURE = "simukraft:textures/gui/button.png";

    private final BlockPos buildBoxPos;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    public PlanAreaScreen(BlockPos buildBoxPos) {
        super(createModularUI(buildBoxPos), 0);
        this.buildBoxPos = buildBoxPos;
        Minecraft.getInstance().getSoundManager()
                .play(nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.BUILD_BOX_OPEN.get()), 1.0F)));
    }

    private static ModularUI createModularUI(BlockPos buildBoxPos) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        Player player = Minecraft.getInstance().player;
        PlanAreaUIHolder holder = new PlanAreaUIHolder();
        ModularUI modularUI = new ModularUI(new Size(screenWidth, screenHeight), holder, player);

        WidgetGroup rootGroup = new WidgetGroup();
        rootGroup.setSize(screenWidth, screenHeight);

        TextTexture titleTexture = new TextTexture("gui.plan_area.title");
        titleTexture.setWidth(screenWidth);
        titleTexture.setDropShadow(true);
        rootGroup.addWidget(new ImageWidget(0, 25, screenWidth, 20, titleTexture));

        TextTexture instructionTexture = new TextTexture("gui.plan_area.instruction");
        instructionTexture.setWidth(screenWidth);
        instructionTexture.setColor(0xF5F5A0);
        instructionTexture.setDropShadow(true);
        rootGroup.addWidget(new ImageWidget(0, 60, screenWidth, 20, instructionTexture));

        TextTexture copyrightTexture = new TextTexture("gui.copyright");
        copyrightTexture.setWidth(200);
        copyrightTexture.setType(TextTexture.TextType.RIGHT);
        copyrightTexture.setColor(0x666666);
        rootGroup.addWidget(new ImageWidget(screenWidth - 205, 5, 200, 16, copyrightTexture));

        rootGroup.addWidget(createButton(5, 5, 45, 20, "gui.button.done",
                clickData -> Minecraft.getInstance().setScreen(new BuildBoxScreen(buildBoxPos))));

        int totalWidth = BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
        int startX = centerX - totalWidth / 2;
        int buttonY = centerY - BUTTON_HEIGHT / 2;

        rootGroup.addWidget(createButton(startX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, "gui.plan_area.replace_blocks",
                clickData -> startSelectionMode(buildBoxPos, AreaSelectionManager.SelectionMode.REPLACE)));
        rootGroup.addWidget(createButton(startX + BUTTON_WIDTH + BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, "gui.plan_area.fill_blocks",
                clickData -> startSelectionMode(buildBoxPos, AreaSelectionManager.SelectionMode.FILL)));
        rootGroup.addWidget(createButton(startX + (BUTTON_WIDTH + BUTTON_SPACING) * 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, "gui.plan_area.remove_blocks",
                clickData -> startSelectionMode(buildBoxPos, AreaSelectionManager.SelectionMode.REMOVE)));

        modularUI.widget(rootGroup);
        modularUI.initWidgets();
        return modularUI;
    }

    /**
     * 开始区域选择模式
     */
    private static void startSelectionMode(BlockPos buildBoxPos, AreaSelectionManager.SelectionMode mode) {
        Minecraft.getInstance().setScreen(new AreaSelectionScreen(buildBoxPos, Minecraft.getInstance().screen, mode));
    }

    private static ButtonWidget createButton(int x, int y, int width, int height, String textKey, java.util.function.Consumer<ClickData> onPress) {
        ButtonWidget button = new ButtonWidget();
        button.setSelfPosition(x, y);
        button.setSize(width, height);

        String textureLocation = getButtonTexture(width, height);
        var buttonBackground = new ResourceTexture(textureLocation);
        var buttonHover = new GuiTextureGroup(
                new ResourceTexture(textureLocation),
                new ColorBorderTexture(1, HOVER_BORDER_COLOR)
        );
        var buttonText = new TextTexture(textKey);

        button.setButtonTexture(buttonBackground, buttonText);
        button.setHoverTexture(buttonHover, buttonText);
        button.setOnPressCallback(onPress);
        return button;
    }

    private static String getButtonTexture(int width, int height) {
        boolean useShortTexture = width <= SHORT_BUTTON_WIDTH_THRESHOLD && height <= SHORT_BUTTON_HEIGHT_THRESHOLD;
        if (useShortTexture) {
            return BUTTON_TEXTURE;
        }
        return LONG_BUTTON_TEXTURE;
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new BuildBoxScreen(buildBoxPos));
    }

    private static class PlanAreaUIHolder implements IUIHolder {
        @Override
        public ModularUI createUI(Player entityPlayer) {
            return null;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public void markAsDirty() {
        }
    }
}
