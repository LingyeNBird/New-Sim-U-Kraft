package com.xiaoliang.simukraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.Objects;

public class LightBlock extends Block {
    public LightBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.METAL))
                .strength(1.0F)
                .sound(Objects.requireNonNull(SoundType.GLASS))
                .lightLevel(state -> 15)
                .instrument(NoteBlockInstrument.HAT));
    }
}
