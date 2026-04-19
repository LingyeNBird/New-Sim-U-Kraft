package com.xiaoliang.simukraft.block;

import com.xiaoliang.simukraft.init.ModBlocks;
import com.xiaoliang.simukraft.network.CheckCityStatusPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;
import javax.annotation.Nonnull;

public class CityCoreBlock extends Block {
    public CityCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.METAL))
                .strength(1.0F)  // 默认可破坏
                .explosionResistance(3600000.0F)  // 最高爆炸抗性
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        super.onRemove(
                Objects.requireNonNull(state),
                Objects.requireNonNull(level),
                Objects.requireNonNull(pos),
                Objects.requireNonNull(newState),
                isMoving
        );
        
        // 如果新状态是空气（方块被破坏），检查是否需要回溯
        if (!level.isClientSide && newState.isAir()) {
            checkAndRestoreBlock(level, pos);
        }
    }
    
    /**
     * 检查并恢复城市核心方块（回溯机制）
     */
    private void checkAndRestoreBlock(Level level, BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        CityData cityData = CityData.get(serverLevel);
        CityData.CityInfo city = cityData.getCityByCorePos(pos);
        
        if (city != null) {
            // 该位置有城市，需要回溯恢复方块
            
            // 立即恢复方块
            BlockState restoredState = Objects.requireNonNull(Objects.requireNonNull(ModBlocks.CITY_CORE.get()).defaultBlockState());
            level.setBlock(
                    Objects.requireNonNull(pos),
                    restoredState,
                    3
            );
            
            // 播放放置音效
            SoundEvent sound = Objects.requireNonNull(net.minecraft.sounds.SoundEvents.METAL_PLACE);
            level.playSound(null, Objects.requireNonNull(pos), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // 向附近的玩家发送提示
            for (Player player : level.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                    player.displayClientMessage(Objects.requireNonNull(
                            net.minecraft.network.chat.Component.translatable("message.simukraft.city_core.protected")
                    ), true);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        if (!oldState.is(Objects.requireNonNull(state.getBlock()))) {
            SoundEvent sound = Objects.requireNonNull(net.minecraft.sounds.SoundEvents.METAL_PLACE);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        super.onPlace(
                Objects.requireNonNull(state),
                Objects.requireNonNull(level),
                Objects.requireNonNull(pos),
                Objects.requireNonNull(oldState),
                isMoving
        );
    }

    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            CheckCityStatusPacket packet = new CheckCityStatusPacket(pos);
            NetworkManager.INSTANCE.sendToServer(packet);
        }
        return InteractionResult.SUCCESS;
    }
}
