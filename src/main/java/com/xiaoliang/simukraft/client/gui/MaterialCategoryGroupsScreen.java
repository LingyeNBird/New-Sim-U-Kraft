package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.config.ServerConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 通类匹配组配置界面
 * 用于配置普通模式下的材料通类匹配组
 * 新格式: 组名:组头1,组头2:组员1,组员2,组员3
 * 只有组头可以匹配组员，组员不能变成其他方块
 */
@SuppressWarnings("unused")
public class MaterialCategoryGroupsScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<String>> onSave;

    // 数据 - 组名列表
    private List<String> groupNames;
    private String selectedGroup = null;
    
    // 数据 - 独立的组头和组员存储，避免直接操作MaterialGroupInfo的列表
    private Map<String, List<String>> groupHeaders = new HashMap<>();
    private Map<String, List<String>> groupMembers = new HashMap<>();

    // UI组件 - 组列表
    private Button addGroupButton;
    private Button deleteGroupButton;
    private Button saveButton;
    private Button cancelButton;
    private EditBox newGroupNameBox;

    // UI组件 - 组头编辑
    private EditBox addHeaderBox;
    private Button addHeaderButton;

    // UI组件 - 组员编辑
    private EditBox addMemberBox;
    private Button addMemberButton;

    // 滚动相关 - 组列表
    private double groupScrollOffset = 0;
    private boolean isGroupScrolling = false;
    private static final int GROUP_ITEM_HEIGHT = 28;

    // 滚动相关 - 组头列表
    private double headerScrollOffset = 0;
    private boolean isHeaderScrolling = false;
    private static final int HEADER_ITEM_HEIGHT = 22;

    // 滚动相关 - 组员列表
    private double memberScrollOffset = 0;
    private boolean isMemberScrolling = false;
    private static final int MEMBER_ITEM_HEIGHT = 22;

    // 布局常量
    private static final int PANEL_MARGIN_X = 20;
    private static final int PANEL_MARGIN_TOP = 50;
    private static final int PANEL_MARGIN_BOTTOM = 50;
    private static final int CONTENT_PADDING = 10;
    private static final int SCROLLBAR_WIDTH = 6;

    // 颜色常量
    private static final int COLOR_TITLE = 0xFF55FF55;
    private static final int COLOR_PANEL_BG = 0x99000000;
    private static final int COLOR_ITEM_BG = 0xFF333333;
    private static final int COLOR_ITEM_BG_HOVER = 0xFF444444;
    private static final int COLOR_ITEM_BG_SELECTED = 0xFF5555AA;
    private static final int COLOR_HEADER_BG = 0xFF664400;
    private static final int COLOR_HEADER_BG_HOVER = 0xFF885500;
    private static final int COLOR_MEMBER_BG = 0xFF334466;
    private static final int COLOR_MEMBER_BG_HOVER = 0xFF446688;
    private static final int COLOR_SCROLLBAR_BG = 0xFF333333;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF888888;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xFFAAAAAA;
    private static final int COLOR_DELETE_BUTTON = 0xFFFF5555;
    private static final int COLOR_HEADER_LABEL = 0xFFFFAA00;
    private static final int COLOR_MEMBER_LABEL = 0xFF88AAFF;

    // 建议列表
    private List<String> headerSuggestions = new ArrayList<>();
    private List<String> memberSuggestions = new ArrayList<>();
    private boolean showHeaderSuggestions = false;
    private boolean showMemberSuggestions = false;

    public MaterialCategoryGroupsScreen(Screen parent, Map<String, ServerConfig.MaterialGroupInfo> initialGroups,
                                        Consumer<List<String>> onSave) {
        super(Component.translatable("gui.material_category_groups.title"));
        this.parent = parent;
        this.onSave = onSave;
        
        // 从MaterialGroupInfo中提取数据到独立的存储
        this.groupNames = new ArrayList<>(initialGroups.keySet());
        this.groupNames.sort(String::compareTo);
        
        for (Map.Entry<String, ServerConfig.MaterialGroupInfo> entry : initialGroups.entrySet()) {
            String groupName = entry.getKey();
            ServerConfig.MaterialGroupInfo group = entry.getValue();
            // 创建新的列表，避免共享引用
            this.groupHeaders.put(groupName, new ArrayList<>(group.getHeaders()));
            this.groupMembers.put(groupName, new ArrayList<>(group.getMembers()));
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

        int centerX = this.width / 2;
        int panelWidth = Math.min(800, this.width - PANEL_MARGIN_X * 2);
        int leftPanelWidth = (panelWidth - 40) / 3;
        int middlePanelWidth = (panelWidth - 40) / 3;
        int rightPanelWidth = panelWidth - 40 - leftPanelWidth - middlePanelWidth;
        int panelStartX = centerX - panelWidth / 2;
        int panelEndX = centerX + panelWidth / 2;
        int panelTop = PANEL_MARGIN_TOP - 20; // 向上移动
        int panelBottom = this.height - PANEL_MARGIN_BOTTOM;
        int contentHeight = panelBottom - panelTop;

        // 左侧面板 - 组列表
        int leftPanelX = panelStartX;
        int leftContentY = panelTop + 25; // 向上移动
        int leftContentHeight = contentHeight - 70;

        // 新建组输入框
        newGroupNameBox = new EditBox(nn(this.font), leftPanelX + CONTENT_PADDING, leftContentY - 25,
                leftPanelWidth - CONTENT_PADDING * 2 - 50, 20, nn(Component.literal("")));
        newGroupNameBox.setMaxLength(50);
        newGroupNameBox.setHint(nn(Component.translatable("gui.material_category_groups.new_group_hint")));
        this.addRenderableWidget(nn(newGroupNameBox));

        // 添加组按钮
        addGroupButton = Button.builder(nn(Component.literal("+")), button -> addNewGroup())
                .pos(leftPanelX + leftPanelWidth - 45, leftContentY - 25)
                .size(30, 20)
                .build();
        this.addRenderableWidget(nn(addGroupButton));

        // 删除组按钮
        deleteGroupButton = Button.builder(nn(Component.translatable("gui.material_category_groups.delete_group")), button -> deleteSelectedGroup())
                .pos(leftPanelX + CONTENT_PADDING, panelBottom - 30)
                .size(leftPanelWidth - CONTENT_PADDING * 2, 20)
                .build();
        this.addRenderableWidget(nn(deleteGroupButton));

        // 中间面板 - 组头列表
        int middlePanelX = leftPanelX + leftPanelWidth + 20;
        int middleContentY = panelTop + 25; // 向上移动
        int middleContentHeight = (contentHeight - 80) / 2;

        // 组头输入框和添加按钮
        addHeaderBox = new EditBox(nn(this.font), middlePanelX + CONTENT_PADDING, middleContentY - 25,
                middlePanelWidth - CONTENT_PADDING * 2 - 50, 20, nn(Component.literal("")));
        addHeaderBox.setMaxLength(100);
        addHeaderBox.setHint(nn(Component.translatable("gui.material_category_groups.add_header_hint")));
        addHeaderBox.setEditable(false);
        this.addRenderableWidget(nn(addHeaderBox));

        addHeaderButton = Button.builder(nn(Component.literal("+")), button -> addHeaderToGroup())
                .pos(middlePanelX + middlePanelWidth - 45, middleContentY - 25)
                .size(30, 20)
                .build();
        addHeaderButton.active = false;
        this.addRenderableWidget(nn(addHeaderButton));

        // 右侧面板 - 组员列表
        int rightPanelX = middlePanelX + middlePanelWidth + 20;
        int rightContentY = panelTop + 25; // 向上移动
        int rightContentHeight = (contentHeight - 80) / 2;

        // 组员输入框和添加按钮
        addMemberBox = new EditBox(nn(this.font), rightPanelX + CONTENT_PADDING, rightContentY - 25,
                rightPanelWidth - CONTENT_PADDING * 2 - 50, 20, nn(Component.literal("")));
        addMemberBox.setMaxLength(100);
        addMemberBox.setHint(nn(Component.translatable("gui.material_category_groups.add_member_hint")));
        addMemberBox.setEditable(false);
        this.addRenderableWidget(nn(addMemberBox));

        addMemberButton = Button.builder(nn(Component.literal("+")), button -> addMemberToGroup())
                .pos(rightPanelX + rightPanelWidth - 45, rightContentY - 25)
                .size(30, 20)
                .build();
        addMemberButton.active = false;
        this.addRenderableWidget(nn(addMemberButton));

        // 保存和取消按钮
        int buttonY = panelBottom - 30;
        int buttonWidth = 80;
        int buttonSpacing = 10;

        saveButton = Button.builder(nn(Component.translatable("gui.configurable_list.save")), button -> saveAndClose())
                .pos(panelEndX - buttonWidth * 2 - buttonSpacing, buttonY)
                .size(buttonWidth, 20)
                .build();
        this.addRenderableWidget(nn(saveButton));

        cancelButton = Button.builder(nn(Component.translatable("gui.configurable_list.cancel")), button -> onClose())
                .pos(panelEndX - buttonWidth, buttonY)
                .size(buttonWidth, 20)
                .build();
        this.addRenderableWidget(nn(cancelButton));

        updateUIState();
    }

    private void updateUIState() {
        boolean hasSelection = selectedGroup != null && groupNames.contains(selectedGroup);
        deleteGroupButton.active = hasSelection;
        addHeaderBox.setEditable(hasSelection);
        addHeaderButton.active = hasSelection;
        addMemberBox.setEditable(hasSelection);
        addMemberButton.active = hasSelection;
    }

    private void addNewGroup() {
        String groupName = newGroupNameBox.getValue().trim();
        if (groupName.isEmpty()) {
            return;
        }

        if (groupNames.contains(groupName)) {
            return;
        }

        groupNames.add(groupName);
        groupNames.sort(String::compareTo);
        groupHeaders.put(groupName, new ArrayList<>());
        groupMembers.put(groupName, new ArrayList<>());
        selectedGroup = groupName;
        newGroupNameBox.setValue("");
        groupScrollOffset = 0;
        headerScrollOffset = 0;
        memberScrollOffset = 0;
        updateUIState();
    }

    private void deleteSelectedGroup() {
        if (selectedGroup == null || !groupNames.contains(selectedGroup)) {
            return;
        }

        groupNames.remove(selectedGroup);
        groupHeaders.remove(selectedGroup);
        groupMembers.remove(selectedGroup);
        selectedGroup = null;
        groupScrollOffset = 0;
        headerScrollOffset = 0;
        memberScrollOffset = 0;
        updateUIState();
    }

    private void addHeaderToGroup() {
        if (selectedGroup == null) {
            return;
        }

        String headerId = addHeaderBox.getValue().trim();
        if (headerId.isEmpty()) {
            return;
        }

        // 标准化ID
        if (!headerId.contains(":")) {
            headerId = "minecraft:" + headerId;
        }

        List<String> headers = groupHeaders.get(selectedGroup);
        if (headers == null) return;

        // 检查是否已在组头中（允许组头和组员重复）
        if (headers.contains(headerId)) {
            addHeaderBox.setValue("");
            return;
        }

        headers.add(headerId);
        addHeaderBox.setValue("");
        headerScrollOffset = 0;
    }

    private void addMemberToGroup() {
        if (selectedGroup == null) {
            return;
        }

        String memberId = addMemberBox.getValue().trim();
        if (memberId.isEmpty()) {
            return;
        }

        // 标准化ID
        if (!memberId.contains(":")) {
            memberId = "minecraft:" + memberId;
        }

        List<String> members = groupMembers.get(selectedGroup);
        if (members == null) return;

        // 检查是否已在组员中（允许组头和组员重复）
        if (members.contains(memberId)) {
            addMemberBox.setValue("");
            return;
        }

        members.add(memberId);
        addMemberBox.setValue("");
        memberScrollOffset = 0;
    }

    private void removeHeaderFromGroup(String headerId) {
        if (selectedGroup == null) return;

        List<String> headers = groupHeaders.get(selectedGroup);
        if (headers != null) {
            headers.remove(headerId);
        }
    }

    private void removeMemberFromGroup(String memberId) {
        if (selectedGroup == null) return;

        List<String> members = groupMembers.get(selectedGroup);
        if (members != null) {
            members.remove(memberId);
        }
    }

    private void saveAndClose() {
        // 转换为配置格式，使用 | 作为分隔符避免与材料ID中的冒号冲突
        List<String> configList = new ArrayList<>();
        for (String groupName : groupNames) {
            StringBuilder sb = new StringBuilder();
            sb.append(groupName).append("|");

            // 组头
            List<String> headers = groupHeaders.getOrDefault(groupName, new ArrayList<>());
            if (!headers.isEmpty()) {
                sb.append(String.join(",", headers));
            }
            sb.append("|");

            // 组员
            List<String> members = groupMembers.getOrDefault(groupName, new ArrayList<>());
            if (!members.isEmpty()) {
                sb.append(String.join(",", members));
            }

            configList.add(sb.toString());
        }

        onSave.accept(configList);
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 背景
        renderBackground(graphics);

        int centerX = this.width / 2;
        int panelWidth = Math.min(800, this.width - PANEL_MARGIN_X * 2);
        int leftPanelWidth = (panelWidth - 40) / 3;
        int middlePanelWidth = (panelWidth - 40) / 3;
        int rightPanelWidth = panelWidth - 40 - leftPanelWidth - middlePanelWidth;
        int panelStartX = centerX - panelWidth / 2;
        int panelEndX = centerX + panelWidth / 2;
        int panelTop = PANEL_MARGIN_TOP;
        int panelBottom = this.height - PANEL_MARGIN_BOTTOM;

        // 标题
        graphics.drawCenteredString(nn(this.font), nn(this.title), centerX, 20, COLOR_TITLE);

        // 绘制说明文字
        graphics.drawString(nn(this.font),
                safeString(Component.translatable("gui.material_category_groups.description").getString()),
                panelStartX, 35, COLOR_TEXT_GRAY);

        // 左侧面板 - 组列表
        int leftPanelX = panelStartX;
        int leftPanelY = panelTop;
        int leftPanelHeight = panelBottom - panelTop;
        int leftContentY = leftPanelY + 40;
        int leftContentHeight = leftPanelHeight - 90;

        graphics.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelWidth, leftPanelY + leftPanelHeight, COLOR_PANEL_BG);
        graphics.drawString(nn(this.font),
                safeString(Component.translatable("gui.material_category_groups.group_list_title").getString()),
                leftPanelX + CONTENT_PADDING, leftPanelY + 10, COLOR_TITLE);

        // 绘制组列表
        renderGroupList(graphics, leftPanelX + CONTENT_PADDING, leftContentY,
                leftPanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH, leftContentHeight, mouseX, mouseY);

        // 中间面板 - 组头列表
        int middlePanelX = leftPanelX + leftPanelWidth + 20;
        int middlePanelY = panelTop;
        int middleContentY = middlePanelY + 40;
        int middleContentHeight = (panelBottom - panelTop - 100) / 2;

        graphics.fill(middlePanelX, middlePanelY, middlePanelX + middlePanelWidth, middlePanelY + middleContentY + middleContentHeight + 40, COLOR_PANEL_BG);
        graphics.drawString(nn(this.font),
                safeString(Component.translatable("gui.material_category_groups.header_list_title").getString()),
                middlePanelX + CONTENT_PADDING, middlePanelY + 10, COLOR_HEADER_LABEL);

        // 绘制组头列表
        renderHeaderList(graphics, middlePanelX + CONTENT_PADDING, middleContentY,
                middlePanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH, middleContentHeight, mouseX, mouseY);

        // 右侧面板 - 组员列表
        int rightPanelX = middlePanelX + middlePanelWidth + 20;
        int rightPanelY = panelTop;
        int rightContentY = rightPanelY + 40;
        int rightContentHeight = (panelBottom - panelTop - 100) / 2;

        graphics.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + rightContentY + rightContentHeight + 40, COLOR_PANEL_BG);
        graphics.drawString(nn(this.font),
                safeString(Component.translatable("gui.material_category_groups.member_list_title").getString()),
                rightPanelX + CONTENT_PADDING, rightPanelY + 10, COLOR_MEMBER_LABEL);

        // 绘制组员列表
        renderMemberList(graphics, rightPanelX + CONTENT_PADDING, rightContentY,
                rightPanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH, rightContentHeight, mouseX, mouseY);

        // 绘制建议列表（最后渲染，确保在最上层）
        if (showHeaderSuggestions && !headerSuggestions.isEmpty()) {
            renderSuggestions(graphics, addHeaderBox.getX(), addHeaderBox.getY() + addHeaderBox.getHeight(),
                    addHeaderBox.getWidth(), headerSuggestions, mouseX, mouseY, true);
        }
        if (showMemberSuggestions && !memberSuggestions.isEmpty()) {
            renderSuggestions(graphics, addMemberBox.getX(), addMemberBox.getY() + addMemberBox.getHeight(),
                    addMemberBox.getWidth(), memberSuggestions, mouseX, mouseY, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderGroupList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        graphics.enableScissor(x, y, x + width + SCROLLBAR_WIDTH, y + height);

        int totalHeight = groupNames.size() * GROUP_ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - height);
        groupScrollOffset = Math.max(0, Math.min(groupScrollOffset, maxScroll));

        int startY = y - (int) groupScrollOffset;

        for (int i = 0; i < groupNames.size(); i++) {
            String groupName = groupNames.get(i);
            int itemY = startY + i * GROUP_ITEM_HEIGHT;

            if (itemY + GROUP_ITEM_HEIGHT < y || itemY > y + height) continue;

            boolean isSelected = groupName.equals(selectedGroup);
            boolean isHovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= itemY && mouseY < itemY + GROUP_ITEM_HEIGHT;

            int bgColor = isSelected ? COLOR_ITEM_BG_SELECTED :
                    (isHovered ? COLOR_ITEM_BG_HOVER : COLOR_ITEM_BG);
            graphics.fill(x, itemY, x + width, itemY + GROUP_ITEM_HEIGHT - 2, bgColor);

            // 组名
            graphics.drawString(nn(this.font), safeString(groupName), x + 5, itemY + 8, COLOR_TEXT);
        }

        graphics.disableScissor();

        // 滚动条
        if (totalHeight > height) {
            int scrollbarHeight = Math.max(20, height * height / totalHeight);
            int scrollbarY = y + (int) (groupScrollOffset * (height - scrollbarHeight) / maxScroll);
            graphics.fill(x + width, y, x + width + SCROLLBAR_WIDTH, y + height, COLOR_SCROLLBAR_BG);
            graphics.fill(x + width, scrollbarY, x + width + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, COLOR_SCROLLBAR_THUMB);
        }
    }

    private void renderHeaderList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        graphics.enableScissor(x, y, x + width + SCROLLBAR_WIDTH, y + height);

        List<String> headers = selectedGroup != null && groupNames.contains(selectedGroup)
                ? groupHeaders.getOrDefault(selectedGroup, new ArrayList<>())
                : new ArrayList<>();

        int totalHeight = headers.size() * HEADER_ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - height);
        headerScrollOffset = Math.max(0, Math.min(headerScrollOffset, maxScroll));

        int startY = y - (int) headerScrollOffset;

        for (int i = 0; i < headers.size(); i++) {
            String headerId = headers.get(i);
            int itemY = startY + i * HEADER_ITEM_HEIGHT;

            if (itemY + HEADER_ITEM_HEIGHT < y || itemY > y + height) continue;

            boolean isHovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= itemY && mouseY < itemY + HEADER_ITEM_HEIGHT;

            int bgColor = isHovered ? COLOR_HEADER_BG_HOVER : COLOR_HEADER_BG;
            graphics.fill(x, itemY, x + width - 20, itemY + HEADER_ITEM_HEIGHT - 2, bgColor);

            // 图标
            renderItemIcon(graphics, safeString(headerId), x + 2, itemY + 2);

            // 名称
            String displayName = getDisplayName(safeString(headerId));
            String truncated = truncateString(displayName, width - 50, "...");
            graphics.drawString(nn(this.font), safeString(truncated), x + 22, itemY + 6, COLOR_TEXT);

            // 删除按钮
            int deleteX = x + width - 18;
            boolean deleteHovered = mouseX >= deleteX && mouseX <= deleteX + 16 &&
                    mouseY >= itemY && mouseY < itemY + HEADER_ITEM_HEIGHT - 2;
            int deleteColor = deleteHovered ? 0xFFFF7777 : COLOR_DELETE_BUTTON;
            graphics.fill(deleteX, itemY, deleteX + 16, itemY + HEADER_ITEM_HEIGHT - 2, deleteColor);
            graphics.drawCenteredString(nn(this.font), "x", deleteX + 8, itemY + 5, COLOR_TEXT);
        }

        graphics.disableScissor();

        // 滚动条
        if (totalHeight > height) {
            int scrollbarHeight = Math.max(20, height * height / totalHeight);
            int scrollbarY = y + (int) (headerScrollOffset * (height - scrollbarHeight) / maxScroll);
            graphics.fill(x + width, y, x + width + SCROLLBAR_WIDTH, y + height, COLOR_SCROLLBAR_BG);
            graphics.fill(x + width, scrollbarY, x + width + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, COLOR_SCROLLBAR_THUMB);
        }
    }

    private void renderMemberList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        graphics.enableScissor(x, y, x + width + SCROLLBAR_WIDTH, y + height);

        List<String> members = selectedGroup != null && groupNames.contains(selectedGroup)
                ? groupMembers.getOrDefault(selectedGroup, new ArrayList<>())
                : new ArrayList<>();

        int totalHeight = members.size() * MEMBER_ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - height);
        memberScrollOffset = Math.max(0, Math.min(memberScrollOffset, maxScroll));

        int startY = y - (int) memberScrollOffset;

        for (int i = 0; i < members.size(); i++) {
            String memberId = members.get(i);
            int itemY = startY + i * MEMBER_ITEM_HEIGHT;

            if (itemY + MEMBER_ITEM_HEIGHT < y || itemY > y + height) continue;

            boolean isHovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= itemY && mouseY < itemY + MEMBER_ITEM_HEIGHT;

            int bgColor = isHovered ? COLOR_MEMBER_BG_HOVER : COLOR_MEMBER_BG;
            graphics.fill(x, itemY, x + width - 20, itemY + MEMBER_ITEM_HEIGHT - 2, bgColor);

            // 图标
            renderItemIcon(graphics, safeString(memberId), x + 2, itemY + 2);

            // 名称
            String displayName = getDisplayName(safeString(memberId));
            String truncated = truncateString(displayName, width - 50, "...");
            graphics.drawString(nn(this.font), safeString(truncated), x + 22, itemY + 6, COLOR_TEXT);

            // 删除按钮
            int deleteX = x + width - 18;
            boolean deleteHovered = mouseX >= deleteX && mouseX <= deleteX + 16 &&
                    mouseY >= itemY && mouseY < itemY + MEMBER_ITEM_HEIGHT - 2;
            int deleteColor = deleteHovered ? 0xFFFF7777 : COLOR_DELETE_BUTTON;
            graphics.fill(deleteX, itemY, deleteX + 16, itemY + MEMBER_ITEM_HEIGHT - 2, deleteColor);
            graphics.drawCenteredString(nn(this.font), "x", deleteX + 8, itemY + 5, COLOR_TEXT);
        }

        graphics.disableScissor();

        // 滚动条
        if (totalHeight > height) {
            int scrollbarHeight = Math.max(20, height * height / totalHeight);
            int scrollbarY = y + (int) (memberScrollOffset * (height - scrollbarHeight) / maxScroll);
            graphics.fill(x + width, y, x + width + SCROLLBAR_WIDTH, y + height, COLOR_SCROLLBAR_BG);
            graphics.fill(x + width, scrollbarY, x + width + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, COLOR_SCROLLBAR_THUMB);
        }
    }

    private void renderSuggestions(GuiGraphics graphics, int x, int y, int width,
                                   List<String> suggestions, int mouseX, int mouseY, boolean isHeader) {
        int itemHeight = 20;
        int maxDisplay = Math.min(suggestions.size(), 8);
        int height = maxDisplay * itemHeight;

        // 保存当前矩阵状态并提高Z层（使用更高的Z值确保在最上层）
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);

        // 绘制多层背景（防止透字）
        // 第一层：扩展的黑色遮罩层
        graphics.fill(x - 5, y - 3, x + width + 5, y + height + 3, 0xFF000000);
        // 第二层：主背景层
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xFF000000);
        // 第三层：内层背景
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xFF111111);
        // 第四层：内容区域背景
        graphics.fill(x, y, x + width, y + height, 0xFF444444);

        for (int i = 0; i < maxDisplay; i++) {
            String suggestion = suggestions.get(i);
            int itemY = y + i * itemHeight;

            boolean isHovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= itemY && mouseY < itemY + itemHeight;

            int bgColor = isHovered ? 0xFF666666 : 0xFF444444;
            graphics.fill(x, itemY, x + width, itemY + itemHeight, bgColor);

            // 图标
            renderItemIcon(graphics, safeString(suggestion), x + 2, itemY + 2);

            // 名称
            String displayName = getDisplayName(safeString(suggestion));
            graphics.drawString(nn(this.font), safeString(displayName), x + 22, itemY + 6, COLOR_TEXT);
        }

        // 恢复矩阵状态
        graphics.pose().popPose();
    }

    private void renderItemIcon(GuiGraphics graphics, String itemId, int x, int y) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(safeString(itemId));
            if (location == null) return;

            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null && item != Items.AIR) {
                    graphics.renderItem(nn(new ItemStack(item)), x, y);
            } else {
                Block block = ForgeRegistries.BLOCKS.getValue(location);
                if (block != null && block != Blocks.AIR) {
                    graphics.renderItem(nn(new ItemStack(block)), x, y);
                }
            }
        } catch (Exception e) {
            // 忽略渲染错误
        }
    }

    @Nonnull
    private String getDisplayName(@Nonnull String itemId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(safeString(itemId));
            if (location == null) return itemId;

            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null && item != Items.AIR) {
                return safeString(item.getDescription().getString());
            }

            Block block = ForgeRegistries.BLOCKS.getValue(location);
            if (block != null && block != Blocks.AIR) {
                return safeString(block.getName().getString());
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return safeString(itemId);
    }

    @Nonnull
    private String truncateString(@Nonnull String str, int maxWidth, @Nonnull String suffix) {
        if (nn(this.font).width(str) <= maxWidth) {
            return safeString(str);
        }

        String result = safeString(str);
        while (nn(this.font).width(result + suffix) > maxWidth && !result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }
        return safeString(result + suffix);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int panelWidth = Math.min(800, this.width - PANEL_MARGIN_X * 2);
        int leftPanelWidth = (panelWidth - 40) / 3;
        int middlePanelWidth = (panelWidth - 40) / 3;
        int rightPanelWidth = panelWidth - 40 - leftPanelWidth - middlePanelWidth;
        int panelStartX = centerX - panelWidth / 2;
        int panelTop = PANEL_MARGIN_TOP;
        int panelBottom = this.height - PANEL_MARGIN_BOTTOM;
        int contentHeight = panelBottom - panelTop;

        // 左侧面板 - 组列表
        int leftPanelX = panelStartX;
        int leftContentY = panelTop + 40;
        int leftContentHeight = contentHeight - 90;
        int leftContentWidth = leftPanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH;

        // 检查是否点击了建议列表（优先处理）
        if (showHeaderSuggestions && !headerSuggestions.isEmpty()) {
            int suggestionX = addHeaderBox.getX();
            int suggestionY = addHeaderBox.getY() + addHeaderBox.getHeight();
            int suggestionWidth = addHeaderBox.getWidth();
            int itemHeight = 20;
            int maxDisplay = Math.min(headerSuggestions.size(), 8);
            int suggestionHeight = maxDisplay * itemHeight;

            if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth &&
                    mouseY >= suggestionY && mouseY <= suggestionY + suggestionHeight) {
                int index = (int) ((mouseY - suggestionY) / itemHeight);
                if (index >= 0 && index < headerSuggestions.size()) {
                    addHeaderBox.setValue(safeString(headerSuggestions.get(index)));
                    showHeaderSuggestions = false;
                    return true;
                }
            }
            showHeaderSuggestions = false;
        }

        if (showMemberSuggestions && !memberSuggestions.isEmpty()) {
            int suggestionX = addMemberBox.getX();
            int suggestionY = addMemberBox.getY() + addMemberBox.getHeight();
            int suggestionWidth = addMemberBox.getWidth();
            int itemHeight = 20;
            int maxDisplay = Math.min(memberSuggestions.size(), 8);
            int suggestionHeight = maxDisplay * itemHeight;

            if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth &&
                    mouseY >= suggestionY && mouseY <= suggestionY + suggestionHeight) {
                int index = (int) ((mouseY - suggestionY) / itemHeight);
                if (index >= 0 && index < memberSuggestions.size()) {
                    addMemberBox.setValue(safeString(memberSuggestions.get(index)));
                    showMemberSuggestions = false;
                    return true;
                }
            }
            showMemberSuggestions = false;
        }

        // 检查组列表点击
        if (mouseX >= leftPanelX + CONTENT_PADDING &&
                mouseX <= leftPanelX + CONTENT_PADDING + leftContentWidth &&
                mouseY >= leftContentY && mouseY <= leftContentY + leftContentHeight) {

            int totalHeight = groupNames.size() * GROUP_ITEM_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - leftContentHeight);
            int relativeY = (int) (mouseY - leftContentY + groupScrollOffset);
            int index = relativeY / GROUP_ITEM_HEIGHT;

            if (index >= 0 && index < groupNames.size()) {
                selectedGroup = groupNames.get(index);
                headerScrollOffset = 0;
                memberScrollOffset = 0;
                updateUIState();
                return true;
            }
        }

        // 检查组头列表点击
        int middlePanelX = leftPanelX + leftPanelWidth + 20;
        int middleContentY = panelTop + 40;
        int middleContentHeight = (contentHeight - 100) / 2;
        int middleContentWidth = middlePanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH;

        if (selectedGroup != null && groupNames.contains(selectedGroup)) {
            List<String> headers = groupHeaders.getOrDefault(selectedGroup, new ArrayList<>());

            if (mouseX >= middlePanelX + CONTENT_PADDING &&
                    mouseX <= middlePanelX + CONTENT_PADDING + middleContentWidth &&
                    mouseY >= middleContentY && mouseY <= middleContentY + middleContentHeight) {

                int totalHeight = headers.size() * HEADER_ITEM_HEIGHT;
                int maxScroll = Math.max(0, totalHeight - middleContentHeight);
                int relativeY = (int) (mouseY - middleContentY + headerScrollOffset);
                int index = relativeY / HEADER_ITEM_HEIGHT;

                // 检查是否点击了删除按钮
                int deleteX = middlePanelX + CONTENT_PADDING + middleContentWidth - 18;
                if (index >= 0 && index < headers.size() &&
                        mouseX >= deleteX && mouseX <= deleteX + 16) {
                    removeHeaderFromGroup(headers.get(index));
                    return true;
                }
            }
        }

        // 检查组员列表点击
        int rightPanelX = middlePanelX + middlePanelWidth + 20;
        int rightContentY = panelTop + 40;
        int rightContentHeight = (contentHeight - 100) / 2;
        int rightContentWidth = rightPanelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH;

        if (selectedGroup != null && groupNames.contains(selectedGroup)) {
            List<String> members = groupMembers.getOrDefault(selectedGroup, new ArrayList<>());

            if (mouseX >= rightPanelX + CONTENT_PADDING &&
                    mouseX <= rightPanelX + CONTENT_PADDING + rightContentWidth &&
                    mouseY >= rightContentY && mouseY <= rightContentY + rightContentHeight) {

                int totalHeight = members.size() * MEMBER_ITEM_HEIGHT;
                int maxScroll = Math.max(0, totalHeight - rightContentHeight);
                int relativeY = (int) (mouseY - rightContentY + memberScrollOffset);
                int index = relativeY / MEMBER_ITEM_HEIGHT;

                // 检查是否点击了删除按钮
                int deleteX = rightPanelX + CONTENT_PADDING + rightContentWidth - 18;
                if (index >= 0 && index < members.size() &&
                        mouseX >= deleteX && mouseX <= deleteX + 16) {
                    removeMemberFromGroup(members.get(index));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int centerX = this.width / 2;
        int panelWidth = Math.min(800, this.width - PANEL_MARGIN_X * 2);
        int leftPanelWidth = (panelWidth - 40) / 3;
        int middlePanelWidth = (panelWidth - 40) / 3;
        int panelStartX = centerX - panelWidth / 2;
        int panelTop = PANEL_MARGIN_TOP;
        int panelBottom = this.height - PANEL_MARGIN_BOTTOM;
        int contentHeight = panelBottom - panelTop;

        // 左侧面板
        int leftPanelX = panelStartX;
        int leftContentY = panelTop + 40;
        int leftContentHeight = contentHeight - 90;

        if (mouseX >= leftPanelX && mouseX <= leftPanelX + leftPanelWidth &&
                mouseY >= leftContentY && mouseY <= leftContentY + leftContentHeight) {
            groupScrollOffset -= delta * 30;
            return true;
        }

        // 中间面板
        int middlePanelX = leftPanelX + leftPanelWidth + 20;
        int middleContentY = panelTop + 40;
        int middleContentHeight = (contentHeight - 100) / 2;

        if (mouseX >= middlePanelX && mouseX <= middlePanelX + middlePanelWidth &&
                mouseY >= middleContentY && mouseY <= middleContentY + middleContentHeight) {
            headerScrollOffset -= delta * 30;
            return true;
        }

        // 右侧面板
        int rightPanelX = middlePanelX + middlePanelWidth + 20;
        int rightContentY = panelTop + 40;
        int rightContentHeight = (contentHeight - 100) / 2;
        int rightPanelWidth = this.width - PANEL_MARGIN_X * 2 - leftPanelWidth - middlePanelWidth - 40;

        if (mouseX >= rightPanelX && mouseX <= rightPanelX + rightPanelWidth &&
                mouseY >= rightContentY && mouseY <= rightContentY + rightContentHeight) {
            memberScrollOffset -= delta * 30;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean result = super.charTyped(codePoint, modifiers);
        updateSuggestions();
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addHeaderBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                addHeaderToGroup();
                return true;
            }
        }

        if (addMemberBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                addMemberToGroup();
                return true;
            }
        }

        if (newGroupNameBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                addNewGroup();
                return true;
            }
        }

        // Ctrl+S 保存
        if (keyCode == 83 && (modifiers & 2) != 0) {
            saveAndClose();
            return true;
        }

        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        updateSuggestions();
        return result;
    }

    private void updateSuggestions() {
        // 更新组头建议
        if (addHeaderBox.isFocused()) {
            String input = addHeaderBox.getValue().toLowerCase();
            if (input.length() >= 1) {
                headerSuggestions = ForgeRegistries.ITEMS.getKeys().stream()
                        .map(Object::toString)
                        .filter(id -> id.toLowerCase().contains(input))
                        .limit(50)
                        .collect(Collectors.toList());

                if (headerSuggestions.isEmpty()) {
                    headerSuggestions = ForgeRegistries.BLOCKS.getKeys().stream()
                            .map(Object::toString)
                            .filter(id -> id.toLowerCase().contains(input))
                            .limit(50)
                            .collect(Collectors.toList());
                }
                showHeaderSuggestions = !headerSuggestions.isEmpty();
            } else {
                showHeaderSuggestions = false;
            }
        } else {
            showHeaderSuggestions = false;
        }

        // 更新组员建议
        if (addMemberBox.isFocused()) {
            String input = addMemberBox.getValue().toLowerCase();
            if (input.length() >= 1) {
                memberSuggestions = ForgeRegistries.ITEMS.getKeys().stream()
                        .map(Object::toString)
                        .filter(id -> id.toLowerCase().contains(input))
                        .limit(50)
                        .collect(Collectors.toList());

                if (memberSuggestions.isEmpty()) {
                    memberSuggestions = ForgeRegistries.BLOCKS.getKeys().stream()
                            .map(Object::toString)
                            .filter(id -> id.toLowerCase().contains(input))
                            .limit(50)
                            .collect(Collectors.toList());
                }
                showMemberSuggestions = !memberSuggestions.isEmpty();
            } else {
                showMemberSuggestions = false;
            }
        } else {
            showMemberSuggestions = false;
        }
    }

    /**
     * 创建通类匹配组配置界面
     */
    public static MaterialCategoryGroupsScreen createScreen(Screen parent, Runnable onSaveCallback) {
        return new MaterialCategoryGroupsScreen(
                parent,
                ServerConfig.parseMaterialCategoryGroups(),
                items -> {
                    ServerConfig.setMaterialCategoryGroups(items);
                    ServerConfig.SPEC.save();
                    if (onSaveCallback != null) {
                        onSaveCallback.run();
                    }
                }
        );
    }
}
