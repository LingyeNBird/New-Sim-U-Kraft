package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 玩家选择对话框
 * 用于选择在线玩家
 */
public class PlayerSelectDialog extends Screen {
    
    private final Screen parent;
    private final Component title;
    private final Consumer<UUID> onSelect;
    private final Runnable onCancel;
    private final boolean allowSelf;
    
    private EditBox searchField;
    private Button cancelButton;
    
    private List<PlayerInfo> allPlayers = new ArrayList<>();
    private List<PlayerInfo> filteredPlayers = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 24;
    private static final int VISIBLE_ITEMS = 6;
    
    // 颜色主题 - 与 ChatScreen 保持一致
    private static final int COLOR_BACKGROUND = 0xCC000000;
    private static final int COLOR_PANEL_BG = 0xFF16213E;
    private static final int COLOR_ACCENT = 0xFF0F3460;
    private static final int COLOR_HIGHLIGHT = 0xFFE94560;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;
    
    private static final int DIALOG_WIDTH = 280;
    private static final int DIALOG_HEIGHT = 280;
    private static final int LIST_Y_OFFSET = 70;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;
    
    public PlayerSelectDialog(Screen parent, Component title, Consumer<UUID> onSelect, Runnable onCancel) {
        this(parent, title, onSelect, onCancel, false);
    }
    
    public PlayerSelectDialog(Screen parent, Component title, Consumer<UUID> onSelect, Runnable onCancel, boolean allowSelf) {
        super(title);
        this.parent = parent;
        this.title = title;
        this.onSelect = onSelect;
        this.onCancel = onCancel;
        this.allowSelf = allowSelf;
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
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        
        // 搜索框
        this.searchField = new EditBox(nn(this.font), dialogX + 20, dialogY + 45, DIALOG_WIDTH - 40, 18,
                nn(Component.translatable("chat.simukraft.create_group.search_hint")));
        this.searchField.setMaxLength(32);
        this.searchField.setResponder(this::onSearchChanged);
        this.addRenderableWidget(nn(this.searchField));
        
        // 取消按钮
        this.cancelButton = Button.builder(nn(Component.translatable("chat.simukraft.cancel")), btn -> {
            onCancel.run();
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        })
        .bounds(dialogX + (DIALOG_WIDTH - 70) / 2, dialogY + DIALOG_HEIGHT - 30, 70, 20)
        .build();
        this.addRenderableWidget(nn(this.cancelButton));
        
        // 加载玩家列表
        loadPlayers();
    }
    
    private void loadPlayers() {
        allPlayers.clear();
        
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        var connection = mc.getConnection();
        if (player != null && connection != null) {
            UUID selfId = player.getUUID();
            
            for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
                if (playerInfo.getProfile().getId().equals(selfId) && !allowSelf) {
                    continue; // 跳过自己（除非允许）
                }
                allPlayers.add(playerInfo);
            }
        }
        
        // 按名称排序
        allPlayers.sort(Comparator.comparing(p -> p.getProfile().getName()));
        filteredPlayers = new ArrayList<>(allPlayers);
    }
    
    private void onSearchChanged(String text) {
        String filter = text.toLowerCase();
        filteredPlayers.clear();
        
        for (PlayerInfo player : allPlayers) {
            if (player.getProfile().getName().toLowerCase().contains(filter)) {
                filteredPlayers.add(player);
            }
        }
        
        scrollOffset = 0;
    }
    
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        
        // 对话框背景
        guiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, COLOR_PANEL_BG);
        
        // 标题栏
        guiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + 35, COLOR_ACCENT);
        
        // 标题
        int titleWidth = nn(this.font).width(nn(title));
        guiGraphics.drawString(nn(this.font), nn(title), dialogX + (DIALOG_WIDTH - titleWidth) / 2, dialogY + 12, COLOR_TEXT_PRIMARY);
        
        // 玩家列表背景
        int listY = dialogY + LIST_Y_OFFSET;
        guiGraphics.fill(dialogX + 15, listY, dialogX + DIALOG_WIDTH - 15, listY + LIST_HEIGHT, 0xFF0A0A1A);
        
        // 渲染玩家列表
        for (int i = 0; i < VISIBLE_ITEMS && (i + scrollOffset) < filteredPlayers.size(); i++) {
            int index = i + scrollOffset;
            PlayerInfo player = filteredPlayers.get(index);
            int itemY = listY + i * ITEM_HEIGHT;
            
            renderPlayerItem(guiGraphics, dialogX + 15, itemY, DIALOG_WIDTH - 30, ITEM_HEIGHT, 
                    player, mouseX, mouseY);
        }
        
        // 渲染滚动条
        if (filteredPlayers.size() > VISIBLE_ITEMS) {
            renderScrollbar(guiGraphics, dialogX + DIALOG_WIDTH - 20, listY, 4, LIST_HEIGHT,
                    scrollOffset, filteredPlayers.size(), VISIBLE_ITEMS);
        }
        
        // 玩家数量提示
        String countText = safeString(filteredPlayers.size() + " " + Component.translatable("chat.simukraft.create_group.player_list").getString());
        guiGraphics.drawString(nn(this.font), countText, dialogX + 20, dialogY + DIALOG_HEIGHT - 55, COLOR_TEXT_SECONDARY);
        
        // 渲染组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void renderPlayerItem(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   PlayerInfo player, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        // 背景
        if (hovered) {
            guiGraphics.fill(x, y, x + width, y + height, COLOR_HIGHLIGHT);
        } else {
            guiGraphics.fill(x, y, x + width, y + height, (y / ITEM_HEIGHT) % 2 == 0 ? 0xFF1A1A2E : 0xFF16213E);
        }
        
        // 玩家头像
        renderPlayerHead(guiGraphics, x + 4, y + 4, 16, player);
        
        // 玩家名称
        String name = safeString(player.getProfile().getName());
        guiGraphics.drawString(nn(this.font), name, x + 26, y + 8, COLOR_TEXT_PRIMARY);
    }
    
    private void renderPlayerHead(GuiGraphics guiGraphics, int x, int y, int size, PlayerInfo playerInfo) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, nn(playerInfo.getSkinLocation()));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        
        // 头部底层
        guiGraphics.blit(nn(playerInfo.getSkinLocation()), x, y, size, size, 8, 8, 8, 8, 64, 64);
        
        // 帽子层
        int hatOffset = Math.max(1, size / 8);
        int hatSize = size + hatOffset * 2;
        guiGraphics.blit(nn(playerInfo.getSkinLocation()), x - hatOffset, y - hatOffset, hatSize, hatSize, 40, 8, 8, 8, 64, 64);
        
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    
    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                  int scrollOffset, int totalItems, int visibleItems) {
        // 滚动条背景
        guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);
        
        // 滚动条滑块
        float ratio = (float) visibleItems / totalItems;
        int thumbHeight = Math.max(20, (int) (height * ratio));
        int maxScroll = totalItems - visibleItems;
        int thumbY = maxScroll > 0 ? y + (int) ((float) scrollOffset / maxScroll * (height - thumbHeight)) : y;
        
        guiGraphics.fill(x, thumbY, x + width, thumbY + thumbHeight, COLOR_HIGHLIGHT);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        int listY = dialogY + LIST_Y_OFFSET;
        
        // 检查是否点击玩家列表
        if (mouseX >= dialogX + 15 && mouseX <= dialogX + DIALOG_WIDTH - 15 &&
            mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
            
            for (int i = 0; i < VISIBLE_ITEMS && (i + scrollOffset) < filteredPlayers.size(); i++) {
                int itemY = listY + i * ITEM_HEIGHT;
                if (mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
                    PlayerInfo player = filteredPlayers.get(i + scrollOffset);
                    onSelect.accept(player.getProfile().getId());
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;
        int listY = dialogY + LIST_Y_OFFSET;
        
        if (mouseX >= dialogX + 15 && mouseX <= dialogX + DIALOG_WIDTH - 15 &&
            mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
            
            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(Math.max(0, filteredPlayers.size() - VISIBLE_ITEMS), scrollOffset + 1);
            }
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            nn(cancelButton).onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
