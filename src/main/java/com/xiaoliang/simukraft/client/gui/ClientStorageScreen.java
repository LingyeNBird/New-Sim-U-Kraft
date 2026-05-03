package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.ClientStorageRequestPacket;
import com.xiaoliang.simukraft.network.ClientStorageResponsePacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 客户端储量信息界面 — 显示当前客户端绑定的所有容器内的物品。
 */
public class ClientStorageScreen extends AbstractTransitionScreen
        implements ClientStorageResponsePacket.ClientStorageScreenReceiver {

    private static final int GRID_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = GRID_COLS * SLOT_SIZE + 16;
    private static final int PANEL_HEIGHT = 200;

    private final BlockPos blockPos;
    private final List<BlockPos> containerPositions;

    private List<AggregatedItem> allItems = new ArrayList<>();
    private List<AggregatedItem> filteredItems = new ArrayList<>();

    // 服务端返回的统计信息
    private int emptyContainers = 0;

    private EditBox searchBox;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int hoveredSlot = -1;

    public ClientStorageScreen(BlockPos blockPos, List<BlockPos> containerPositions) {
        super(Component.translatable("gui.client_storage.title"));
        this.blockPos = blockPos;
        this.containerPositions = containerPositions;
    }

    @Override
    protected void init() {
        super.init();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        searchBox = nn(new EditBox(nn(this.font), panelX, panelY - 22, PANEL_WIDTH - 90, 18, nn(Component.translatable("gui.configurable_list.search_hint"))));
        searchBox.setHint(nn(Component.translatable("gui.configurable_list.search_hint")));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        addRenderableWidget(nn(searchBox));

        // 刷新按钮
        addRenderableWidget(nn(net.minecraft.client.gui.components.Button.builder(
                nn(Component.translatable("gui.client_storage.refresh")), btn -> refreshItems())
                .bounds(panelX + PANEL_WIDTH - 85, panelY - 24, 40, 20).build()));

        // 返回按钮
        addRenderableWidget(nn(net.minecraft.client.gui.components.Button.builder(
                nn(Component.translatable("gui.button.back")), btn -> this.onClose())
                .bounds(panelX + PANEL_WIDTH + 8, panelY, 40, 20).build()));

        refreshItems();
    }

    private void refreshItems() {
        // 始终通过网络包请求服务端数据
        NetworkManager.INSTANCE.sendToServer(new ClientStorageRequestPacket(blockPos));
    }

    @Override
    public void onClientStorageDataReceived(ClientStorageResponsePacket packet) {
        if (!packet.getClientBlockPos().equals(blockPos)) {
            return;
        }

        // 转换响应数据为 AggregatedItem
        List<AggregatedItem> result = new ArrayList<>();
        for (ClientStorageResponsePacket.ItemEntry entry : packet.getItems()) {
            ItemStack stack = entry.createItemStack();
            if (!stack.isEmpty()) {
                result.add(new AggregatedItem(stack, entry.count));
            }
        }

        allItems = result;
        emptyContainers = packet.getEmptyContainers();
        applyFilter();
    }

    private void onSearchChanged(String text) {
        applyFilter();
    }

    private void applyFilter() {
        String query = searchBox != null ? nn(searchBox).getValue().toLowerCase() : "";
        if (query.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = new ArrayList<>();
            for (AggregatedItem item : allItems) {
                String name = nn(item.stack.getHoverName()).getString().toLowerCase();
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(item.stack.getItem());
                String id = key != null ? key.toString().toLowerCase() : "";
                if (name.contains(query) || id.contains(query)) {
                    filteredItems.add(item);
                }
            }
        }
        int totalRows = (filteredItems.size() + GRID_COLS - 1) / GRID_COLS;
        int visibleRows = PANEL_HEIGHT / SLOT_SIZE;
        maxScrollOffset = Math.max(0, totalRows - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);
    }

    @Override
    protected void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        int a = (int) (getAlpha() * 200) & 0xFF;
        nn(guiGraphics).fill(0, 0, this.width, this.height, (a << 24) | 0x0A0A0A);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(nn(guiGraphics), mouseX, mouseY, partialTicks);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int a = (int) (getAlpha() * 255) & 0xFF;

        // 面板背景
        guiGraphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, (a << 24) | 0x1A1A2E);
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, (a << 24) | 0x16213E);

        // 标题 - 使用服务端返回的统计信息
        int containersWithItems = containerPositions.size() - emptyContainers;
        int validContainers = containerPositions.size();
        String titleText = "储量信息 (" + allItems.size() + " 种, " + containersWithItems + "/" + validContainers + " 个容器有物品)";
        guiGraphics.drawString(nn(this.font), safeString(titleText), panelX, panelY - 36, (a << 24) | 0xFFFFFF);

        // 物品网格
        hoveredSlot = -1;
        int visibleRows = PANEL_HEIGHT / SLOT_SIZE;
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int idx = (scrollOffset + row) * GRID_COLS + col;
                int slotX = panelX + 4 + col * SLOT_SIZE;
                int slotY = panelY + 4 + row * SLOT_SIZE;

                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, (a << 24) | 0x2A2A4A);

                if (idx < filteredItems.size()) {
                    AggregatedItem item = filteredItems.get(idx);
                    guiGraphics.renderItem(nn(item.stack), slotX, slotY);

                    String cnt = safeString(item.count > 999 ? (item.count / 1000) + "k" : String.valueOf(item.count));
                    guiGraphics.drawString(nn(this.font), safeString(cnt), slotX + 17 - nn(this.font).width(cnt), slotY + 9, (a << 24) | 0xFFFFFF);

                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        hoveredSlot = idx;
                        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x40FFFFFF);
                    }
                }
            }
        }

        // 滚动条
        if (maxScrollOffset > 0) {
            int sbX = panelX + PANEL_WIDTH - 6;
            int sbH = PANEL_HEIGHT - 8;
            int thumbH = Math.max(10, sbH * visibleRows / (maxScrollOffset + visibleRows));
            int thumbY = panelY + 4 + (int) ((sbH - thumbH) * ((float) scrollOffset / maxScrollOffset));
            guiGraphics.fill(sbX, panelY + 4, sbX + 4, panelY + PANEL_HEIGHT - 4, (a << 24) | 0x333333);
            guiGraphics.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, (a << 24) | 0x888888);
        }

        // 悬停提示
        if (hoveredSlot >= 0 && hoveredSlot < filteredItems.size()) {
            AggregatedItem item = filteredItems.get(hoveredSlot);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(nn(item.stack.getHoverName()));
            tooltip.add(nn(Component.literal("§7数量: " + item.count)));
            guiGraphics.renderTooltip(nn(this.font), nn(tooltip), nn(Optional.empty()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - (int) delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(String value) {
        return nn(value);
    }

    private static class AggregatedItem {
        final ItemStack stack;
        int count;
        AggregatedItem(ItemStack stack, int count) {
            this.stack = stack;
            this.stack.setCount(1);
            this.count = count;
        }
    }
}
