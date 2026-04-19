package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import com.xiaoliang.simukraft.client.preview.AreaSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.CreatePlanningTaskPacket;
import com.xiaoliang.simukraft.network.NetworkManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Objects;

/**
 * 拆除方块确认界面 - 显示选区内方块统计
 */
public class RemoveBlocksConfirmScreen extends Screen {
    private final BlockPos buildBoxPos;
    private final Screen parent;
    private final List<BlockPos> selectedBlocks;
    private final Map<Block, Integer> blockCounts = new LinkedHashMap<>();
    private int currentPage = 0;
    private static final int BLOCKS_PER_PAGE = 8;
    private static final double COST_PER_BLOCK = 0.02; // 每个方块0.02元（与实际扣款一致）
    private double totalCost = 0.0;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    public RemoveBlocksConfirmScreen(BlockPos buildBoxPos, Screen parent, List<BlockPos> selectedBlocks) {
        super(Component.translatable("gui.remove_blocks_confirm.title"));
        this.buildBoxPos = buildBoxPos;
        this.parent = parent;
        this.selectedBlocks = selectedBlocks;

        // 统计方块
        countBlocks();

        // 计算总费用
        calculateTotalCost();

        Minecraft.getInstance().getSoundManager().play(
            nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.BUILD_BOX_OPEN.get()), 1.0F))
        );
    }

    /**
     * 计算总费用
     */
    private void calculateTotalCost() {
        int nonAirBlocks = 0;
        for (int count : blockCounts.values()) {
            nonAirBlocks += count;
        }
        totalCost = nonAirBlocks * COST_PER_BLOCK;
    }

    /**
     * 统计选区内的方块
     */
    private void countBlocks() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        for (BlockPos pos : selectedBlocks) {
            BlockState state = level.getBlockState(nn(pos));
            Block block = state.getBlock();

            // 跳过空气
            if (block.defaultBlockState().isAir()) continue;

            blockCounts.merge(block, 1, (left, right) -> left + right);
        }

        // 按数量排序
        List<Map.Entry<Block, Integer>> sorted = new ArrayList<>(blockCounts.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        blockCounts.clear();
        for (Map.Entry<Block, Integer> entry : sorted) {
            blockCounts.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    protected void init() {
        super.init();

        // 释放鼠标，让玩家可以操作界面
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.mouseHandler.releaseMouse();
            // 显示鼠标光标
            org.lwjgl.glfw.GLFW.glfwSetInputMode(
                minecraft.getWindow().getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL
            );
        }

        int centerX = this.width / 2;
        int buttonY = this.height - 40;

        // 取消按钮
        this.addRenderableWidget(nn(Button.builder(
                nn(Component.translatable("gui.cancel")),
                button -> cancelRemoval())
            .bounds(centerX - 110, buttonY, 100, 20)
            .build()));

        // 开始拆除按钮
        this.addRenderableWidget(nn(Button.builder(
                nn(Component.translatable("gui.remove_blocks_confirm.start")),
                button -> startRemoval())
            .bounds(centerX + 10, buttonY, 100, 20)
            .build()));

        // 上一页按钮
        this.addRenderableWidget(nn(Button.builder(
                nn(Component.literal("<")),
                button -> prevPage())
            .bounds(centerX - 160, buttonY, 30, 20)
            .build()));

        // 下一页按钮
        this.addRenderableWidget(nn(Button.builder(
                nn(Component.literal(">")),
                button -> nextPage())
            .bounds(centerX + 130, buttonY, 30, 20)
            .build()));
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    private void nextPage() {
        int maxPage = (blockCounts.size() - 1) / BLOCKS_PER_PAGE;
        if (currentPage < maxPage) {
            currentPage++;
        }
    }

    private void cancelRemoval() {
        // 返回区域选择界面
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void startRemoval() {
        // 清除预览图
        AreaSelectionManager.endSelection();

        // 发送网络包到服务器创建拆除任务
        NetworkManager.INSTANCE.sendToServer(
            new CreatePlanningTaskPacket(buildBoxPos, selectedBlocks, CreatePlanningTaskPacket.TaskType.REMOVE)
        );

        // 关闭自由相机
        FreeCameraManager.deactivate();

        // 显示消息
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                nn(Component.translatable("gui.remove_blocks_confirm.task_created", selectedBlocks.size())),
                false
            );
        }

        // 关闭界面
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        var font = nn(this.font);
        var title = nn(this.title);
        // 黑色半透明背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);

        int centerX = this.width / 2;
        int y = 20;

        // 标题
        guiGraphics.drawCenteredString(
            font,
            title,
            centerX,
            y,
            0xFFFFFF
        );

        y += 25;

        // 统计信息
        guiGraphics.drawCenteredString(
            nn(this.font),
            nn(Component.literal(safeString(String.format("§e选区总计: %d 个方块 | %d 种不同类型",
                selectedBlocks.size(), blockCounts.size())))),
            centerX,
            y,
            0xFFFFFF
        );

        y += 20;

        // 费用信息
        guiGraphics.drawCenteredString(
            nn(this.font),
            nn(Component.literal(safeString(String.format("§c拆除费用: ¥%.2f (每个方块 ¥%.2f)",
                totalCost, COST_PER_BLOCK)))),
            centerX,
            y,
            0xFFFFFF
        );

        y += 25;

        // 分页显示方块列表
        List<Map.Entry<Block, Integer>> entries = new ArrayList<>(blockCounts.entrySet());
        int startIndex = currentPage * BLOCKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BLOCKS_PER_PAGE, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<Block, Integer> entry = entries.get(i);
            Block block = entry.getKey();
            int count = entry.getValue();

            // 获取方块的物品形式用于显示
            ItemStack itemStack = new ItemStack(nn(block.asItem()));

            // 绘制物品图标
            int itemX = centerX - 120;
            guiGraphics.renderItem(itemStack, itemX, y - 4);

            // 绘制方块名称和数量（使用中文映射）
            String blockName = com.xiaoliang.simukraft.utils.BlockNameTranslator.getItemName(itemStack);
            guiGraphics.drawString(
                nn(this.font),
                nn(Component.literal(safeString(blockName + " §7x " + count))),
                itemX + 25,
                y,
                0xFFFFFF
            );

            y += 22;
        }

        // 页码信息
        int maxPage = Math.max(0, (blockCounts.size() - 1) / BLOCKS_PER_PAGE);
        guiGraphics.drawCenteredString(
            nn(this.font),
            nn(Component.literal(safeString(String.format("§7第 %d/%d 页", currentPage + 1, maxPage + 1)))),
            centerX,
            this.height - 65,
            0xFFFFFF
        );

        // 提示信息
        guiGraphics.drawCenteredString(
            nn(this.font),
            nn(Component.translatable("gui.remove_blocks_confirm.hint")),
            centerX,
            this.height - 55,
            0xF5F5A0
        );

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        cancelRemoval();
    }
}
