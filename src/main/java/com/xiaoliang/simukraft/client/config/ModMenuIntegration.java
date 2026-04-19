package com.xiaoliang.simukraft.client.config;

import com.xiaoliang.simukraft.client.gui.ModConfigScreen;
import com.xiaoliang.simukraft.client.gui.ServerConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ModMenuIntegration {
    @Nonnull
    private static Minecraft requireMinecraft() {
        return Objects.requireNonNull(Minecraft.getInstance());
    }

    @Nonnull
    private static ResourceLocation requireResourceLocation(ResourceLocation value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static CubeMap requireCubeMap(CubeMap value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Button requireButton(Button value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Component requireComponent(Component value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String requireString(String value) {
        return Objects.requireNonNull(value);
    }

    /**
     * 注册配置屏幕工厂
     * 在客户端初始化时调用
     */
    @SuppressWarnings("removal")
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ConfigSelectionScreen(parent)
                )
        );
    }

    /**
     * 配置选择界面 - 使用原生 Screen + LDLib Widget
     * 兼容不同屏幕尺寸和GUI缩放
     */
    public static class ConfigSelectionScreen extends Screen {
        private final Screen parent;

        // 全景图相关
        public static final CubeMap PANORAMA_RESOURCES =
                requireCubeMap(new CubeMap(requireResourceLocation(ResourceLocation.fromNamespaceAndPath(com.xiaoliang.simukraft.Simukraft.MOD_ID, "textures/background/panorama"))));
        public static final PanoramaRenderer PANORAMA = new PanoramaRenderer(PANORAMA_RESOURCES);

        // Logo 相关
        private static final ResourceLocation LOGO_TEXTURE = requireResourceLocation(ResourceLocation.fromNamespaceAndPath(com.xiaoliang.simukraft.Simukraft.MOD_ID, "logo.png"));
        private static final int LOGO_WIDTH_BASE = 200;
        private static final int LOGO_HEIGHT_BASE = 60;

        // 使用原生按钮
        private Button clientConfigButton;
        private Button serverConfigButton;
        private Button updateButton;
        private Button bilibiliButton;
        private Button mcmodButton;
        private Button backButton;

        // 动态计算的尺寸和位置
        private int panelX, panelY, panelWidth, panelHeight;
        private int buttonWidth, buttonHeight, buttonSpacing;
        private int logoWidth, logoHeight;

        public ConfigSelectionScreen(Screen parent) {
            super(Component.translatable("gui.mod_config_select.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();

            // 计算动态尺寸
            calculateDimensions();

            int buttonX = panelX + (panelWidth - buttonWidth) / 2;
            // 向上移动起始位置，让所有按钮居中偏上
            int startY = panelY + (int)(panelHeight * 0.15);
            // 减小按钮间距以适应5个按钮
            int adjustedSpacing = Math.max(3, buttonSpacing - 2);

            // 客户端配置按钮
            this.clientConfigButton = requireButton(Button.builder(
                    requireComponent(Component.translatable("gui.mod_config_select.client")),
                    button -> requireMinecraft().setScreen(new ModConfigScreen(this))
            ).bounds(buttonX, startY, buttonWidth, buttonHeight).build());
            this.addRenderableWidget(requireButton(this.clientConfigButton));

            // 服务器配置按钮
            Minecraft minecraft = requireMinecraft();
            boolean hasServerPermission = minecraft.getSingleplayerServer() != null ||
                    (minecraft.player != null && minecraft.player.hasPermissions(2));
            this.serverConfigButton = requireButton(Button.builder(
                    requireComponent(Component.translatable("gui.mod_config_select.server")),
                    button -> requireMinecraft().setScreen(new ServerConfigScreen(this))
            ).bounds(buttonX, startY + buttonHeight + adjustedSpacing, buttonWidth, buttonHeight)
                    .build());
            this.serverConfigButton.active = hasServerPermission;
            this.addRenderableWidget(requireButton(this.serverConfigButton));

            // 检查更新按钮
            this.updateButton = requireButton(Button.builder(
                    requireComponent(Component.translatable("gui.mod_config_select.check_update")),
                    button -> {
                        // 打开更新界面
                        requireMinecraft().setScreen(
                            new com.xiaoliang.simukraft.client.gui.UpdateScreen(
                                this,
                                com.xiaoliang.simukraft.client.update.UpdateHandler.getInstance().getUpdateChecker()
                            )
                        );
                    }
            ).bounds(buttonX, startY + 2 * (buttonHeight + adjustedSpacing), buttonWidth, buttonHeight).build());
            this.addRenderableWidget(requireButton(this.updateButton));

            // Bilibili 和 mcmod 按钮（并排）
            int halfButtonWidth = (buttonWidth - 5) / 2;
            this.bilibiliButton = requireButton(Button.builder(
                    requireComponent(Component.literal("Bilibili")),
                    button -> openUrl("https://space.bilibili.com/3546922073721320")
            ).bounds(buttonX, startY + 3 * (buttonHeight + adjustedSpacing), halfButtonWidth, buttonHeight).build());
            this.addRenderableWidget(requireButton(this.bilibiliButton));

            this.mcmodButton = requireButton(Button.builder(
                    requireComponent(Component.literal("mcmod")),
                    button -> openUrl("https://www.mcmod.cn/class/24995.html")
            ).bounds(buttonX + halfButtonWidth + 5, startY + 3 * (buttonHeight + adjustedSpacing), halfButtonWidth, buttonHeight).build());
            this.addRenderableWidget(requireButton(this.mcmodButton));

            // 返回按钮 - 确保在面板内
            this.backButton = requireButton(Button.builder(
                    requireComponent(Component.translatable("gui.button.back")),
                    button -> this.onClose()
            ).bounds(buttonX, startY + 4 * (buttonHeight + adjustedSpacing), buttonWidth, buttonHeight).build());
            this.addRenderableWidget(requireButton(this.backButton));
        }

        /**
         * 计算动态尺寸 - 适配不同屏幕大小和GUI缩放
         * 在高GUI缩放(自动/3/4)下缩小尺寸，确保所有缩放都能正常显示
         */
        private void calculateDimensions() {
            // 获取GUI缩放因子
            double guiScale = requireMinecraft().getWindow().getGuiScale();

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

            // 基础面板尺寸 - 使用屏幕比例的相对大小
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

            // 按钮尺寸 - 基于面板宽度，随缩放减小
            int baseButtonWidth = Math.max(160, Math.min(240, (int)(panelWidth * 0.85)));
            int baseButtonHeight = Math.max(22, Math.min(32, (int)(panelHeight * 0.15)));

            // 高缩放时进一步缩小按钮
            if (guiScale >= 4.0) {
                buttonWidth = (int)(baseButtonWidth * 0.85);
                buttonHeight = (int)(baseButtonHeight * 0.85);
            } else if (guiScale >= 3.0) {
                buttonWidth = (int)(baseButtonWidth * 0.90);
                buttonHeight = (int)(baseButtonHeight * 0.90);
            } else {
                buttonWidth = baseButtonWidth;
                buttonHeight = baseButtonHeight;
            }

            // 按钮间距
            buttonSpacing = Math.max(6, (int)(panelHeight * 0.05));

            // Logo 尺寸 - 保持宽高比，基于面板宽度，更大一些
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
            guiGraphics.blit(LOGO_TEXTURE, logoX, logoY, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);

            // 渲染说明文字
            Component desc = requireComponent(Component.translatable("gui.mod_config_select.description"));
            int descY = panelY + (int)(panelHeight * 0.22);
            var font = Objects.requireNonNull(this.font);
            int descX = (this.width - font.width(desc)) / 2;
            guiGraphics.drawString(font, desc, descX, descY, 0xAAAAAA, false);

            // 渲染按钮
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onClose() {
            requireMinecraft().setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }

        /**
         * 使用系统浏览器打开 URL
         */
        private void openUrl(String url) {
            try {
                // 使用 Minecraft 的跨平台方法打开链接
                net.minecraft.Util.getPlatform().openUri(requireString(url));
            } catch (Exception e) {
                com.xiaoliang.simukraft.Simukraft.LOGGER.error("Failed to open URL: " + url, e);
                // 如果玩家存在（在游戏中），显示错误提示
                Minecraft minecraft = requireMinecraft();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                        requireComponent(net.minecraft.network.chat.Component.literal("§c无法打开链接: " + url)),
                        false
                    );
                }
            }
        }
    }
}
