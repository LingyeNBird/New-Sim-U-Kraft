package com.xiaoliang.simukraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Objects;

public class OtherControlBoxBlock extends Block {
    public OtherControlBoxBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.METAL))
                .strength(0.8F)
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }
}
