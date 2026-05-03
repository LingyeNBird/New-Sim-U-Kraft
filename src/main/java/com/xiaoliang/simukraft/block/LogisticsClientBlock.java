package com.xiaoliang.simukraft.block;

import com.xiaoliang.simukraft.utils.ClientRuntimeBridge;
import com.xiaoliang.simukraft.world.LogisticsData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/**
 * 物流盒客户端方块 — 作为物流网络的收发端口，无需雇佣 NPC。
 */
public class LogisticsClientBlock extends Block {

    public LogisticsClientBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.COLOR_ORANGE))
                .strength(1.0F)
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }

    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                          @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            try {
                ClientRuntimeBridge.openScreen(
                        "com.xiaoliang.simukraft.client.gui.LogisticsClientScreen",
                        new Class<?>[]{BlockPos.class},
                        pos
                );
            } catch (Exception e) {
                System.err.println("[LogisticsClientBlock] 打开界面失败: " + e.getMessage());
            }
        }
        return Objects.requireNonNull(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(Objects.requireNonNull(newState.getBlock())) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            LogisticsData data = LogisticsData.get(serverLevel);
            LogisticsData.LogisticsClient client = data.getClientByBlockPos(pos);
            if (client != null) {
                data.removeClient(client.getClientId());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
