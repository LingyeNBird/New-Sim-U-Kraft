package com.xiaoliang.simukraft.client.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 预览渲染状态管理器
 * 用于在 SchematicBlockRenderer 和 MixinBlockRenderDispatcher 之间传递状态
 */
public class PreviewRenderState {

    // 存储当前正在渲染的预览方块位置和 BlockState 的映射
    private static final ThreadLocal<Map<BlockPos, BlockState>> renderingBlockStates = ThreadLocal.withInitial(HashMap::new);

    // 存储当前正在渲染的方块位置
    private static final ThreadLocal<BlockPos> currentRenderingPos = ThreadLocal.withInitial(() -> BlockPos.ZERO);

    /**
     * 设置当前正在渲染的预览方块位置集合
     * 同时从 SchematicBlockData 列表中提取 BlockState 信息
     */
    public static void setRenderingBlocks(java.util.List<SchematicBlockData> blocks) {
        Map<BlockPos, BlockState> stateMap = new HashMap<>();
        for (SchematicBlockData block : blocks) {
            stateMap.put(block.pos(), block.blockState());
        }
        renderingBlockStates.set(stateMap);
    }

    /**
     * 设置当前正在渲染的预览方块位置集合（兼容旧版本）
     */
    public static void setRenderingBlockPositions(Set<BlockPos> positions) {
        // 如果只提供位置，创建一个空的 BlockState 映射
        // 实际使用时应该调用 setRenderingBlocks
        Map<BlockPos, BlockState> stateMap = new HashMap<>();
        for (BlockPos pos : positions) {
            stateMap.put(pos, null);
        }
        renderingBlockStates.set(stateMap);
    }

    /**
     * 获取当前正在渲染的预览方块 BlockState 映射
     */
    public static Map<BlockPos, BlockState> getRenderingBlockStates() {
        return renderingBlockStates.get();
    }

    /**
     * 获取当前正在渲染的预览方块位置集合
     */
    public static Set<BlockPos> getRenderingBlockPositions() {
        return renderingBlockStates.get().keySet();
    }

    /**
     * 获取指定位置的 BlockState
     */
    public static BlockState getBlockState(BlockPos pos) {
        return renderingBlockStates.get().get(pos);
    }

    /**
     * 设置当前正在渲染的方块位置
     */
    public static void setCurrentRenderingPos(BlockPos pos) {
        currentRenderingPos.set(pos);
    }

    /**
     * 获取当前正在渲染的方块位置
     */
    public static BlockPos getCurrentRenderingPos() {
        return currentRenderingPos.get();
    }

    /**
     * 清除渲染状态
     */
    public static void clearRenderingState() {
        renderingBlockStates.remove();
        currentRenderingPos.remove();
    }
}
