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

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 方块填充GUI
 * 选择箱子里的方块来填充选区内的空气
 */
@SuppressWarnings("null")
public class BlockFillScreen extends Screen {
    private final BlockPos selectionStart;
    private final BlockPos selectionEnd;
    private final BlockPos chestPos;
    private final BlockPos buildBoxPos;

    // 箱子里的方块及其数量
    private static Map<Block, Integer> chestBlocks = new LinkedHashMap<>();
    // 选中的方块
    private Block selectedBlock = null;

    // 滚动偏移
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private static final int ITEM_HEIGHT = 28;

    // 按钮
    private Button startButton;
    // 等待服务器响应的标志
    private boolean waitingForChestData = true;

    public BlockFillScreen(BlockPos selectionStart, BlockPos selectionEnd, BlockPos chestPos, BlockPos buildBoxPos) {
        super(Component.translatable("gui.block_fill.title"));
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.chestPos = chestPos;
        this.buildBoxPos = buildBoxPos;
    }

    @Override
    protected void init() {
        super.init();

        // 清空之前的箱子数据
        chestBlocks.clear();
        waitingForChestData = true;

        // 发送请求到服务器扫描箱子
        System.out.println("[BlockFill] 发送箱子扫描请求到服务器: " + chestPos);
        NetworkManager.INSTANCE.sendToServer(new ChestScanRequestPacket(chestPos));

        int centerX = this.width / 2;

        // 开始填充按钮
        startButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.block_fill.start"),
                        button -> startFill())
                .bounds(centerX - 100, this.height - 35, 90, 22)
                .build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.button.cancel"),
                        button -> this.onClose())
                .bounds(centerX + 10, this.height - 35, 90, 22)
                .build());

        updateButtonStates();
    }

    /**
     * 静态方法：更新箱子内容（由服务器响应调用）
     */
    public static void updateChestContents(BlockPos pos, Map<Block, Integer> contents) {
        System.out.println("[BlockFill] 收到服务器箱子数据: " + pos + ", " + contents.size() + " 种方块");
        chestBlocks.clear();
        chestBlocks.putAll(contents);

        // 如果当前有打开的BlockFillScreen，刷新显示
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BlockFillScreen screen) {
            screen.waitingForChestData = false;
            screen.updateButtonStates();
        }
    }

    /**
     * 开始填充 - 创建NPC任务
     */
    private void startFill() {
        if (selectedBlock == null) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("message.block_fill.select_block"));
            }
            return;
        }

        // 获取选区内的所有位置（包括空气、杂草和花）
        List<BlockPos> targetBlocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(selectionStart, selectionEnd)) {
            targetBlocks.add(pos.immutable());
        }

        // 获取目标方块ID
        String targetBlockId = selectedBlock.getDescriptionId();

        // 发送创建任务包
        CreatePlanningTaskPacket packet = new CreatePlanningTaskPacket(
                buildBoxPos,
                targetBlocks,
                CreatePlanningTaskPacket.TaskType.FILL,
                targetBlockId,
                null
        );
        NetworkManager.INSTANCE.sendToServer(packet);

        // 关闭界面
        this.onClose();
    }

    /**
     * 滚动
     */
    private void scroll(int direction) {
        int maxScroll = Math.max(0, chestBlocks.size() - ITEMS_PER_PAGE);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + direction));
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        this.renderBackground(graphics);

        // 渲染标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // 渲染费用信息（填充只计算空气和花草方块，每个0.02元）
        int chargeableBlocks = countAirAndPlantsBlocks();
        double totalCost = chargeableBlocks * 0.02;
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.block_fill.cost_info", chargeableBlocks, totalCost).getString(),
                this.width / 2, 30, 0xFFAA00);

        // 渲染说明
        graphics.drawCenteredString(this.font, Component.translatable("gui.block_fill.hint").getString(), this.width / 2, 50, 0xAAAAAA);

        // 渲染列表
        renderBlockList(graphics, mouseX, mouseY);

        // 渲染选中信息
        if (selectedBlock != null) {
            ItemStack stack = new ItemStack(selectedBlock.asItem());
            String blockName = com.xiaoliang.simukraft.utils.BlockNameTranslator.getItemName(stack);
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.block_fill.selected", blockName).getString(), this.width / 2, this.height - 60, 0x55FF55);
        }

        // 如果正在等待服务器数据，显示提示
        if (waitingForChestData) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.common.loading_chest_data").getString(), this.width / 2, this.height / 2, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    /**
     * 渲染方块列表
     */
    private void renderBlockList(GuiGraphics graphics, int mouseX, int mouseY) {
        // 列表放在右侧，避免遮挡底部按钮
        int x = this.width - 180;
        int y = 70;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(chestBlocks.entrySet());

        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + ITEMS_PER_PAGE); i++) {
            Map.Entry<Block, Integer> entry = entries.get(i);
            Block block = entry.getKey();
            int count = entry.getValue();
            int displayIndex = i - scrollOffset;
            int itemY = y + displayIndex * ITEM_HEIGHT;

            // 渲染背景
            int bgColor;
            if (block == selectedBlock) {
                bgColor = 0xFF448844; // 选中：绿色
            } else {
                bgColor = 0xFF333333; // 默认：深灰
            }
            graphics.fill(x, itemY, x + listWidth, itemY + ITEM_HEIGHT - 2, bgColor);

            // 渲染物品图标
            ItemStack stack = new ItemStack(block.asItem());
            graphics.renderItem(stack, x + 6, itemY + 4);

            // 渲染方块名称和数量（使用中文映射）
            String name = com.xiaoliang.simukraft.utils.BlockNameTranslator.getItemName(stack);
            if (name.length() > 12) {
                name = name.substring(0, 12) + "...";
            }
            graphics.drawString(this.font, name, x + 28, itemY + 4, 0xFFFFFF);
            graphics.drawString(this.font, "x" + count, x + 28, itemY + 15, 0xAAAAAA);
        }

        // 渲染滚动条
        if (chestBlocks.size() > ITEMS_PER_PAGE) {
            renderScrollbar(graphics, x + listWidth - 6, y, 6, listHeight,
                    chestBlocks.size(), ITEMS_PER_PAGE, scrollOffset);
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
     * 计算空气和可替换植物方块数量
     */
    private int countAirAndPlantsBlocks() {
        if (minecraft == null || minecraft.level == null) return 0;
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(selectionStart, selectionEnd)) {
            net.minecraft.world.level.block.state.BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir() || isReplaceablePlant(state)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查方块是否是可替换的植物（杂草、花等）
     */
    private boolean isReplaceablePlant(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.GRASS ||
               block == net.minecraft.world.level.block.Blocks.TALL_GRASS ||
               block == net.minecraft.world.level.block.Blocks.FERN ||
               block == net.minecraft.world.level.block.Blocks.LARGE_FERN ||
               block == net.minecraft.world.level.block.Blocks.DEAD_BUSH ||
               block == net.minecraft.world.level.block.Blocks.DANDELION ||
               block == net.minecraft.world.level.block.Blocks.POPPY ||
               block == net.minecraft.world.level.block.Blocks.BLUE_ORCHID ||
               block == net.minecraft.world.level.block.Blocks.ALLIUM ||
               block == net.minecraft.world.level.block.Blocks.AZURE_BLUET ||
               block == net.minecraft.world.level.block.Blocks.RED_TULIP ||
               block == net.minecraft.world.level.block.Blocks.ORANGE_TULIP ||
               block == net.minecraft.world.level.block.Blocks.WHITE_TULIP ||
               block == net.minecraft.world.level.block.Blocks.PINK_TULIP ||
               block == net.minecraft.world.level.block.Blocks.OXEYE_DAISY ||
               block == net.minecraft.world.level.block.Blocks.CORNFLOWER ||
               block == net.minecraft.world.level.block.Blocks.LILY_OF_THE_VALLEY ||
               block == net.minecraft.world.level.block.Blocks.WITHER_ROSE ||
               block == net.minecraft.world.level.block.Blocks.SUNFLOWER ||
               block == net.minecraft.world.level.block.Blocks.LILAC ||
               block == net.minecraft.world.level.block.Blocks.ROSE_BUSH ||
               block == net.minecraft.world.level.block.Blocks.PEONY ||
               block == net.minecraft.world.level.block.Blocks.TALL_SEAGRASS ||
               block == net.minecraft.world.level.block.Blocks.SEAGRASS ||
               block == net.minecraft.world.level.block.Blocks.KELP ||
               block == net.minecraft.world.level.block.Blocks.KELP_PLANT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理列表点击 - 列表在右侧
        int listX = this.width - 180;
        int listY = 70;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            int relativeY = (int) (mouseY - listY);
            int index = relativeY / ITEM_HEIGHT;
            if (index >= 0 && index < ITEMS_PER_PAGE) {
                int actualIndex = scrollOffset + index;
                if (actualIndex < chestBlocks.size()) {
                    selectedBlock = getBlockByIndex(actualIndex);
                    updateButtonStates();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 列表滚动 - 列表在右侧
        int listX = this.width - 180;
        int listY = 70;
        int listWidth = 160;
        int listHeight = ITEMS_PER_PAGE * ITEM_HEIGHT;

        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            scroll(delta > 0 ? -1 : 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateButtonStates() {
        startButton.active = selectedBlock != null && !waitingForChestData;
    }

    private Block getBlockByIndex(int index) {
        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(chestBlocks.entrySet());
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
