package com.xiaoliang.simukraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.network.SyncConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 服务器配置界面
 * 允许在游戏中修改服务器配置
 */
public class ServerConfigScreen extends Screen {
    private final Screen parent;
    private int currentPage = 0;
    private static final int MAX_PAGES = 5;
    private final List<ConfigEntry> configEntries = new ArrayList<>();

    // 滚动相关
    private double scrollOffset = 0;
    private boolean isScrolling = false;
    private int contentHeight = 0;
    private int viewportHeight = 0;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 2;

    private Button prevButton;
    private Button nextButton;
    private Button saveButton;
    private Button reloadButton;
    private Button resetButton;
    private Button cancelButton;

    // 颜色常量
    private static final int COLOR_TITLE = 0xFF55FF55;
    private static final int COLOR_SUBTITLE = 0xFFAAAAAA;
    private static final int COLOR_LABEL = 0xFFFFD700;
    private static final int COLOR_PANEL_BG = 0x99000000;
    private static final int COLOR_SCROLLBAR_BG = 0xFF333333;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF888888;
    private static final int COLOR_SCROLLBAR_THUMB_HOVER = 0xFFAAAAAA;

    // 布局常量
    private static final int PANEL_MARGIN_X = 40;
    private static final int PANEL_MARGIN_TOP = 45;
    private static final int PANEL_MARGIN_BOTTOM = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN_BOTTOM = 15;
    private static final int CONTENT_PADDING = 10;

    // 全景图相关
    @Nonnull
    public static final CubeMap PANORAMA_RESOURCES = createPanoramaResources();
    @Nonnull
    public static final ResourceLocation PANORAMA_OVERLAY_TEXTURES =
            nn(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/title/background/panorama_overlay.png"));
    @Nonnull
    public static final PanoramaRenderer PANORAMA = createPanoramaRenderer();

    private long firstRenderTime;

    // 页面标题
    private Component[] getPageTitles() {
        return new Component[] {
            Component.translatable("gui.server_config.page.general"),
            Component.translatable("gui.server_config.page.npc_leveling"),
            Component.translatable("gui.server_config.page.planner"),
            Component.translatable("gui.server_config.page.builder"),
            Component.translatable("gui.server_config.page.materials")
        };
    }

    public ServerConfigScreen(Screen parent) {
        super(Component.translatable("gui.server_config.title"));
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
        this.configEntries.clear();
        this.scrollOffset = 0;

        int centerX = this.width / 2;
        int panelTop = PANEL_MARGIN_TOP;
        int panelBottom = this.height - PANEL_MARGIN_BOTTOM;
        int contentStartY = panelTop + 25; // 预留标题空间
        int contentEndY = panelBottom - BUTTON_HEIGHT - 10;
        this.viewportHeight = contentEndY - contentStartY;

        // 根据当前页面初始化内容
        switch (currentPage) {
            case 0 -> initGeneralPage(centerX);
            case 1 -> initNpcLevelingPage(centerX);
            case 2 -> initPlannerPage(centerX);
            case 3 -> initBuilderPage(centerX);
            case 4 -> initMaterialsPage(centerX);
        }

        // 计算内容高度
        this.contentHeight = configEntries.size() * 28 + 40;

        // 底部按钮区域
        int buttonY = this.height - BUTTON_MARGIN_BOTTOM - BUTTON_HEIGHT;
        int buttonSpacing = 5;
        int smallButtonWidth = Math.min(60, (this.width - 100) / 8);
        int navButtonWidth = Math.min(70, smallButtonWidth + 10);

        int totalButtonsWidth = navButtonWidth * 2 + smallButtonWidth * 4 + buttonSpacing * 5;
        int startX = centerX - totalButtonsWidth / 2;
        int currentX = startX;

        this.prevButton = Button.builder(nn(Component.translatable("gui.server_config.button.prev")), button -> {
            if (currentPage > 0) {
                currentPage--;
                this.scrollOffset = 0;
                this.rebuildWidgets();
            }
        }).pos(currentX, buttonY).size(navButtonWidth, BUTTON_HEIGHT).build();
        this.addRenderableWidget(nn(this.prevButton));
        this.prevButton.active = currentPage > 0;
        this.prevButton.setTooltip(Tooltip.create(nn(Component.translatable("gui.server_config.tooltip.prev"))));
        currentX += navButtonWidth + buttonSpacing;

        this.saveButton = Button.builder(nn(Component.translatable("gui.server_config.button.save")), button -> saveConfig())
                .pos(currentX, buttonY)
                .size(smallButtonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(nn(Component.translatable("gui.server_config.tooltip.save"))))
                .build();
        this.addRenderableWidget(nn(this.saveButton));
        currentX += smallButtonWidth + buttonSpacing;

        this.reloadButton = Button.builder(nn(Component.translatable("gui.server_config.button.reload")), button -> reloadConfig())
                .pos(currentX, buttonY)
                .size(smallButtonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(nn(Component.translatable("gui.server_config.tooltip.reload"))))
                .build();
        this.addRenderableWidget(nn(this.reloadButton));
        currentX += smallButtonWidth + buttonSpacing;

        this.resetButton = Button.builder(nn(Component.translatable("gui.server_config.button.reset")), button -> resetConfig())
                .pos(currentX, buttonY)
                .size(smallButtonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(nn(Component.translatable("gui.server_config.tooltip.reset"))))
                .build();
        this.addRenderableWidget(nn(this.resetButton));
        currentX += smallButtonWidth + buttonSpacing;

        this.cancelButton = Button.builder(nn(Component.translatable("gui.server_config.button.cancel")), button -> onClose())
                .pos(currentX, buttonY)
                .size(smallButtonWidth, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(nn(this.cancelButton));
        currentX += smallButtonWidth + buttonSpacing;

        this.nextButton = Button.builder(nn(Component.translatable("gui.server_config.button.next")), button -> {
            if (currentPage < MAX_PAGES - 1) {
                currentPage++;
                this.scrollOffset = 0;
                this.rebuildWidgets();
            }
        }).pos(currentX, buttonY).size(navButtonWidth, BUTTON_HEIGHT).build();
        this.addRenderableWidget(nn(this.nextButton));
        this.nextButton.active = currentPage < MAX_PAGES - 1;
        this.nextButton.setTooltip(Tooltip.create(nn(Component.translatable("gui.server_config.tooltip.next"))));
    }

    private void initGeneralPage(int centerX) {
        int y = CONTENT_PADDING;

        addPageTitleLabel(centerX, y, getPageTitles()[0].getString());
        y += 30;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.enable_blacklist_protection").getString(), "enableBlacklistProtection",
                Component.translatable("gui.server_config.enable_blacklist_protection.tooltip").getString(),
                ServerConfig.ENABLE_BLACKLIST_PROTECTION.get());
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.log_skipped_blocks").getString(), "logSkippedBlocks",
                Component.translatable("gui.server_config.log_skipped_blocks.tooltip").getString(),
                ServerConfig.LOG_BLACKLIST_SKIPPED_BLOCKS.get());
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.enable_debug_log").getString(), "enableDebugLog",
                Component.translatable("gui.server_config.enable_debug_log.tooltip").getString(),
                ServerConfig.ENABLE_DEBUG_LOG.get());
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.farmer_enable_crop_growth_boost").getString(), "farmerEnableCropGrowthBoost",
                Component.translatable("gui.server_config.farmer_enable_crop_growth_boost.tooltip").getString(),
                ServerConfig.isFarmerCropGrowthBoostEnabled());
    }

    private void initNpcLevelingPage(int centerX) {
        int y = CONTENT_PADDING;

        addPageTitleLabel(centerX, y, getPageTitles()[1].getString());
        y += 30;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.npc_max_level").getString(), "npcMaxLevel",
                Component.translatable("gui.server_config.npc_max_level.tooltip").getString(),
                ServerConfig.NPC_MAX_LEVEL.get(), 1, 20);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.npc_speed_bonus_per_level").getString(), "npcSpeedBonusPerLevel",
                Component.translatable("gui.server_config.npc_speed_bonus_per_level.tooltip").getString(),
                ServerConfig.NPC_SPEED_BONUS_PER_LEVEL.get(), 0, 50);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.npc_min_speed_ticks").getString(), "npcMinSpeedTicks",
                Component.translatable("gui.server_config.npc_min_speed_ticks.tooltip").getString(),
                ServerConfig.NPC_MIN_SPEED_TICKS.get(), 1, 100);
    }

    private void initPlannerPage(int centerX) {
        int y = CONTENT_PADDING;

        addPageTitleLabel(centerX, y, getPageTitles()[2].getString());
        y += 30;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.work_speed").getString());
        y += 22;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_remove_speed").getString(), "plannerRemoveSpeedBase",
                Component.translatable("gui.server_config.planner_remove_speed.tooltip").getString(),
                ServerConfig.PLANNER_REMOVE_SPEED_BASE.get(), 5, 200);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_replace_speed").getString(), "plannerReplaceSpeedBase",
                Component.translatable("gui.server_config.planner_replace_speed.tooltip").getString(),
                ServerConfig.PLANNER_REPLACE_SPEED_BASE.get(), 5, 200);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_fill_speed").getString(), "plannerFillSpeedBase",
                Component.translatable("gui.server_config.planner_fill_speed.tooltip").getString(),
                ServerConfig.PLANNER_FILL_SPEED_BASE.get(), 5, 200);
        y += 28;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.item_handling").getString());
        y += 22;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.planner_drop_items").getString(), "plannerDropItemsOnRemove",
                Component.translatable("gui.server_config.planner_drop_items.tooltip").getString(),
                ServerConfig.PLANNER_DROP_ITEMS_ON_REMOVE.get());
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.planner_store_in_chest").getString(), "plannerStoreItemsInChest",
                Component.translatable("gui.server_config.planner_store_in_chest.tooltip").getString(),
                ServerConfig.PLANNER_STORE_ITEMS_IN_CHEST.get());
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_chest_range").getString(), "plannerChestSearchRange",
                Component.translatable("gui.server_config.planner_chest_range.tooltip").getString(),
                ServerConfig.PLANNER_CHEST_SEARCH_RANGE.get(), 1, 20);
        y += 28;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.other_settings").getString());
        y += 22;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_warning_cooldown").getString(), "plannerWarningCooldown",
                Component.translatable("gui.server_config.planner_warning_cooldown.tooltip").getString(),
                ServerConfig.PLANNER_WARNING_COOLDOWN.get(), 1, 300);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_rest_interval").getString(), "plannerRestCheckInterval",
                Component.translatable("gui.server_config.planner_rest_interval.tooltip").getString(),
                ServerConfig.PLANNER_REST_CHECK_INTERVAL.get(), 1, 200);
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.planner_enable_xp").getString(), "plannerEnableXpGain",
                Component.translatable("gui.server_config.planner_enable_xp.tooltip").getString(),
                ServerConfig.PLANNER_ENABLE_XP_GAIN.get());
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.planner_xp_per_block").getString(), "plannerXpPerBlock",
                Component.translatable("gui.server_config.planner_xp_per_block.tooltip").getString(),
                ServerConfig.PLANNER_XP_PER_BLOCK.get(), 0, 100);
    }

    private void initBuilderPage(int centerX) {
        int y = CONTENT_PADDING;

        addPageTitleLabel(centerX, y, getPageTitles()[3].getString());
        y += 30;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.work_speed").getString());
        y += 22;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_place_speed").getString(), "builderPlaceSpeedBase",
                Component.translatable("gui.server_config.builder_place_speed.tooltip").getString(),
                ServerConfig.BUILDER_PLACE_SPEED_BASE.get(), 1, 200);
        y += 28;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.item_handling").getString());
        y += 22;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.builder_require_materials").getString(), "builderRequireMaterials",
                Component.translatable("gui.server_config.builder_require_materials.tooltip").getString(),
                ServerConfig.BUILDER_REQUIRE_MATERIALS.get());
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_chest_range").getString(), "builderChestSearchRange",
                Component.translatable("gui.server_config.builder_chest_range.tooltip").getString(),
                ServerConfig.BUILDER_CHEST_SEARCH_RANGE.get(), 1, 20);
        y += 28;

        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.other_settings").getString());
        y += 22;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_warning_cooldown").getString(), "builderWarningCooldown",
                Component.translatable("gui.server_config.builder_warning_cooldown.tooltip").getString(),
                ServerConfig.BUILDER_WARNING_COOLDOWN.get(), 1, 300);
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.builder_enable_xp").getString(), "builderEnableXpGain",
                Component.translatable("gui.server_config.builder_enable_xp.tooltip").getString(),
                ServerConfig.BUILDER_ENABLE_XP_GAIN.get());
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_xp_per_block").getString(), "builderXpPerBlock",
                Component.translatable("gui.server_config.builder_xp_per_block.tooltip").getString(),
                ServerConfig.BUILDER_XP_PER_BLOCK.get(), 0, 100);
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.builder_force_load_chunks").getString(), "builderForceLoadChunks",
                Component.translatable("gui.server_config.builder_force_load_chunks.tooltip").getString(),
                ServerConfig.BUILDER_FORCE_LOAD_CHUNKS.get());
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_chunk_load_radius").getString(), "builderChunkLoadRadius",
                Component.translatable("gui.server_config.builder_chunk_load_radius.tooltip").getString(),
                ServerConfig.BUILDER_CHUNK_LOAD_RADIUS.get(), 1, 5);
        y += 28;

        addIntConfig(centerX, y, Component.translatable("gui.server_config.builder_chunk_wait_ticks").getString(), "builderChunkLoadWaitTicks",
                Component.translatable("gui.server_config.builder_chunk_wait_ticks.tooltip").getString(),
                ServerConfig.BUILDER_CHUNK_LOAD_WAIT_TICKS.get(), 10, 200);
    }

    private void initMaterialsPage(int centerX) {
        Minecraft minecraft = this.minecraft;
        int y = CONTENT_PADDING;

        addPageTitleLabel(centerX, y, getPageTitles()[4].getString());
        y += 30;

        // 模式选择
        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.mode_config").getString());
        y += 22;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.enable_expert_mode").getString(), "enableExpertMode",
                Component.translatable("gui.server_config.enable_expert_mode.tooltip").getString(),
                ServerConfig.ENABLE_EXPERT_MODE.get());
        y += 28;

        addBooleanConfig(centerX, y, Component.translatable("gui.server_config.enable_category_matching").getString(), "enableMaterialCategoryMatching",
                Component.translatable("gui.server_config.enable_category_matching.tooltip").getString(),
                ServerConfig.ENABLE_MATERIAL_CATEGORY_MATCHING.get());
        y += 28;

        // 配置按钮
        addSectionLabel(centerX, y, Component.translatable("gui.server_config.section.list_config").getString());
        y += 22;

        // 规划师方块黑名单按钮
        addOpenScreenButton(centerX, y,
                Component.translatable("gui.server_config.button.edit_planner_blacklist").getString(),
                Component.translatable("gui.server_config.button.edit_planner_blacklist.tooltip").getString(),
                () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(BlockBlacklistScreen.createPlanningBlacklistScreen(this, this::refreshScreen));
                    }
                });
        y += 28;

        // 建筑师方块黑名单按钮
        addOpenScreenButton(centerX, y,
                Component.translatable("gui.server_config.button.edit_builder_blacklist").getString(),
                Component.translatable("gui.server_config.button.edit_builder_blacklist.tooltip").getString(),
                () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(BlockBlacklistScreen.createConstructionBlacklistScreen(this, this::refreshScreen));
                    }
                });
        y += 28;

        // 基础材料按钮
        addOpenScreenButton(centerX, y,
                Component.translatable("gui.server_config.button.edit_basic_materials").getString(),
                Component.translatable("gui.server_config.button.edit_basic_materials.tooltip").getString(),
                () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(BasicMaterialsScreen.createScreen(this, this::refreshScreen));
                    }
                });
        y += 28;

        // 通类匹配组按钮
        addOpenScreenButton(centerX, y,
                Component.translatable("gui.server_config.button.edit_category_groups").getString(),
                Component.translatable("gui.server_config.button.edit_category_groups.tooltip").getString(),
                () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(MaterialCategoryGroupsScreen.createScreen(this, this::refreshScreen));
                    }
                });
        y += 28;

        // 专家模式跳过列表按钮
        addOpenScreenButton(centerX, y,
                Component.translatable("gui.server_config.button.edit_expert_skip_list").getString(),
                Component.translatable("gui.server_config.button.edit_expert_skip_list.tooltip").getString(),
                () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(ExpertModeSkipListScreen.createScreen(this, this::refreshScreen));
                    }
                });
    }

    private void addOpenScreenButton(int centerX, int y, String displayName, String tooltip, Runnable onPress) {
        int buttonWidth = Math.min(300, this.width - 100);
        Button button = Button.builder(nn(Component.literal(safeString(displayName))), b -> onPress.run())
                .pos(centerX - buttonWidth / 2, y)
                .size(buttonWidth, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(nn(Component.literal(safeString(tooltip)))))
                .build();
        this.addRenderableWidget(nn(button));
        configEntries.add(new ConfigEntry(displayName, "", ConfigType.BUTTON, button, 0, 0, tooltip, centerX, y, false));
    }

    private void refreshScreen() {
        this.init();
    }

    private void addPageTitleLabel(int centerX, int y, String title) {
        // 页面标题作为虚拟配置项存储，用于渲染
        configEntries.add(new ConfigEntry(title, "", ConfigType.LABEL, null, 0, 0, "", centerX, y, true));
    }

    private void addSectionLabel(int centerX, int y, String title) {
        // 分组标题
        configEntries.add(new ConfigEntry(title, "", ConfigType.LABEL, null, 0, 0, "", centerX, y, false));
    }

    private void addBooleanConfig(int centerX, int y, String displayName, String configKey, String tooltip, boolean initialValue) {
        int checkboxWidth = Math.min(280, this.width - 120);
        Checkbox checkbox = new Checkbox(centerX - checkboxWidth / 2, 0, checkboxWidth, 20,
                nn(Component.literal(safeString(displayName))), initialValue);
        checkbox.setTooltip(Tooltip.create(nn(Component.literal(safeString(tooltip)))));
        this.addRenderableWidget(nn(checkbox));
        configEntries.add(new ConfigEntry(displayName, configKey, ConfigType.BOOLEAN, checkbox, 0, 0, tooltip, centerX, y, false));
    }

    private void addIntConfig(int centerX, int y, String displayName, String configKey, String tooltip, int initialValue, int min, int max) {
        int totalWidth = Math.min(320, this.width - 100);
        int inputWidth = Math.min(70, totalWidth / 4);
        int inputX = centerX + totalWidth / 2 - inputWidth;

        EditBox editBox = new EditBox(nn(this.font), inputX, 0, inputWidth, 20,
                nn(Component.literal(safeString(displayName))));
        editBox.setValue(safeString(String.valueOf(initialValue)));
        editBox.setTooltip(Tooltip.create(nn(Component.literal(safeString(tooltip)))));
        this.addRenderableWidget(nn(editBox));
        configEntries.add(new ConfigEntry(displayName, configKey, ConfigType.INTEGER, editBox, min, max, tooltip, centerX, y, false));
    }

    @Override
    public void render(@Nonnull net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 记录首次渲染时间
        if (this.firstRenderTime == 0L) {
            this.firstRenderTime = System.currentTimeMillis();
        }

        // 渲染全景图背景 (Minecraft 1.20.1 API)
        PANORAMA.render(partialTick, 1.0F);

        // 渲染叠加层
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        guiGraphics.blit(PANORAMA_OVERLAY_TEXTURES, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);

        int panelWidth = Math.min(420, this.width - PANEL_MARGIN_X * 2);
        int panelHeight = this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = PANEL_MARGIN_TOP;
        int contentX = panelX + CONTENT_PADDING;
        int contentWidth = panelWidth - CONTENT_PADDING * 2;

        // 主面板背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);
        guiGraphics.renderOutline(panelX - 1, panelY - 1, panelWidth + 2, panelHeight + 2, 0xFF555555);

        // 渲染标题
        guiGraphics.drawCenteredString(nn(this.font), nn(this.title), this.width / 2, 10, COLOR_TITLE);

        // 渲染页面指示器
        Component pageText = Component.translatable("gui.server_config.page_indicator", currentPage + 1, MAX_PAGES);
        guiGraphics.drawCenteredString(nn(this.font), nn(pageText), this.width / 2, 22, COLOR_SUBTITLE);

        // 更新控件位置
        int scrolledY = (int) -scrollOffset;
        for (ConfigEntry entry : configEntries) {
            int renderY = panelY + 10 + entry.y + scrolledY;

            if (entry.type == ConfigType.BOOLEAN && entry.widget instanceof Checkbox checkbox) {
                checkbox.setY(renderY);
            } else if (entry.type == ConfigType.INTEGER && entry.widget instanceof EditBox editBox) {
                editBox.setY(renderY);
            } else if (entry.type == ConfigType.BUTTON && entry.widget instanceof Button button) {
                button.setY(renderY);
            }
        }

        // 启用裁剪区域 - 只裁剪内容区域
        guiGraphics.enableScissor(contentX, panelY + 5, contentX + contentWidth, panelY + panelHeight - 5);

        // 渲染配置项标签和标题
        for (ConfigEntry entry : configEntries) {
            int renderY = panelY + 10 + entry.y + scrolledY;

            if (entry.type == ConfigType.INTEGER) {
                // 渲染标签
                int totalWidth = Math.min(320, this.width - 100);
                int labelX = entry.centerX - totalWidth / 2;
                guiGraphics.drawString(nn(this.font), nn(Component.literal(safeString(entry.displayName))),
                        labelX, renderY + 6, COLOR_LABEL);
            } else if (entry.type == ConfigType.LABEL) {
                // 渲染标题
                int color = entry.isPageTitle ? COLOR_TITLE : COLOR_SUBTITLE;
                guiGraphics.drawCenteredString(nn(this.font), nn(Component.literal(safeString(entry.displayName))),
                        entry.centerX, renderY, color);
            }
        }

        // 渲染可滚动区域内的控件（复选框、输入框和配置按钮）
        for (ConfigEntry entry : configEntries) {
            if (entry.widget instanceof Checkbox checkbox) {
                checkbox.render(guiGraphics, mouseX, mouseY, partialTick);
            } else if (entry.widget instanceof EditBox editBox) {
                editBox.render(guiGraphics, mouseX, mouseY, partialTick);
            } else if (entry.widget instanceof Button button && entry.type == ConfigType.BUTTON) {
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        // 禁用裁剪
        guiGraphics.disableScissor();

        // 渲染底部导航按钮（在裁剪区域外，确保不被裁剪）
        // 只渲染导航按钮，配置按钮已在裁剪区域内渲染
        this.prevButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.reloadButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.nextButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染滚动条
        if (contentHeight > viewportHeight) {
            renderScrollbar(guiGraphics, panelX + panelWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN,
                    panelY + 5, SCROLLBAR_WIDTH, panelHeight - 10);
        }

        // 渲染工具提示由各个控件自己处理
    }

    private void renderScrollbar(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 滚动条背景
        guiGraphics.fill(x, y, x + width, y + height, COLOR_SCROLLBAR_BG);

        // 计算滑块位置和大小
        float visibleRatio = (float) viewportHeight / contentHeight;
        int thumbHeight = Math.max(20, (int) (height * visibleRatio));
        float scrollRatio = (float) scrollOffset / (contentHeight - viewportHeight);
        int thumbY = y + (int) ((height - thumbHeight) * scrollRatio);

        // 滑块颜色
        int thumbColor = isScrolling ? COLOR_SCROLLBAR_THUMB_HOVER : COLOR_SCROLLBAR_THUMB;

        // 渲染滑块
        guiGraphics.fill(x + 1, thumbY, x + width - 1, thumbY + thumbHeight, thumbColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelWidth = Math.min(420, this.width - PANEL_MARGIN_X * 2);
            int panelHeight = this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM;
            int panelX = (this.width - panelWidth) / 2;
            int panelY = PANEL_MARGIN_TOP;
            int contentX = panelX + CONTENT_PADDING;
            int contentWidth = panelWidth - CONTENT_PADDING * 2;
            int scrollbarX = panelX + panelWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
            int scrollbarY = PANEL_MARGIN_TOP + 5;
            int scrollbarHeight = panelHeight - 10;

            // 检查鼠标是否在裁剪区域内（内容区域）
        boolean isInContentArea = mouseX >= contentX && mouseX <= contentX + contentWidth &&
                                 mouseY >= panelY + 5 && mouseY <= panelY + panelHeight - 5;

        // 滚动条区域不受裁剪限制
        if (contentHeight > viewportHeight &&
            mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            isScrolling = true;
            setScrollFromMouse(mouseY, scrollbarY, scrollbarHeight);
            return true;
        }

        // 导航按钮区域不受裁剪限制（位于面板底部）
        int buttonY = this.height - BUTTON_MARGIN_BOTTOM - BUTTON_HEIGHT;
        int buttonHeight = BUTTON_HEIGHT;
        if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            // 鼠标在导航按钮区域，让父类处理按钮点击
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 如果鼠标在裁剪区域外，不处理配置项的点击事件
        if (!isInContentArea) {
            return false;
        }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling && button == 0) {
            int panelHeight = this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM;
            int scrollbarY = PANEL_MARGIN_TOP + 5;
            int scrollbarHeight = panelHeight - 10;
            setScrollFromMouse(mouseY, scrollbarY, scrollbarHeight);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentHeight > viewportHeight) {
            double scrollSpeed = 20;
            scrollOffset = Mth.clamp(scrollOffset - delta * scrollSpeed, 0, contentHeight - viewportHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void setScrollFromMouse(double mouseY, int scrollbarY, int scrollbarHeight) {
        float visibleRatio = (float) viewportHeight / contentHeight;
        int thumbHeight = Math.max(20, (int) (scrollbarHeight * visibleRatio));
        int availableHeight = scrollbarHeight - thumbHeight;

        double ratio = Mth.clamp((mouseY - scrollbarY - thumbHeight / 2.0) / availableHeight, 0, 1);
        scrollOffset = ratio * (contentHeight - viewportHeight);
    }

    private void saveConfig() {
        int syncCount = 0;
        for (ConfigEntry entry : configEntries) {
            if (entry.type == ConfigType.LABEL || entry.type == ConfigType.BUTTON) continue;

            try {
                switch (entry.type) {
                    case BOOLEAN -> {
                        Checkbox checkbox = (Checkbox) entry.widget;
                        boolean value = checkbox.selected();
                        applyBooleanConfig(entry.configKey, value);
                        com.xiaoliang.simukraft.network.NetworkManager.sendToServer(
                                new SyncConfigPacket(entry.configKey, value));
                        syncCount++;
                    }
                    case INTEGER -> {
                        EditBox editBox = (EditBox) entry.widget;
                        int value = Integer.parseInt(editBox.getValue());
                        value = Math.max(entry.min, Math.min(entry.max, value));
                        applyIntConfig(entry.configKey, value);
                        com.xiaoliang.simukraft.network.NetworkManager.sendToServer(
                                new SyncConfigPacket(entry.configKey, value));
                        syncCount++;
                    }
                    case STRING, LIST, LABEL, BUTTON -> {
                        // 当前界面只同步布尔和整数配置，其余类型由专用子界面或按钮处理。
                    }
                }
            } catch (Exception e) {
                Simukraft.LOGGER.error("[ServerConfig] 同步配置 '{}' 时出错: {}", entry.configKey, e.getMessage());
            }
        }

        ServerConfig.SPEC.save();
        ServerConfig.clearCache();
        Simukraft.LOGGER.info("[ServerConfig] 已同步 {} 个配置项并保存到文件", syncCount);

        Minecraft.getInstance().setScreen(new MessageScreen(this.parent,
                Component.translatable("gui.message.title.saved"),
                Component.translatable("gui.message.desc.saved")));
    }

    private void applyBooleanConfig(String configKey, boolean value) {
        switch (configKey) {
            case "enableBlacklistProtection" -> ServerConfig.ENABLE_BLACKLIST_PROTECTION.set(value);
            case "logSkippedBlocks" -> ServerConfig.LOG_BLACKLIST_SKIPPED_BLOCKS.set(value);
            case "enableDebugLog" -> ServerConfig.ENABLE_DEBUG_LOG.set(value);
            case "farmerEnableCropGrowthBoost" -> ServerConfig.FARMER_ENABLE_CROP_GROWTH_BOOST.set(value);
            case "plannerDropItemsOnRemove" -> ServerConfig.PLANNER_DROP_ITEMS_ON_REMOVE.set(value);
            case "plannerStoreItemsInChest" -> ServerConfig.PLANNER_STORE_ITEMS_IN_CHEST.set(value);
            case "plannerEnableXpGain" -> ServerConfig.PLANNER_ENABLE_XP_GAIN.set(value);
            case "builderRequireMaterials" -> ServerConfig.BUILDER_REQUIRE_MATERIALS.set(value);
            case "builderEnableXpGain" -> ServerConfig.BUILDER_ENABLE_XP_GAIN.set(value);
            case "builderForceLoadChunks" -> ServerConfig.BUILDER_FORCE_LOAD_CHUNKS.set(value);
            case "enableExpertMode" -> ServerConfig.ENABLE_EXPERT_MODE.set(value);
            case "enableMaterialCategoryMatching" -> ServerConfig.ENABLE_MATERIAL_CATEGORY_MATCHING.set(value);
        }
    }

    private void applyIntConfig(String configKey, int value) {
        switch (configKey) {
            case "npcMaxLevel" -> ServerConfig.NPC_MAX_LEVEL.set(value);
            case "npcSpeedBonusPerLevel" -> ServerConfig.NPC_SPEED_BONUS_PER_LEVEL.set(value);
            case "npcMinSpeedTicks" -> ServerConfig.NPC_MIN_SPEED_TICKS.set(value);
            case "plannerRemoveSpeedBase" -> ServerConfig.PLANNER_REMOVE_SPEED_BASE.set(value);
            case "plannerReplaceSpeedBase" -> ServerConfig.PLANNER_REPLACE_SPEED_BASE.set(value);
            case "plannerFillSpeedBase" -> ServerConfig.PLANNER_FILL_SPEED_BASE.set(value);
            case "plannerChestSearchRange" -> ServerConfig.PLANNER_CHEST_SEARCH_RANGE.set(value);
            case "plannerWarningCooldown" -> ServerConfig.PLANNER_WARNING_COOLDOWN.set(value);
            case "plannerRestCheckInterval" -> ServerConfig.PLANNER_REST_CHECK_INTERVAL.set(value);
            case "plannerXpPerBlock" -> ServerConfig.PLANNER_XP_PER_BLOCK.set(value);
            case "builderPlaceSpeedBase" -> ServerConfig.BUILDER_PLACE_SPEED_BASE.set(value);
            case "builderChestSearchRange" -> ServerConfig.BUILDER_CHEST_SEARCH_RANGE.set(value);
            case "builderWarningCooldown" -> ServerConfig.BUILDER_WARNING_COOLDOWN.set(value);
            case "builderXpPerBlock" -> ServerConfig.BUILDER_XP_PER_BLOCK.set(value);
            case "builderChunkLoadRadius" -> ServerConfig.BUILDER_CHUNK_LOAD_RADIUS.set(value);
            case "builderChunkLoadWaitTicks" -> ServerConfig.BUILDER_CHUNK_LOAD_WAIT_TICKS.set(value);
        }
    }

    private void reloadConfig() {
        com.xiaoliang.simukraft.network.NetworkManager.sendToServer(
                new com.xiaoliang.simukraft.network.ReloadConfigPacket());
        this.rebuildWidgets();
        Minecraft.getInstance().setScreen(new MessageScreen(this.parent,
                Component.translatable("gui.message.title.reloaded"),
                Component.translatable("gui.message.desc.reloaded")));
    }

    private void resetConfig() {
        com.xiaoliang.simukraft.network.NetworkManager.sendToServer(
                new com.xiaoliang.simukraft.network.ResetConfigPacket());
        this.rebuildWidgets();
        Simukraft.LOGGER.info("[ServerConfig] 配置重置请求已发送");
        Minecraft.getInstance().setScreen(new MessageScreen(this.parent,
                Component.translatable("gui.message.title.reset"),
                Component.translatable("gui.message.desc.reset")));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private enum ConfigType {
        BOOLEAN, INTEGER, STRING, LIST, LABEL, BUTTON
    }

    private static class ConfigEntry {
        final String displayName;
        final String configKey;
        final ConfigType type;
        final Object widget;
        final int min;
        final int max;
        @SuppressWarnings("unused")
        final String tooltip;
        final int centerX;
        final int y;
        final boolean isPageTitle;

        ConfigEntry(String displayName, String configKey, ConfigType type, Object widget,
                    int min, int max, String tooltip, int centerX, int y, boolean isPageTitle) {
            this.displayName = displayName;
            this.configKey = configKey;
            this.type = type;
            this.widget = widget;
            this.min = min;
            this.max = max;
            this.tooltip = tooltip;
            this.centerX = centerX;
            this.y = y;
            this.isPageTitle = isPageTitle;
        }
    }
}
