package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ModConfigScreen extends Screen {
    private final Screen parent;

    // 全景图相关
    @Nonnull
    public static final CubeMap PANORAMA_RESOURCES = createPanoramaResources();
    @Nonnull
    public static final PanoramaRenderer PANORAMA = createPanoramaRenderer();

    // Logo 相关
    private static final ResourceLocation LOGO_TEXTURE = nn(ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "logo.png"));
    private static final int LOGO_WIDTH_BASE = 200;
    private static final int LOGO_HEIGHT_BASE = 60;

    // UI 组件
    private CycleButton<ClientConfig.Anchor> anchorButton;
    private EditBox xInput;
    private EditBox yInput;
    private Button saveButton;
    private Button resetButton;
    private Button cancelButton;

    // 动态计算的尺寸和位置
    private int panelX, panelY, panelWidth, panelHeight;
    private int buttonHeight;
    private int inputWidth, inputHeight;
    private int shortButtonWidth;
    private int rowHeight;
    private int logoWidth, logoHeight;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("gui.mod_config.title"));
        this.parent = parent;
    }

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    @Nonnull
    private static CubeMap createPanoramaResources() {
        return nn(new CubeMap(nn(ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "textures/background/panorama"))));
    }

    @Nonnull
    private static PanoramaRenderer createPanoramaRenderer() {
        return nn(new PanoramaRenderer(PANORAMA_RESOURCES));
    }

    @Override
    protected void init() {
        super.init();

        // 计算动态尺寸 - 基于屏幕大小和GUI缩放
        calculateDimensions();

        int inputX = panelX + (int)(panelWidth * 0.45);
        int startY = panelY + (int)(panelHeight * 0.22);

        // 锚点位置选择按钮
        this.anchorButton = CycleButton.<ClientConfig.Anchor>builder(anchor -> {
                    String name = switch (anchor) {
                        case TOP_LEFT -> Component.translatable("gui.anchor.top_left").getString();
                        case TOP_RIGHT -> Component.translatable("gui.anchor.top_right").getString();
                        case BOTTOM_LEFT -> Component.translatable("gui.anchor.bottom_left").getString();
                        case BOTTOM_RIGHT -> Component.translatable("gui.anchor.bottom_right").getString();
                        case TOP_CENTER -> Component.translatable("gui.anchor.top_center").getString();
                        case BOTTOM_CENTER -> Component.translatable("gui.anchor.bottom_center").getString();
                    };
                    return Component.literal(safeString(name));
                })
                .withValues(ClientConfig.Anchor.values())
                .withInitialValue(nn(ClientConfig.getAnchor()))
                .create(inputX, startY, inputWidth, inputHeight, nn(Component.translatable("gui.mod_config.anchor")));
        this.addRenderableWidget(nn(this.anchorButton));

        // X 偏移输入框
        this.xInput = new EditBox(nn(this.font), inputX, startY + rowHeight, inputWidth, inputHeight,
                nn(Component.translatable("gui.mod_config.pos_x")));
        this.xInput.setValue(safeString(String.valueOf(ClientConfig.getPosX())));
        this.xInput.setMaxLength(10);
        this.addRenderableWidget(nn(this.xInput));

        // Y 偏移输入框
        this.yInput = new EditBox(nn(this.font), inputX, startY + rowHeight * 2, inputWidth, inputHeight,
                nn(Component.translatable("gui.mod_config.pos_y")));
        this.yInput.setValue(safeString(String.valueOf(ClientConfig.getPosY())));
        this.yInput.setMaxLength(10);
        this.addRenderableWidget(nn(this.yInput));

        // 按钮区域 - 水平排列在输入框下方，居中
        int buttonStartY = startY + rowHeight * 3 + (int)(panelHeight * 0.05);
        int buttonSpacing = 4;

        // 计算三个按钮整体居中位置
        int buttonsTotalWidth = shortButtonWidth * 3 + buttonSpacing * 2;
        int buttonsStartX = panelX + (panelWidth - buttonsTotalWidth) / 2;

        // 保存按钮
        this.saveButton = Button.builder(
                nn(Component.translatable("gui.mod_config.save")),
                button -> {
                    saveConfig();
                    this.onClose();
                }
        ).bounds(buttonsStartX, buttonStartY, shortButtonWidth, buttonHeight).build();
        this.addRenderableWidget(nn(this.saveButton));

        // 重置按钮
        this.resetButton = Button.builder(
                nn(Component.translatable("gui.mod_config.reset")),
                button -> {
                    anchorButton.setValue(ClientConfig.Anchor.TOP_RIGHT);
                    xInput.setValue("-25");
                    yInput.setValue("5");
                }
        ).bounds(buttonsStartX + shortButtonWidth + buttonSpacing, buttonStartY, shortButtonWidth, buttonHeight).build();
        this.addRenderableWidget(nn(this.resetButton));

        // 取消按钮
        this.cancelButton = Button.builder(
                nn(Component.translatable("gui.mod_config.cancel")),
                button -> this.onClose()
        ).bounds(buttonsStartX + (shortButtonWidth + buttonSpacing) * 2, buttonStartY, shortButtonWidth, buttonHeight).build();
        this.addRenderableWidget(nn(this.cancelButton));
    }

    /**
     * 计算动态尺寸 - 适配不同屏幕大小和GUI缩放
     * 与 ConfigSelectionScreen 保持一致
     */
    private void calculateDimensions() {
        // 获取GUI缩放因子
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        // 根据GUI缩放调整基础尺寸比例
        // 缩放越大，界面越小，保持视觉一致性
        double scaleFactor;
        if (guiScale >= 4.0) {
            scaleFactor = 0.40; // 缩放4x时，使用40%的屏幕空间
        } else if (guiScale >= 3.0) {
            scaleFactor = 0.48; // 缩放3x时，使用48%的屏幕空间
        } else if (guiScale >= 2.0) {
            scaleFactor = 0.65; // 缩放2x时，使用65%的屏幕空间（增大）
        } else {
            scaleFactor = 0.75; // 缩放1x时，使用75%的屏幕空间（增大）
        }

        // 基础面板尺寸 - 使用屏幕比例的相对大小，与 ConfigSelectionScreen 保持一致
        // 最小宽度 240，最大 360，基于屏幕宽度
        panelWidth = Math.max(240, Math.min(360, (int)(this.width * scaleFactor)));
        // 最小高度 180，最大 260，基于屏幕高度
        panelHeight = Math.max(180, Math.min(260, (int)(this.height * scaleFactor * 0.8)));

        // 确保面板不会超出屏幕
        panelWidth = Math.min(panelWidth, this.width - 40);
        panelHeight = Math.min(panelHeight, this.height - 60);

        // 面板居中
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        // 按钮尺寸 - 基于面板宽度，与 ConfigSelectionScreen 保持一致
        int baseButtonHeight = Math.max(22, Math.min(32, (int)(panelHeight * 0.15)));

        // 高缩放时进一步缩小按钮
        if (guiScale >= 4.0) {
            buttonHeight = (int)(baseButtonHeight * 0.85);
        } else if (guiScale >= 3.0) {
            buttonHeight = (int)(baseButtonHeight * 0.90);
        } else {
            buttonHeight = baseButtonHeight;
        }

        // 输入框尺寸 - 适当减小宽度以适应面板
        int baseInputWidth = Math.max(100, Math.min(160, (int)(panelWidth * 0.50)));
        int baseInputHeight = Math.max(22, Math.min(32, (int)(panelHeight * 0.15)));

        if (guiScale >= 4.0) {
            inputWidth = (int)(baseInputWidth * 0.85);
            inputHeight = (int)(baseInputHeight * 0.85);
        } else if (guiScale >= 3.0) {
            inputWidth = (int)(baseInputWidth * 0.90);
            inputHeight = (int)(baseInputHeight * 0.90);
        } else {
            inputWidth = baseInputWidth;
            inputHeight = baseInputHeight;
        }

        // 行高 - 基于输入框高度
        rowHeight = inputHeight + Math.max(6, (int)(panelHeight * 0.05));

        // 下方三个按钮的宽度 - 基于面板宽度，随GUI缩放调整
        int baseShortButtonWidth = Math.max(70, Math.min(100, (int)(panelWidth * 0.28)));
        if (guiScale >= 4.0) {
            shortButtonWidth = (int)(baseShortButtonWidth * 0.85);
        } else if (guiScale >= 3.0) {
            shortButtonWidth = (int)(baseShortButtonWidth * 0.90);
        } else {
            shortButtonWidth = baseShortButtonWidth;
        }

        // Logo 尺寸 - 保持宽高比，基于面板宽度，与 ConfigSelectionScreen 保持一致
        double logoScale = Math.min(1.3, panelWidth / 250.0);
        if (guiScale >= 4.0) {
            logoScale *= 0.90;
        } else if (guiScale >= 3.0) {
            logoScale *= 0.95;
        }
        logoWidth = (int)(LOGO_WIDTH_BASE * logoScale);
        logoHeight = (int)(LOGO_HEIGHT_BASE * logoScale);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        // 窗口大小改变时重新初始化
        this.init();
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染全景图背景
        PANORAMA.render(partialTicks, 1.0F);

        // 渲染面板背景 - 边框内半透明背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD000000);
        guiGraphics.renderOutline(panelX - 1, panelY - 1, panelWidth + 2, panelHeight + 2, 0xFF555555);

        // 渲染 Logo - 居中显示在面板上方
        int logoX = (this.width - logoWidth) / 2;
        int logoY = panelY - logoHeight - (int)(Math.min(this.height, this.width) * 0.015);
        // 确保logo不会超出屏幕顶部
        if (logoY < 5) {
            logoY = 5;
        }
        guiGraphics.blit(nn(LOGO_TEXTURE), logoX, logoY, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);

        int labelX = panelX + (int)(panelWidth * 0.08);
        int startY = panelY + (int)(panelHeight * 0.22);

        // 渲染标签 - 与输入控件垂直居中
        Component anchorLabel = Component.translatable("gui.mod_config.anchor");
        guiGraphics.drawString(nn(this.font), nn(anchorLabel), labelX, startY + (inputHeight - nn(this.font).lineHeight) / 2 + 1, 0xFFFFFF, false);

        Component xLabel = Component.translatable("gui.mod_config.pos_x");
        guiGraphics.drawString(nn(this.font), nn(xLabel), labelX, startY + rowHeight + (inputHeight - nn(this.font).lineHeight) / 2 + 1, 0xFFFFFF, false);

        Component yLabel = Component.translatable("gui.mod_config.pos_y");
        guiGraphics.drawString(nn(this.font), nn(yLabel), labelX, startY + rowHeight * 2 + (inputHeight - nn(this.font).lineHeight) / 2 + 1, 0xFFFFFF, false);

        // 渲染说明文字 - 移到左下角标签区域，更靠下一些
        Component desc = Component.translatable("gui.mod_config.description");
        int descY = panelY + panelHeight - (int)(panelHeight * 0.05);
        guiGraphics.drawString(nn(this.font), nn(desc), labelX, descY, 0xAAAAAA, false);

        // 渲染输入框和按钮
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void saveConfig() {
        try {
            // 保存锚点
            ClientConfig.HUD_ANCHOR.set(safeString(nn(anchorButton.getValue()).name()));

            // 保存X偏移
            String xValue = xInput.getValue();
            if (!xValue.isEmpty()) {
                ClientConfig.HUD_POS_X.set(Integer.parseInt(xValue));
            }

            // 保存Y偏移
            String yValue = yInput.getValue();
            if (!yValue.isEmpty()) {
                ClientConfig.HUD_POS_Y.set(Integer.parseInt(yValue));
            }

            // 保存配置到文件
            ClientConfig.SPEC.save();

            // 清除缓存，确保下次读取时获取最新值
            ClientConfig.clearCache();
        } catch (Exception e) {
            // 保存失败，显示错误（简化处理）
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
