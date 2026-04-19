package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 可配置列表界面基类
 * 支持搜索、图标显示、自动补全
 */
public abstract class ConfigurableListScreen extends Screen {
    protected final Screen parent;
    protected final String title;
    protected final Consumer<List<String>> onSave;

    // 列表数据
    protected List<String> items;
    protected List<String> filteredItems;
    protected List<String> availableItems; // 可添加的物品列表（用于自动补全）

    // UI组件
    protected EditBox searchBox;
    protected EditBox addItemBox;
    protected Button addButton;
    protected Button saveButton;
    protected Button cancelButton;
    protected Button clearSearchButton;

    // 滚动相关
    protected double scrollOffset = 0;
    protected boolean isScrolling = false;
    protected int contentHeight = 0;
    protected int viewportHeight = 0;
    protected static final int SCROLLBAR_WIDTH = 6;
    protected static final int ITEM_HEIGHT = 24;
    protected static final int MAX_VISIBLE_ITEMS = 10;

    // 布局常量
    protected static final int PANEL_MARGIN_X = 30;
    protected static final int PANEL_MARGIN_TOP = 50;
    protected static final int PANEL_MARGIN_BOTTOM = 60;
    protected static final int CONTENT_PADDING = 10;

    // 颜色常量
    protected static final int COLOR_TITLE = 0xFF55FF55;
    protected static final int COLOR_PANEL_BG = 0x99000000;
    protected static final int COLOR_ITEM_BG = 0xFF333333;
    protected static final int COLOR_ITEM_BG_HOVER = 0xFF444444;
    protected static final int COLOR_SCROLLBAR_BG = 0xFF333333;
    protected static final int COLOR_SCROLLBAR_THUMB = 0xFF888888;
    protected static final int COLOR_SCROLLBAR_THUMB_HOVER = 0xFFAAAAAA;
    protected static final int COLOR_TEXT = 0xFFFFFFFF;
    protected static final int COLOR_TEXT_GRAY = 0xFFAAAAAA;
    protected static final int COLOR_DELETE_BUTTON = 0xFFFF5555;

    // 搜索相关
    protected List<String> searchSuggestions = new ArrayList<>();
    protected int selectedSuggestionIndex = -1;
    protected boolean showSuggestions = false;

    public ConfigurableListScreen(Screen parent, String title, List<String> initialItems,
                                  List<String> availableItems, Consumer<List<String>> onSave) {
        super(Component.literal(safeString(title)));
        this.parent = parent;
        this.title = title;
        this.items = new ArrayList<>(initialItems);
        this.filteredItems = new ArrayList<>(items);
        this.availableItems = new ArrayList<>(availableItems);
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelWidth = Math.min(500, this.width - PANEL_MARGIN_X * 2);
        int panelX = (this.width - panelWidth) / 2;
        int contentX = panelX + CONTENT_PADDING;
        int contentWidth = panelWidth - CONTENT_PADDING * 2;

        // 搜索框
        this.searchBox = nn(new EditBox(nn(this.font), contentX, PANEL_MARGIN_TOP + 5,
                contentWidth - 60, 18, nn(Component.literal(""))));
        nn(this.searchBox).setHint(nn(Component.translatable("gui.configurable_list.search_hint")));
        nn(this.searchBox).setMaxLength(100); // 设置最大长度
        nn(this.searchBox).setResponder(this::onSearchChanged);
        this.addRenderableWidget(nn(this.searchBox));

        // 清除搜索按钮
        this.clearSearchButton = nn(Button.builder(nn(Component.literal("§c✕")), button -> {
            nn(this.searchBox).setValue("");
            this.onSearchChanged("");
        }).pos(contentX + contentWidth - 55, PANEL_MARGIN_TOP + 5).size(25, 18).build());
        this.addRenderableWidget(nn(this.clearSearchButton));

        // 添加物品框
        this.addItemBox = nn(new EditBox(nn(this.font), contentX, PANEL_MARGIN_TOP + 30,
                contentWidth - 60, 18, nn(Component.literal(""))));
        nn(this.addItemBox).setHint(nn(Component.translatable("gui.configurable_list.add_item_hint")));
        nn(this.addItemBox).setMaxLength(100); // 设置最大长度，支持长物品ID如 minecraft:stripped_dark_oak_wood
        nn(this.addItemBox).setResponder(this::onAddBoxChanged);
        this.addRenderableWidget(nn(this.addItemBox));

        // 添加按钮
        this.addButton = nn(Button.builder(nn(Component.literal("§a+")), button -> addCurrentItem())
                .pos(contentX + contentWidth - 55, PANEL_MARGIN_TOP + 30)
                .size(50, 18)
                .build());
        this.addRenderableWidget(nn(this.addButton));

        // 底部按钮
        int buttonY = this.height - 45;
        int buttonWidth = 70;
        int buttonSpacing = 10;

        this.saveButton = nn(Button.builder(nn(Component.translatable("gui.configurable_list.save")), button -> saveAndClose())
                .pos(centerX - buttonWidth - buttonSpacing, buttonY)
                .size(buttonWidth, 20)
                .build());
        this.addRenderableWidget(nn(this.saveButton));

        this.cancelButton = nn(Button.builder(nn(Component.translatable("gui.configurable_list.cancel")), button -> onClose())
                .pos(centerX + buttonSpacing, buttonY)
                .size(buttonWidth, 20)
                .build());
        this.addRenderableWidget(nn(this.cancelButton));

        // 计算可视区域高度
        this.viewportHeight = this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM - 60;

        updateFilteredItems();
    }

    protected void onSearchChanged(String search) {
        updateFilteredItems();
    }

    protected void onAddBoxChanged(String text) {
        updateSuggestions(text);
    }

    protected void updateSuggestions(String text) {
        searchSuggestions.clear();
        selectedSuggestionIndex = -1;

        if (text == null || text.trim().isEmpty()) {
            showSuggestions = false;
            return;
        }

        String lowerText = text.toLowerCase();
        searchSuggestions = availableItems.stream()
                .filter(item -> item.toLowerCase().contains(lowerText))
                .filter(item -> !items.contains(item))
                .limit(5)
                .collect(Collectors.toList());

        showSuggestions = !searchSuggestions.isEmpty();
    }

    protected void updateFilteredItems() {
        String search = searchBox != null ? nn(searchBox).getValue().toLowerCase().trim() : "";
        if (search.isEmpty()) {
            filteredItems = new ArrayList<>(items);
        } else {
            filteredItems = items.stream()
                    .filter(item -> item.toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }

        // 计算内容高度
        this.contentHeight = filteredItems.size() * ITEM_HEIGHT + 10;

        // 重置滚动位置
        this.scrollOffset = 0;
    }

    protected void addCurrentItem() {
        String itemId = addItemBox != null ? nn(addItemBox).getValue().trim() : "";
        if (itemId.isEmpty()) return;

        // 验证物品是否存在
        if (!isValidItem(itemId)) {
            // 显示错误提示
            if (Minecraft.getInstance().player != null) {
                nn(Minecraft.getInstance().player).displayClientMessage(
                        nn(Component.translatable("message.simukraft.config.invalid_item_id", itemId)), false);
            }
            return;
        }

        if (!items.contains(itemId)) {
            items.add(itemId);
            if (addItemBox != null) {
                nn(addItemBox).setValue("");
            }
            updateFilteredItems();
            showSuggestions = false;
        }
    }

    protected boolean isValidItem(String itemId) {
        // 检查是否是有效的物品或方块ID
        ResourceLocation location = ResourceLocation.tryParse(safeString(itemId));
        if (location == null) return false;

        // 检查是否是有效的方块
        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != Blocks.AIR) {
            return true;
        }

        // 检查是否是有效的物品
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item != null && item != Items.AIR;
    }

    protected void removeItem(String item) {
        items.remove(item);
        updateFilteredItems();
    }

    protected void saveAndClose() {
        if (onSave != null) {
            onSave.accept(new ArrayList<>(items));
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            nn(this.minecraft).setScreen(parent);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(nn(guiGraphics));

        int panelWidth = Math.min(500, this.width - PANEL_MARGIN_X * 2);
        int panelHeight = this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM + 10;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = PANEL_MARGIN_TOP;
        int contentX = panelX + CONTENT_PADDING;
        int contentWidth = panelWidth - CONTENT_PADDING * 2;

        // 主面板背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);
        guiGraphics.renderOutline(panelX - 1, panelY - 1, panelWidth + 2, panelHeight + 2, 0xFF555555);

        // 渲染标题
        guiGraphics.drawCenteredString(nn(this.font), nn(Component.literal(safeString(title))), this.width / 2, 15, COLOR_TITLE);

        // 渲染物品数量
        String countText = String.format("§7共 §e%d§7 项", filteredItems.size());
        guiGraphics.drawString(nn(this.font), nn(Component.literal(safeString(countText))),
                contentX + contentWidth - 80, PANEL_MARGIN_TOP + 8, COLOR_TEXT_GRAY);

        // 启用裁剪区域
        int listStartY = PANEL_MARGIN_TOP + 60;
        guiGraphics.enableScissor(contentX, listStartY, contentX + contentWidth, listStartY + viewportHeight);

        // 渲染列表项
        int scrolledY = (int) -scrollOffset;
        for (int i = 0; i < filteredItems.size(); i++) {
            String item = filteredItems.get(i);
            int itemY = listStartY + scrolledY + i * ITEM_HEIGHT;

            // 检查是否在可视区域内
            if (itemY + ITEM_HEIGHT < listStartY || itemY > listStartY + viewportHeight) {
                continue;
            }

            // 渲染项背景
            boolean isHovered = mouseX >= contentX && mouseX <= contentX + contentWidth - 20
                    && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 2;
            int bgColor = isHovered ? COLOR_ITEM_BG_HOVER : COLOR_ITEM_BG;
            guiGraphics.fill(contentX, itemY, contentX + contentWidth - 20, itemY + ITEM_HEIGHT - 2, bgColor);

            // 渲染物品图标
            renderItemIcon(guiGraphics, item, contentX + 3, itemY + 3);

            // 渲染物品名称
            String displayName = getItemDisplayName(item);
            guiGraphics.drawString(nn(this.font), nn(Component.literal("§f" + safeString(displayName))),
                    contentX + 22, itemY + 7, COLOR_TEXT);

            // 渲染物品ID（灰色小字）
            guiGraphics.drawString(nn(this.font), nn(Component.literal("§7" + safeString(item))),
                    contentX + 22, itemY + 16, COLOR_TEXT_GRAY);

            // 渲染删除按钮
            int deleteButtonX = contentX + contentWidth - 18;
            boolean deleteHovered = mouseX >= deleteButtonX && mouseX <= deleteButtonX + 16
                    && mouseY >= itemY + 4 && mouseY <= itemY + 20;
            int deleteColor = deleteHovered ? 0xFFFF7777 : COLOR_DELETE_BUTTON;
            guiGraphics.fill(deleteButtonX, itemY + 4, deleteButtonX + 16, itemY + 20, deleteColor);
            guiGraphics.drawCenteredString(nn(this.font), nn(Component.literal("§c✕")),
                    deleteButtonX + 8, itemY + 7, 0xFFFFFFFF);
        }

        // 禁用裁剪
        guiGraphics.disableScissor();

        // 渲染滚动条
        if (contentHeight > viewportHeight) {
            renderScrollbar(guiGraphics, panelX + panelWidth - SCROLLBAR_WIDTH - 2,
                    listStartY, SCROLLBAR_WIDTH, viewportHeight);
        }

        // 渲染其他控件（输入框等）
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染建议列表（在控件之后渲染，确保显示在最上层）
        if (showSuggestions && !searchSuggestions.isEmpty()) {
            renderSuggestions(guiGraphics, mouseX, mouseY);
        }
    }

    protected void renderItemIcon(GuiGraphics guiGraphics, String itemId, int x, int y) {
        ResourceLocation location = ResourceLocation.tryParse(safeString(itemId));
        if (location == null) return;

        ItemStack stack = ItemStack.EMPTY;

        // 尝试获取方块对应的物品
        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != Blocks.AIR) {
            stack = new ItemStack(block);
        }

        // 如果不是方块，尝试获取物品
        if (stack.isEmpty()) {
            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null && item != Items.AIR) {
                stack = new ItemStack(item);
            }
        }

        if (!stack.isEmpty()) {
            guiGraphics.renderItem(nn(stack), x, y);
        } else {
            // 渲染一个默认图标
            guiGraphics.fill(x, y, x + 16, y + 16, 0xFF666666);
        }
    }

    protected String getItemDisplayName(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(safeString(itemId));
        if (location == null) return itemId;

        // 尝试获取方块的显示名称
        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != Blocks.AIR) {
            return nn(block.getName()).getString();
        }

        // 尝试获取物品的显示名称
        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item != null && item != Items.AIR) {
            return nn(item.getDescription()).getString();
        }

        return itemId;
    }

    protected void renderScrollbar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 滚动条背景
        guiGraphics.fill(x, y, x + width, y + height, COLOR_SCROLLBAR_BG);

        // 计算滑块位置和大小
        float contentRatio = (float) viewportHeight / contentHeight;
        int thumbHeight = Math.max(20, (int) (height * contentRatio));
        float scrollRatio = (float) scrollOffset / (contentHeight - viewportHeight);
        int thumbY = y + (int) ((height - thumbHeight) * scrollRatio);

        // 渲染滑块
        int thumbColor = isScrolling ? COLOR_SCROLLBAR_THUMB_HOVER : COLOR_SCROLLBAR_THUMB;
        guiGraphics.fill(x + 1, thumbY, x + width - 1, thumbY + thumbHeight, thumbColor);
    }

    protected void renderSuggestions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int suggestionX = addItemBox != null ? nn(addItemBox).getX() : 0;
        int suggestionY = addItemBox != null ? nn(addItemBox).getY() + nn(addItemBox).getHeight() : 0;
        int suggestionWidth = addItemBox != null ? nn(addItemBox).getWidth() : 0;
        int suggestionHeight = Math.min(searchSuggestions.size() * 18, 90);

        // 保存当前矩阵状态并提高Z层（使用更高的Z值确保在最上层）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000);

        // 渲染全屏黑色遮罩层（防止任何内容透出）
        // 只在建议菜单区域渲染遮罩
        guiGraphics.fill(suggestionX - 5, suggestionY - 3, suggestionX + suggestionWidth + 5,
                suggestionY + suggestionHeight + 3, 0xFF000000);

        // 渲染建议菜单主背景（多层确保不透明）
        guiGraphics.fill(suggestionX - 3, suggestionY - 3, suggestionX + suggestionWidth + 3,
                suggestionY + suggestionHeight + 3, 0xFF000000);
        guiGraphics.fill(suggestionX - 2, suggestionY - 2, suggestionX + suggestionWidth + 2,
                suggestionY + suggestionHeight + 2, 0xFF111111);
        guiGraphics.fill(suggestionX, suggestionY, suggestionX + suggestionWidth,
                suggestionY + suggestionHeight, 0xFF222222);
        guiGraphics.renderOutline(suggestionX - 1, suggestionY - 1, suggestionWidth + 2,
                suggestionHeight + 2, 0xFFAAAAAA);

        // 渲染建议项
        for (int i = 0; i < searchSuggestions.size(); i++) {
            String suggestion = searchSuggestions.get(i);
            int itemY = suggestionY + i * 18;

            boolean isSelected = i == selectedSuggestionIndex;
            boolean isHovered = mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth
                    && mouseY >= itemY && mouseY < itemY + 18;

            int bgColor = isSelected ? 0xFF5555AA : (isHovered ? 0xFF444444 : 0xFF333333);
            guiGraphics.fill(suggestionX, itemY, suggestionX + suggestionWidth, itemY + 18, bgColor);

            // 渲染图标
            renderItemIcon(guiGraphics, suggestion, suggestionX + 2, itemY + 1);

            // 渲染文本
            guiGraphics.drawString(nn(this.font), nn(Component.literal("§f" + safeString(suggestion))),
                    suggestionX + 20, itemY + 5, COLOR_TEXT);
        }

        // 恢复矩阵状态
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 优先检查建议菜单点击（确保它在最上层）
        if (showSuggestions && !searchSuggestions.isEmpty() && addItemBox != null) {
            int suggestionX = nn(addItemBox).getX();
            int suggestionY = nn(addItemBox).getY() + nn(addItemBox).getHeight();
            int suggestionWidth = nn(addItemBox).getWidth();
            int suggestionHeight = Math.min(searchSuggestions.size() * 18, 90);

            // 检查是否点击在建议菜单范围内
            if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth
                    && mouseY >= suggestionY && mouseY <= suggestionY + suggestionHeight) {
                // 点击了建议项
                for (int i = 0; i < searchSuggestions.size(); i++) {
                    int itemY = suggestionY + i * 18;
                    if (mouseY >= itemY && mouseY < itemY + 18) {
                        nn(addItemBox).setValue(safeString(searchSuggestions.get(i)));
                        showSuggestions = false;
                        return true;
                    }
                }
            } else {
                // 点击在建议菜单外，关闭建议列表
                showSuggestions = false;
            }
        }

        int panelWidth = Math.min(500, this.width - PANEL_MARGIN_X * 2);
        int contentWidth = panelWidth - CONTENT_PADDING * 2;
        int panelX = (this.width - panelWidth) / 2;
        int contentX = panelX + CONTENT_PADDING;
        int listStartY = PANEL_MARGIN_TOP + 60;

        // 检查鼠标是否在裁剪区域内（列表区域）
        boolean isInListArea = mouseX >= contentX && mouseX <= contentX + contentWidth &&
                              mouseY >= listStartY && mouseY <= listStartY + viewportHeight;

        // 顶部控件区域不受裁剪限制（搜索框、添加物品框等）
        boolean isInTopArea = mouseY >= PANEL_MARGIN_TOP && mouseY < listStartY;

        // 底部按钮区域不受裁剪限制（保存、取消按钮）
        int buttonY = this.height - 45;
        int buttonHeight = 20;
        boolean isInBottomArea = mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

        // 检查是否点击了滚动条（滚动条区域不受裁剪限制）
        if (contentHeight > viewportHeight) {
            int scrollbarX = panelX + panelWidth - SCROLLBAR_WIDTH - 2;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                    && mouseY >= listStartY && mouseY <= listStartY + viewportHeight) {
                isScrolling = true;
                updateScrollFromMouse(mouseY, listStartY);
                return true;
            }
        }

        // 如果鼠标在顶部控件区域或底部按钮区域，让父类处理点击事件
        if (isInTopArea || isInBottomArea) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 如果鼠标在裁剪区域外，不处理列表项的点击事件
        if (!isInListArea) {
            return false;
        }

        // 检查是否点击了删除按钮
        int scrolledY = (int) -scrollOffset;
        for (int i = 0; i < filteredItems.size(); i++) {
            String item = filteredItems.get(i);
            int itemY = listStartY + scrolledY + i * ITEM_HEIGHT;

            if (itemY + ITEM_HEIGHT < listStartY || itemY > listStartY + viewportHeight) {
                continue;
            }

            int deleteButtonX = contentX + contentWidth - 18;
            if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + 16
                    && mouseY >= itemY + 4 && mouseY <= itemY + 20) {
                removeItem(item);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling) {
            int listStartY = PANEL_MARGIN_TOP + 60;
            updateScrollFromMouse(mouseY, listStartY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelWidth = Math.min(500, this.width - PANEL_MARGIN_X * 2);
        int panelX = (this.width - panelWidth) / 2;
        int contentX = panelX + CONTENT_PADDING;
        int listStartY = PANEL_MARGIN_TOP + 60;

        // 检查鼠标是否在列表区域内
        if (mouseX >= contentX && mouseX <= contentX + panelWidth - CONTENT_PADDING * 2
                && mouseY >= listStartY && mouseY <= listStartY + viewportHeight) {
            scrollOffset -= delta * 20;
            clampScrollOffset();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    protected void updateScrollFromMouse(double mouseY, int listStartY) {
        float scrollRatio = (float) (mouseY - listStartY) / viewportHeight;
        scrollRatio = Math.max(0, Math.min(1, scrollRatio));
        scrollOffset = scrollRatio * (contentHeight - viewportHeight);
        clampScrollOffset();
    }

    protected void clampScrollOffset() {
        if (contentHeight <= viewportHeight) {
            scrollOffset = 0;
        } else {
            scrollOffset = Math.max(0, Math.min(scrollOffset, contentHeight - viewportHeight));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理建议列表的键盘导航
        if (showSuggestions && !searchSuggestions.isEmpty()) {
            switch (keyCode) {
                case 265: // UP
                    selectedSuggestionIndex = Math.max(0, selectedSuggestionIndex - 1);
                    return true;
                case 264: // DOWN
                    selectedSuggestionIndex = Math.min(searchSuggestions.size() - 1, selectedSuggestionIndex + 1);
                    return true;
                case 257: // ENTER
                case 335: // NUMPAD_ENTER
                    if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < searchSuggestions.size()) {
                        if (addItemBox != null) {
                            nn(addItemBox).setValue(safeString(searchSuggestions.get(selectedSuggestionIndex)));
                        }
                        showSuggestions = false;
                        return true;
                    }
                    break;
                case 256: // ESC
                    showSuggestions = false;
                    return true;
            }
        }

        // Ctrl+S 保存
        if (keyCode == 83 && (modifiers & 2) != 0) { // Ctrl+S
            saveAndClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        String searchValue = searchBox != null ? nn(searchBox).getValue() : "";
        String addValue = addItemBox != null ? nn(addItemBox).getValue() : "";
        super.resize(nn(minecraft), width, height);
        if (searchBox != null) {
            nn(searchBox).setValue(safeString(searchValue));
        }
        if (addItemBox != null) {
            nn(addItemBox).setValue(safeString(addValue));
        }
    }

    /**
     * 获取所有可用的物品ID列表
     * 子类可以重写此方法提供特定的可用物品列表
     */
    protected abstract List<String> getAllAvailableItems();

    @Nonnull
    private <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return Objects.requireNonNull(value);
    }
}
