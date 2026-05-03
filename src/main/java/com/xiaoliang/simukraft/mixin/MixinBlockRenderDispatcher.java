package com.xiaoliang.simukraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoliang.simukraft.client.preview.PreviewRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Mixin BlockRenderDispatcher
 * 
 * 性能优化：跳过被完全遮挡的预览方块。
 * 如果一个方块的 6 个面全部被实心邻居挡住，就跳过整个 renderSingleBlock 调用。
 * 其他方块完全交给原版渲染，不做任何自定义渲染，避免视觉问题。
 */
@Mixin(BlockRenderDispatcher.class)
@SuppressWarnings("null")
public class MixinBlockRenderDispatcher {

    @Inject(
        method = "renderSingleBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderSingleBlockHead(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci) {

        Set<BlockPos> renderingPositions = PreviewRenderState.getRenderingBlockPositions();

        if (renderingPositions == null || renderingPositions.isEmpty()) {
            return;
        }

        BlockPos currentPos = PreviewRenderState.getCurrentRenderingPos();

        if (!renderingPositions.contains(currentPos)) {
            return;
        }

        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        if (!simukraft$isFullBlock(state)) {
            return;
        }

        if (simukraft$isFullyOccluded(currentPos, renderingPositions)) {
            ci.cancel();
        }
    }

    /**
     * 检查方块是否被 6 个方向的实心邻居完全遮挡。
     * 只有当所有 6 个面都被遮挡时才返回 true，此时该方块完全不可见，可以安全跳过。
     */
    @Unique
    private boolean simukraft$isFullyOccluded(BlockPos pos, Set<BlockPos> blockPosSet) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (!blockPosSet.contains(neighborPos)) {
                return false;
            }
            BlockState neighborState = PreviewRenderState.getBlockState(neighborPos);
            if (neighborState == null || !simukraft$isFullBlock(neighborState)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private boolean simukraft$isFullBlock(BlockState state) {
        return state.canOcclude();
    }
}
