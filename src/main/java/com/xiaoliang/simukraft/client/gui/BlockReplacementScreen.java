package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.ChestScanRequestPacket;
import com.xiaoliang.simukraft.network.CreatePlanningTaskPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 方块替换GUI
 * 简化版：左侧选择被替换方块，右侧选择替换方块，直接建立映射
 * 由NPC执行替换任务
 */
@SuppressWarnings("null")
public class BlockReplacementScreen extends Screen {
    private final BlockPos selectionStart;
    private final BlockPos selectionEnd;
    private final BlockPos chestPos;
    private final BlockPos buildBoxPos; // 建筑盒位置

    // 左侧：选区内的方块及其数量
    private Map<Block, Integer> selectionBlocks = new LinkedHashMap<>();
    // 右侧：箱子里的方块及其数量
    private static Map<Block, Integer> chestBlocks = new LinkedHashMap<>();
    // 替换映射：选区方块 -> 箱子方块
    private Map<Block, Block> replacementMap = new HashMap<>();

    // 滚动偏移
    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private static final int ITEM_HEIGHT = 28;

    // 选中的索引
    private int leftSelectedIndex = -1;
    private int rightSelectedIndex = -1;

    // 按钮
    private Button startButton;
    private Button clearButton;

    // 等待服务器响应的标志
    private boolean waitingForChestData = true;
    // menglannnn: 父界面引用，用于取消时返回
    private final Screen parentScreen;

    public BlockReplacementScreen(BlockPos selectionStart, BlockPos selectionEnd, BlockPos chestPos, BlockPos buildBoxPos) {
        this(selectionStart, selectionEnd, chestPos, buildBoxPos, null);
    }

    public BlockReplacementScreen(BlockPos selectionStart, BlockPos selectionEnd, BlockPos chestPos, BlockPos buildBoxPos, Screen parentScreen) {
        super(Component.translatable("gui.block_replacement.title"));
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.chestPos = chestPos;
        this.buildBoxPos = buildBoxPos;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        // 扫描选区
        scanSelection();

        // 清空之前的箱子数据
        chestBlocks.clear();
        waitingForChestData = true;

        // 发送请求到服务器扫描箱子
        System.out.println("[BlockReplacement] 发送箱子扫描请求到服务器: " + chestPos);
        NetworkManager.INSTANCE.sendToServer(new ChestScanRequestPacket(chestPos));

        int centerX = this.width / 2;

        // 开始替换按钮 - 白色文本
        startButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.block_replacement.start"),
                        button -> startReplacement())
                .bounds(centerX - 100, this.height - 35, 90, 22)
                .build());

        // 取消按钮 - 白色文本（menglannnn: 如果有父界面则返回父界面，否则关闭）
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.button.cancel"),
                        button -> {
                            if (parentScreen != null) {
                                Minecraft.getInstance().setScreen(parentScreen);
                            } else {
                                this.onClose();
                            }
                        })
                .bounds(centerX + 10, this.height - 35, 90, 22)
                .build());

        // 清除映射按钮 - 白色文本
        clearButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.block_replacement.clear_mapping"),
                        button -> clearMapping())
                .bounds(centerX - 45, this.height - 65, 90, 20)
                .build());

        updateButtonStates();
    }

    /**
     * 静态方法：更新箱子内容（由服务器响应调用）
     */
    public static void updateChestContents(BlockPos pos, Map<Block, Integer> contents) {
        System.out.println("[BlockReplacement] 收到服务器箱子数据: " + pos + ", " + contents.size() + " 种方块");
        chestBlocks.clear();
        chestBlocks.putAll(contents);

        // 如果当前有打开的BlockReplacementScreen，刷新显示
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BlockReplacementScreen screen) {
            screen.waitingForChestData = false;
            screen.updateButtonStates();
        }
    }

    /**
     * 扫描选区内的方块
     */
    private void scanSelection() {
        selectionBlocks.clear();
        if (minecraft == null || minecraft.level == null) return;

        for (BlockPos pos : BlockPos.betweenClosed(selectionStart, selectionEnd)) {
            BlockState state = minecraft.level.getBlockState(pos);
            Block block = state.getBlock();
            // 跳过空气
            if (!state.isAir()) {
                selectionBlocks.merge(block, 1, Integer::sum);
            }
        }
    }

    /**
     * 清除所有映射
     */
    private void clearMapping() {
        replacementMap.clear();
        leftSelectedIndex = -1;
        rightSelectedIndex = -1;
        updateButtonStates();
    }

    /**
     * 开始替换 - 创建NPC任务
     */
    private void startReplacement() {
        if (replacementMap.isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("message.block_replacement.select_mapping"));
            }
            return;
        }

        // 获取选区内的所有方块位置
        List<BlockPos> targetBlocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(selectionStart, selectionEnd)) {
            targetBlocks.add(pos.immutable());
        }

        // 转换替换映射为String格式（方块ID -> 方块ID）
        Map<String, String> stringReplacementMap = new HashMap<>();
        for (Map.Entry<Block, Block> entry : replacementMap.entrySet()) {
            String fromId = entry.getKey().getDescriptionId();
            String toId = entry.getValue().getDescriptionId();
            stringReplacementMap.put(fromId, toId);
        }

        // 发送创建任务包
        CreatePlanningTaskPacket packet = new CreatePlanningTaskPacket(
                buildBoxPos,
                targetBlocks,
                CreatePlanningTaskPacket.TaskType.REPLACE,
                null,
                stringReplacementMap
        );
        NetworkManager.INSTANCE.sendToServer(packet);

        // 关闭界面
        this.onClose();
    }

    /**
     * 左侧滚动
     */
    private void scrollLeft(int direction) {
        int maxScroll = Math.max(0, selectionBlocks.size() - ITEMS_PER_PAGE);
        leftScrollOffset = Math.max(0, Math.min(maxScroll, leftScrollOffset + direction));
    }

    /**
     * 右侧滚动
     */
    private void scrollRight(int direction) {
        int maxScroll = Math.max(0, chestBlocks.size() - ITEMS_PER_PAGE);
        rightScrollOffset = Math.max(0, Math.min(maxScroll, rightScrollOffset + direction));
    }

    /**
     * 计算目标方块数量（已建立映射的选区方块数量）
     */
    private int calculateTargetBlocksCount() {
        int count = 0;
        for (Map.Entry<Block, Block> entry : replacementMap.entrySet()) {
            Block fromBlock = entry.getKey();
            Integer blockCount = selectionBlocks.get(fromBlock);
            if (blockCount != null) {
                count += blockCount;
            }
        }
        return count;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        this.renderBackground(graphics);

        // 渲染标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // 渲染费用信息（替换根据目标方块数量计算，每个0.04元）
        int targetBlocksCount = calculateTargetBlocksCount();
        double totalCost = targetBlocksCount * 0.04;
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.block_replacement.cost_info", targetBlocksCount, totalCost).getString(),
                this.width / 2, 30, 0xFFAA00);

        // 渲染列标题
        graphics.drawString(this.font, Component.translatable("gui.block_replacement.selection_header").getString(), 20, 50, 0x55FF55);
        graphics.drawString(this.font, Component.translatable("gui.block_replacement.chest_header").getString(), this.width - 220, 50, 0x55AAFF);

        // 渲染中间映射信息
        renderMappingInfo(graphics);

        // 渲染左侧列表
        renderLeftList(graphics, mouseX, mouseY);

        // 渲染右侧列表
        renderRightList(graphics, mouseX, mouseY);

        // 如果正在等待服务器数据，显示提示
        if (waitingForChestData) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.common.loading_chest_data").getString(), this.width / 2, this.height / 2, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    /**
     * 渲染左侧列表（选区方块）
     */
    private void renderLeftList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 20;
        int y = 65;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(selectionBlocks.entrySet());

        for (int i = leftScrollOffset; i < Math.min(entries.size(), leftScrollOffset + ITEMS_PER_PAGE); i++) {
            Map.Entry<Block, Integer> entry = entries.get(i);
            Block block = entry.getKey();
            int count = entry.getValue();
            int displayIndex = i - leftScrollOffset;
            int itemY = y + displayIndex * ITEM_HEIGHT;

            // 检查是否已映射
            boolean isMapped = replacementMap.containsKey(block);
            Block mappedTo = replacementMap.get(block);

            // 渲染背景
            int bgColor;
            if (displayIndex == leftSelectedIndex) {
                bgColor = 0xFF6666CC; // 选中：蓝色
            } else if (isMapped) {
                bgColor = 0xFF448844; // 已映射：绿色
            } else {
                bgColor = 0xFF333333; // 默认：深灰
            }
            graphics.fill(x, itemY, x + listWidth, itemY + ITEM_HEIGHT - 2, bgColor);

            // 渲染物品图标
            ItemStack stack = new ItemStack(block.asItem());
            graphics.renderItem(stack, x + 6, itemY + 4);

            // 渲染方块名称和数量（使用中文映射）
            String name = com.xiaoliang.simukraft.utils.BlockNameTranslator.getItemName(stack);
            if (name.length() > 10) {
                name = name.substring(0, 10) + "...";
            }
            graphics.drawString(this.font, name, x + 28, itemY + 4, 0xFFFFFF);
            graphics.drawString(this.font, "x" + count, x + 28, itemY + 15, 0xAAAAAA);

            // 如果已映射，显示映射信息
            if (isMapped && mappedTo != null) {
                ItemStack toStack = new ItemStack(mappedTo.asItem());
                graphics.renderItem(toStack, x + 125, itemY + 4);
                graphics.fill(x + 110, itemY + 10, x + 120, itemY + 14, 0xFFFFFF); // 箭头
                graphics.fill(x + 115, itemY + 8, x + 117, itemY + 16, 0xFFFFFF);
            }
        }

        // 渲染滚动条
        if (selectionBlocks.size() > ITEMS_PER_PAGE) {
            renderScrollbar(graphics, x + listWidth - 6, y, 6, listHeight,
                    selectionBlocks.size(), ITEMS_PER_PAGE, leftScrollOffset);
        }
    }

    /**
     * 渲染右侧列表（箱子方块）
     */
    private void renderRightList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.width - 220;
        int y = 65;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(chestBlocks.entrySet());

        for (int i = rightScrollOffset; i < Math.min(entries.size(), rightScrollOffset + ITEMS_PER_PAGE); i++) {
            Map.Entry<Block, Integer> entry = entries.get(i);
            Block block = entry.getKey();
            int count = entry.getValue();
            int displayIndex = i - rightScrollOffset;
            int itemY = y + displayIndex * ITEM_HEIGHT;

            // 渲染背景
            int bgColor = (displayIndex == rightSelectedIndex) ? 0xFF6666CC : 0xFF333333;
            graphics.fill(x, itemY, x + listWidth, itemY + ITEM_HEIGHT - 2, bgColor);

            // 渲染物品图标
            ItemStack stack = new ItemStack(block.asItem());
            graphics.renderItem(stack, x + 6, itemY + 4);

            // 渲染方块名称和数量（使用中文映射）
            String name = com.xiaoliang.simukraft.utils.BlockNameTranslator.getItemName(stack);
            if (name.length() > 10) {
                name = name.substring(0, 10) + "...";
            }
            graphics.drawString(this.font, name, x + 28, itemY + 4, 0xFFFFFF);
            graphics.drawString(this.font, "x" + count, x + 28, itemY + 15, 0xAAAAAA);
        }

        // 渲染滚动条
        if (chestBlocks.size() > ITEMS_PER_PAGE) {
            renderScrollbar(graphics, x + listWidth - 6, y, 6, listHeight,
                    chestBlocks.size(), ITEMS_PER_PAGE, rightScrollOffset);
        }
    }

    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics graphics, int x, int y, int width, int height,
                                  int totalItems, int visibleItems, int scrollOffset) {
        // 滚动条背景
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        // 计算滑块位置和大小
        float scrollRatio = (float) scrollOffset / (totalItems - visibleItems);
        float thumbRatio = (float) visibleItems / totalItems;
        int thumbHeight = Math.max(10, (int) (height * thumbRatio));
        int thumbY = y + (int) ((height - thumbHeight) * scrollRatio);

        // 渲染滑块
        graphics.fill(x + 1, thumbY, x + width - 1, thumbY + thumbHeight, 0xFF888888);
    }

    /**
     * 渲染映射信息
     */
    private void renderMappingInfo(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int y = 65;

        graphics.drawCenteredString(this.font, Component.translatable("gui.block_replacement.mapping_title").getString(), centerX, y, 0xFFFFFF);
        y += 18;

        if (replacementMap.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.block_replacement.no_mapping").getString(), centerX, y + 10, 0x888888);
            graphics.drawCenteredString(this.font, Component.translatable("gui.block_replacement.hint_left").getString(), centerX, y + 25, 0x666666);
            graphics.drawCenteredString(this.font, Component.translatable("gui.block_replacement.hint_right").getString(), centerX, y + 38, 0x666666);
        } else {
            int count = 0;
            for (Map.Entry<Block, Block> entry : replacementMap.entrySet()) {
                if (count >= 6) {
                    graphics.drawCenteredString(this.font, Component.translatable("gui.block_replacement.more_items", replacementMap.size() - 6).getString(),
                            centerX, y, 0xAAAAAA);
                    break;
                }

                ItemStack fromStack = new ItemStack(entry.getKey().asItem());
                ItemStack toStack = new ItemStack(entry.getValue().asItem());

                // 渲染图标
                graphics.renderItem(fromStack, centerX - 50, y);
                graphics.drawCenteredString(this.font, "→", centerX, y + 4, 0xFFFF00);
                graphics.renderItem(toStack, centerX + 30, y);

                y += 22;
                count++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理左侧列表点击（选区方块）
        int leftX = 20;
        int leftY = 65;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        if (mouseX >= leftX && mouseX <= leftX + listWidth &&
            mouseY >= leftY && mouseY <= leftY + listHeight) {
            int relativeY = (int) (mouseY - leftY);
            int index = relativeY / ITEM_HEIGHT;
            if (index >= 0 && index < ITEMS_PER_PAGE) {
                int actualIndex = leftScrollOffset + index;
                if (actualIndex < selectionBlocks.size()) {
                    leftSelectedIndex = index;

                    // 如果右侧也选中了，直接建立映射
                    if (rightSelectedIndex >= 0) {
                        Block leftBlock = getBlockByIndex(selectionBlocks, actualIndex);
                        Block rightBlock = getBlockByIndex(chestBlocks, rightScrollOffset + rightSelectedIndex);
                        if (leftBlock != null && rightBlock != null) {
                            replacementMap.put(leftBlock, rightBlock);
                        }
                    }
                    updateButtonStates();
                    return true;
                }
            }
        }

        // 处理右侧列表点击（箱子方块）
        int rightX = this.width - 220;
        int rightY = 65;

        if (mouseX >= rightX && mouseX <= rightX + listWidth &&
            mouseY >= rightY && mouseY <= rightY + listHeight) {
            int relativeY = (int) (mouseY - rightY);
            int index = relativeY / ITEM_HEIGHT;
            if (index >= 0 && index < ITEMS_PER_PAGE) {
                int actualIndex = rightScrollOffset + index;
                if (actualIndex < chestBlocks.size()) {
                    rightSelectedIndex = index;

                    // 如果左侧也选中了，直接建立映射
                    if (leftSelectedIndex >= 0) {
                        Block leftBlock = getBlockByIndex(selectionBlocks, leftScrollOffset + leftSelectedIndex);
                        Block rightBlock = getBlockByIndex(chestBlocks, actualIndex);
                        if (leftBlock != null && rightBlock != null) {
                            replacementMap.put(leftBlock, rightBlock);
                        }
                    }
                    updateButtonStates();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 左侧滚动
        int leftX = 20;
        int leftY = 65;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        if (mouseX >= leftX && mouseX <= leftX + listWidth &&
            mouseY >= leftY && mouseY <= leftY + listHeight) {
            scrollLeft(delta > 0 ? -1 : 1);
            return true;
        }

        // 右侧滚动
        int rightX = this.width - 220;
        int rightY = 65;

        if (mouseX >= rightX && mouseX <= rightX + listWidth &&
            mouseY >= rightY && mouseY <= rightY + listHeight) {
            scrollRight(delta > 0 ? -1 : 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateButtonStates() {
        startButton.active = !replacementMap.isEmpty() && !waitingForChestData;
        clearButton.active = !replacementMap.isEmpty();
    }

    private Block getBlockByIndex(Map<Block, Integer> map, int index) {
        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(map.entrySet());
        if (index >= 0 && index < entries.size()) {
            return entries.get(index).getKey();
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
