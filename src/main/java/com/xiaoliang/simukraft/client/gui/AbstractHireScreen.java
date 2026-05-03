package com.xiaoliang.simukraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.NPCListPacket;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import com.xiaoliang.simukraft.utils.SkinManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractHireScreen extends AbstractTransitionScreen {
    @Nonnull
    protected static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Button requireButton(@Nullable Button value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Component requireComponent(@Nullable Component value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Font requireFont(@Nullable Font value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String requireString(@Nullable String value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static ItemStack requireItemStack(@Nullable ItemStack value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static ItemLike requireItemLike(@Nullable Item value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static ResourceLocation requireResourceLocation(@Nullable ResourceLocation value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    protected static Minecraft minecraft() {
        return nn(Minecraft.getInstance());
    }

    @Nonnull
    protected static LocalPlayer player() {
        return nn(minecraft().player);
    }

    @Nonnull
    protected static ClientLevel level() {
        return nn(minecraft().level);
    }

    // 网格布局常量
    protected static final int NPC_PER_PAGE = 6;
    protected static final int COLUMNS = 3;
    protected static final int BUTTON_WIDTH = 180;
    protected static final int BUTTON_HEIGHT = 80;
    protected static final int BUTTON_SPACING = 10;
    protected static final int TOP_MARGIN = 60;
    protected static final int BOTTOM_MARGIN = 50;

    protected int currentPage = 0;
    protected int totalPages = 0;

    protected final List<NPCInfo> npcList = new ArrayList<>();
    protected UUID selectedNPCId = null;
    protected final List<NPCButton> npcButtons = new ArrayList<>();
    protected Button confirmButton;
    protected Button prevPageButton;
    protected Button nextPageButton;
    protected Button backButton;
    protected Component statusText = Component.translatable("message.simukraft.loading_npcs");

    // NPC信息记录类
    public record NPCInfo(UUID uuid, String name, String skinPath, int level, int xp) {
        public static NPCInfo fromPacket(NPCListPacket.NPCInfo packetInfo) {
            return new NPCInfo(packetInfo.uuid(), packetInfo.name(), packetInfo.skinPath(), packetInfo.level(), packetInfo.xp());
        }
    }

    protected AbstractHireScreen(Component title) {
        super(title);
    }

    protected List<NPCInfo> getCurrentPageNpcs() {
        int startIndex = currentPage * NPC_PER_PAGE;
        int endIndex = Math.min(startIndex + NPC_PER_PAGE, npcList.size());
        return npcList.subList(startIndex, endIndex);
    }

    protected void updatePageButtons() {
        if (prevPageButton != null) {
            prevPageButton.active = currentPage > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = currentPage < totalPages - 1;
        }
    }

    protected void clearNPCButtons() {
        for (NPCButton button : npcButtons) {
            this.removeWidget(Objects.requireNonNull(button));
        }
        npcButtons.clear();
    }

    protected void onPageChanged() {
        clearNPCButtons();
        createNPCButtons();
        updatePageButtons();
    }

    protected abstract void confirmSelection();

    public void updateNPCList(Map<UUID, String> newNpcMap) {
        // 兼容旧版本，只更新UUID和名字，等级默认为1，经验值为0
        this.npcList.clear();
        for (Map.Entry<UUID, String> entry : newNpcMap.entrySet()) {
            npcList.add(new NPCInfo(entry.getKey(), entry.getValue(), "", 1, 0));
        }
        this.selectedNPCId = null;
        this.currentPage = 0;
        this.totalPages = (int) Math.ceil((double) npcList.size() / NPC_PER_PAGE);

        if (npcList.isEmpty()) {
            this.statusText = Component.translatable("message.simukraft.no_idle_npcs").withStyle(style -> style.withColor(0xFF5555));
        } else {
            this.statusText = Component.translatable("gui.select_npc.title", npcList.size(), (currentPage + 1), totalPages)
                    .withStyle(style -> style.withColor(0x55FF55));
        }

        onPageChanged();

        if (confirmButton != null) {
            confirmButton.active = !npcList.isEmpty();
        }
    }

    public void updateNPCList(List<NPCListPacket.NPCInfo> newNpcList) {
        // 新版本，包含完整信息
        this.npcList.clear();
        for (NPCListPacket.NPCInfo info : newNpcList) {
            npcList.add(NPCInfo.fromPacket(info));
        }
        this.selectedNPCId = null;
        this.currentPage = 0;
        this.totalPages = (int) Math.ceil((double) npcList.size() / NPC_PER_PAGE);

        if (npcList.isEmpty()) {
            this.statusText = Component.translatable("message.simukraft.no_idle_npcs").withStyle(style -> style.withColor(0xFF5555));
        } else {
            this.statusText = Component.translatable("gui.select_npc.title", npcList.size(), (currentPage + 1), totalPages)
                    .withStyle(style -> style.withColor(0x55FF55));
        }

        onPageChanged();

        if (confirmButton != null) {
            confirmButton.active = !npcList.isEmpty();
        }
    }
    
    public void refreshNPCList() {
        // 重新请求NPC列表数据
        System.out.println("[AbstractHireScreen] 刷新NPC列表");
        
        // 如果是空闲NPC雇佣屏幕，重新请求空闲NPC列表
        if (this.getClass().getSimpleName().contains("Idle")) {
            com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(
                new com.xiaoliang.simukraft.network.RequestIdleNPCsPacket()
            );
        } else {
            // 否则重新请求完整NPC列表
            // NPCListPacket需要参数，我们使用一个空的Map来请求
            com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(
                new com.xiaoliang.simukraft.network.NPCListPacket(new java.util.HashMap<>())
            );
        }
    }

    protected void createNPCButtons() {
        List<NPCInfo> currentPageNpcs = getCurrentPageNpcs();

        // 自适应缩放计算
        int availableWidth = this.width - 40;
        int availableHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN - 50;

        // 计算最大可能的按钮尺寸
        int maxButtonWidth = (availableWidth - (COLUMNS - 1) * BUTTON_SPACING) / COLUMNS;
        int rows = (int) Math.ceil((double) currentPageNpcs.size() / COLUMNS);
        int maxButtonHeight = rows > 0 ? (availableHeight - (rows - 1) * BUTTON_SPACING) / rows : BUTTON_HEIGHT;

        // 使用计算出的尺寸，但不小于最小值
        int actualButtonWidth = Math.max(140, Math.min(BUTTON_WIDTH, maxButtonWidth));
        int actualButtonHeight = Math.max(70, Math.min(BUTTON_HEIGHT, maxButtonHeight));

        // 计算网格布局
        int totalWidth = COLUMNS * actualButtonWidth + (COLUMNS - 1) * BUTTON_SPACING;
        int totalHeight = rows * actualButtonHeight + (rows - 1) * BUTTON_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int startY = TOP_MARGIN + (availableHeight - totalHeight) / 2;
        startY = Math.max(TOP_MARGIN, startY);

        for (int i = 0; i < currentPageNpcs.size(); i++) {
            NPCInfo npc = currentPageNpcs.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;

            int x = startX + col * (actualButtonWidth + BUTTON_SPACING);
            int y = startY + row * (actualButtonHeight + BUTTON_SPACING);

            NPCButton button = new NPCButton(x, y, actualButtonWidth, actualButtonHeight, npc, btn -> selectNPC(npc.uuid()));

            if (npc.uuid().equals(selectedNPCId)) {
                button.setSelected(true);
            }

            npcButtons.add(button);
            addRenderableWidget(button);
        }
    }

    protected void selectNPC(UUID npcId) {
        this.selectedNPCId = npcId;

        for (NPCButton button : npcButtons) {
            button.setSelected(button.getNPCInfo().uuid().equals(npcId));
        }

        if (confirmButton != null) {
            confirmButton.active = true;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);

        guiGraphics.drawCenteredString(
                Objects.requireNonNull(font),
                Objects.requireNonNull(title),
                width / 2,
                25,
                0xFFFFFF);

        guiGraphics.drawCenteredString(
                Objects.requireNonNull(font),
                requireComponent(statusText),
                width / 2,
                45,
                0xFFFFFF);

        if (totalPages > 1) {
            guiGraphics.drawCenteredString(
                    Objects.requireNonNull(font),
                    requireComponent(Component.translatable("gui.pagination.info", (currentPage + 1), totalPages)),
                    width / 2,
                    height - 40,
                    0xAAAAAA);
        }

        if (selectedNPCId != null) {
            String selectedName = getNPCName(selectedNPCId);
            guiGraphics.drawCenteredString(
                    Objects.requireNonNull(font),
                    requireComponent(Component.translatable("gui.select_npc.selected", selectedName).withStyle(style -> style.withColor(0xFFFF00))),
                    width / 2,
                    height - 60,
                    0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private String getNPCName(UUID uuid) {
        for (NPCInfo info : npcList) {
            if (info.uuid().equals(uuid)) {
                return info.name();
            }
        }
        return "";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        Button backButton = requireButton(Button.builder(
                        requireComponent(Component.translatable("gui.button.back")),
                        button -> onClose())
                .pos(5, 5)
                .size(45, 20)
                .build());
        this.addRenderableWidget(backButton);
        this.backButton = backButton;

        Button prevPageButton = requireButton(Button.builder(
                        requireComponent(Component.translatable("gui.pagination.previous")),
                        button -> {
                            if (currentPage > 0) {
                                currentPage--;
                                onPageChanged();
                            }
                        })
                .pos(width / 2 - 100, height - 30)
                .size(80, 20)
                .build());
        this.addRenderableWidget(prevPageButton);
        this.prevPageButton = prevPageButton;

        Button nextPageButton = requireButton(Button.builder(
                        requireComponent(Component.translatable("gui.pagination.next")),
                        button -> {
                            if (currentPage < totalPages - 1) {
                                currentPage++;
                                onPageChanged();
                            }
                        })
                .pos(width / 2 + 20, height - 30)
                .size(80, 20)
                .build());
        this.addRenderableWidget(nextPageButton);
        this.nextPageButton = nextPageButton;

        // 初始化按钮状态
        updatePageButtons();

        Button confirmButton = requireButton(Button.builder(
                        requireComponent(Component.translatable("gui.button.hire")),
                        button -> confirmSelection())
                .pos(width - 90, height - 30)
                .size(80, 20)
                .build());
        this.addRenderableWidget(confirmButton);
        this.confirmButton = confirmButton;

        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());
        this.statusText = Component.translatable("message.simukraft.loading_npcs");
    }

    // 自定义NPC按钮类
    private static class NPCButton extends Button {
        private final NPCInfo npcInfo;
        private boolean selected = false;
        private static final int HEAD_SIZE = 32;

        public NPCButton(int x, int y, int width, int height, NPCInfo npcInfo, OnPress onPress) {
            super(x, y, width, height, requireComponent(Component.literal(requireString(npcInfo.name()))), onPress, DEFAULT_NARRATION);
            this.npcInfo = npcInfo;
        }

        public NPCInfo getNPCInfo() {
            return npcInfo;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // 绘制按钮背景 - 70%透明度
            int color;
            if (this.isHovered) {
                color = 0xB3FFFFFF; // 悬停状态 (70%透明白色)
            } else {
                color = 0xB3000000; // 默认状态 (70%透明黑色)
            }
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

            // 绘制边框 - 选中状态为绿色，其他为白色
            int borderColor = selected ? 0xFF00FF00 : 0xFFFFFFFF;
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);

            // 计算头像位置（左侧）
            int headX = this.getX() + 5;
            int headY = this.getY() + (this.height - HEAD_SIZE) / 2;

            // 绘制NPC头像
            renderNpcHead(guiGraphics, headX, headY);

            // 文字起始X位置（头像右侧）
            int textStartX = this.getX() + HEAD_SIZE + 10;
            Font font = requireFont(Minecraft.getInstance().font);

            // NPC名称
            guiGraphics.drawString(
                font,
                requireComponent(Component.literal(requireString(npcInfo.name()))),
                textStartX,
                this.getY() + 8,
                0xFFFFFF
            );

            // 显示"空闲"状态
            guiGraphics.drawString(
                font,
                requireComponent(Component.translatable("work_status.idle")),
                textStartX,
                this.getY() + 28,
                0x55FF55
            );

            // 绘制经验条和等级（在按钮底部）
            renderExpBarAndLevel(guiGraphics);
        }

        /**
         * 绘制经验条和等级 - 圆角风格
         */
        private void renderExpBarAndLevel(GuiGraphics guiGraphics) {
            int level = npcInfo.level();
            int xp = npcInfo.xp();
            Font font = requireFont(Minecraft.getInstance().font);

            // 计算下一级所需经验值
            int xpForNextLevel = getXpForNextLevel(level);
            int xpForCurrentLevel = getXpForCurrentLevel(level);

            // 经验条位置和尺寸
            int barX = this.getX() + 8;
            int barY = this.getY() + this.height - 20;
            int barWidth = this.width - 50; // 留出空间给等级物品
            int barHeight = 12;
            int cornerRadius = 3;

            // 检查是否已满级（7级且经验达到2850，或超过7级）
            boolean isMaxLevel = xpForNextLevel < 0 || (level == 7 && xp >= 2850);

            // 计算经验条进度
            float progress;
            String expText;
            if (isMaxLevel) {
                // 满级状态：经验条全满，显示 MAX
                progress = 1.0f;
                expText = "MAX";
            } else {
                int xpInCurrentLevel = xp - xpForCurrentLevel;
                int xpNeeded = xpForNextLevel - xpForCurrentLevel;
                progress = xpNeeded > 0 ? (float) xpInCurrentLevel / xpNeeded : 1.0f;
                expText = xpInCurrentLevel + "/" + xpNeeded;
            }
            int filledWidth = (int) ((barWidth - 2) * progress);

            // 绘制圆角背景（深灰色）
            int bgColor = 0xFF3D3D3D;
            renderRoundedRect(guiGraphics, barX, barY, barWidth, barHeight, cornerRadius, bgColor);

            // 绘制圆角填充（灰色）- 满级时使用金色
            if (filledWidth > 0) {
                int fillColor = isMaxLevel ? 0xFFFFD700 : 0xFF808080; // 满级金色，普通灰色
                renderRoundedRect(guiGraphics, barX + 1, barY + 1, filledWidth, barHeight - 2, cornerRadius - 1, fillColor);
            }

            // 绘制经验值文字（居中）- 白色文字+黑色阴影
            int textX = barX + (barWidth - font.width(expText)) / 2;
            int textY = barY + 2;
            // 黑色阴影 + 白色文字
            guiGraphics.drawString(font, requireString(expText), textX + 1, textY + 1, 0xFF000000);
            guiGraphics.drawString(font, requireString(expText), textX, textY, 0xFFFFFFFF);

            // 绘制等级物品（右侧）
            int itemX = this.getX() + this.width - 28;
            int itemY = this.getY() + this.height - 22;
            ItemStack levelItem = requireItemStack(getLevelItem(level));
            guiGraphics.renderItem(levelItem, itemX, itemY);

            // 绘制等级文字（物品上方，带阴影）
            String levelText = "Lv " + level;
            int levelTextX = itemX + 8 - font.width(levelText) / 2;
            int levelTextY = itemY - 10;
            guiGraphics.drawString(font, requireString(levelText), levelTextX + 1, levelTextY + 1, 0xFF000000);
            guiGraphics.drawString(font, requireString(levelText), levelTextX, levelTextY, 0xFFFFFFFF);
        }

        /**
         * 绘制圆角矩形
         */
        private void renderRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
            // 主体矩形
            guiGraphics.fill(x + radius, y, x + width - radius, y + height, color);
            // 左侧矩形
            guiGraphics.fill(x, y + radius, x + radius, y + height - radius, color);
            // 右侧矩形
            guiGraphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
            // 四个角（使用小矩形模拟圆角）
            // 左上角
            guiGraphics.fill(x + radius - 1, y, x + radius, y + 1, color);
            guiGraphics.fill(x, y + radius - 1, x + 1, y + radius, color);
            // 右上角
            guiGraphics.fill(x + width - radius, y, x + width - radius + 1, y + 1, color);
            guiGraphics.fill(x + width - 1, y + radius - 1, x + width, y + radius, color);
            // 左下角
            guiGraphics.fill(x + radius - 1, y + height - 1, x + radius, y + height, color);
            guiGraphics.fill(x, y + height - radius, x + 1, y + height - radius + 1, color);
            // 右下角
            guiGraphics.fill(x + width - radius, y + height - 1, x + width - radius + 1, y + height, color);
            guiGraphics.fill(x + width - 1, y + height - radius, x + width, y + height - radius + 1, color);
        }

        /**
         * 获取当前等级所需经验值（累计值）
         * 与NPCDataManager.LEVEL_THRESHOLDS一致
         */
        private int getXpForCurrentLevel(int level) {
            return switch (level) {
                case 1 -> 0;
                case 2 -> 50;
                case 3 -> 150;
                case 4 -> 350;
                case 5 -> 650;
                case 6 -> 1150;
                case 7 -> 1850;
                default -> 2850;
            };
        }

        /**
         * 获取下一级所需经验值（累计值）
         * 7级满级返回-1表示已满级
         */
        private int getXpForNextLevel(int level) {
            return switch (level) {
                case 1 -> 50;
                case 2 -> 150;
                case 3 -> 350;
                case 4 -> 650;
                case 5 -> 1150;
                case 6 -> 1850;
                case 7 -> 2850;
                default -> -1; // 已满级
            };
        }

        /**
         * 根据等级获取对应的物品
         */
        private ItemStack getLevelItem(int level) {
            return switch (level) {
                case 1 -> new ItemStack(requireItemLike(Items.COAL));
                case 2 -> new ItemStack(requireItemLike(Items.COPPER_INGOT));
                case 3 -> new ItemStack(requireItemLike(Items.IRON_INGOT));
                case 4 -> new ItemStack(requireItemLike(Items.GOLD_INGOT));
                case 5 -> new ItemStack(requireItemLike(Items.DIAMOND));
                case 6 -> new ItemStack(requireItemLike(Items.ANCIENT_DEBRIS));
                case 7 -> new ItemStack(requireItemLike(Items.NETHERITE_INGOT));
                default -> new ItemStack(requireItemLike(Items.COAL));
            };
        }

        /**
         * 渲染NPC头像
         */
        private void renderNpcHead(GuiGraphics guiGraphics, int x, int y) {
            String skinPath = npcInfo.skinPath();
            if (skinPath == null || skinPath.isEmpty() || !SkinManager.isValidSkinPath(skinPath)) {
                // 如果没有皮肤路径或路径无效，绘制默认头像背景
                guiGraphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, 0xFF666666);
                guiGraphics.renderOutline(x, y, HEAD_SIZE, HEAD_SIZE, 0xFFFFFFFF);
                return;
            }

            try {
                // 使用SkinManager构建纹理资源位置
                ResourceLocation texture = requireResourceLocation(SkinManager.getTextureResourceLocation(skinPath));

                // 绑定并绘制纹理
                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();

                // 绘制头部底层
                guiGraphics.blit(texture, x, y, HEAD_SIZE, HEAD_SIZE,
                    8, 8, 8, 8, 64, 64);

                // 绘制头部帽子层
                guiGraphics.blit(texture, x, y, HEAD_SIZE, HEAD_SIZE,
                    40, 8, 8, 8, 64, 64);

                RenderSystem.disableBlend();

                // 绘制头像边框
                guiGraphics.renderOutline(x, y, HEAD_SIZE, HEAD_SIZE, 0xFFFFFFFF);
            } catch (Exception e) {
                // 如果纹理加载失败，绘制默认背景
                guiGraphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, 0xFF666666);
                guiGraphics.renderOutline(x, y, HEAD_SIZE, HEAD_SIZE, 0xFFFFFFFF);
            }
        }
    }
}
