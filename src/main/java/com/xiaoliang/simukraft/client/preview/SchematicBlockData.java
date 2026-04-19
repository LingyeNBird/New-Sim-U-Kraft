package com.xiaoliang.simukraft.client.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record SchematicBlockData(BlockPos pos, BlockState blockState, int packedLight, boolean translucent) {

    public SchematicBlockData(BlockPos pos, BlockState blockState) {
        this(pos, blockState, 15728880, true);
    }
}
