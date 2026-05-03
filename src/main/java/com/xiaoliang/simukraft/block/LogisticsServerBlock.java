package com.xiaoliang.simukraft.block;

import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.NPCWorkStatusPacket;
import com.xiaoliang.simukraft.utils.ClientRuntimeBridge;
import com.xiaoliang.simukraft.world.BaseBuildingHiredData;
import com.xiaoliang.simukraft.world.LogisticsData;
import com.xiaoliang.simukraft.world.LogisticsHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * 物流盒服务端方块 — 放置后作为仓库核心，可雇佣仓库管理员、创建仓库、管理物流网络。
 */
public class LogisticsServerBlock extends Block {

    public LogisticsServerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.COLOR_BLUE))
                .strength(1.0F)
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }

    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                          @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            try {
                ClientRuntimeBridge.openScreen(
                        "com.xiaoliang.simukraft.client.gui.LogisticsServerScreen",
                        new Class<?>[]{BlockPos.class},
                        pos
                );
            } catch (Exception e) {
                System.err.println("[LogisticsServerBlock] 打开界面失败: " + e.getMessage());
            }
        }
        return Objects.requireNonNull(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(Objects.requireNonNull(newState.getBlock())) && !level.isClientSide && level instanceof ServerLevel serverLevel) {

            // 照抄建筑盒模式：当物流服务端被破坏时，自动解雇仓库管理员
            var server = serverLevel.getServer();
            UUID npcUuid = LogisticsHiredData.getServerBoxHiredNpc(server, pos);

            if (npcUuid != null) {
                var npc = BaseBuildingHiredData.findNPCByUuid(server, npcUuid);
                if (npc != null) {
                    // 重置NPC状态为空闲
                    npc.setWorkStatus(WorkStatus.IDLE);
                    npc.setWorking(false);
                    npc.setJob("unemployed");
                    npc.resetToIdle();
                    npc.setItemInHand(InteractionHand.MAIN_HAND, Objects.requireNonNull(ItemStack.EMPTY));
                    npc.setItemInHand(InteractionHand.OFF_HAND, Objects.requireNonNull(ItemStack.EMPTY));
                    com.xiaoliang.simukraft.utils.NPCDataManager.saveJobData(npc);

                    // 发送解雇数据包给所有玩家
                    server.getPlayerList().getPlayers().forEach(player -> {
                        NetworkManager.sendToPlayer(
                            new NPCWorkStatusPacket(npc.getUUID(), WorkStatus.IDLE, pos),
                            player
                        );
                    });

                    // 发送聊天提示（通过通知接口，按城市过滤）
                    String npcName = npc.getFullName();
                    java.util.UUID cityId = npc.getCityId();
                    if (cityId != null) {
                        com.xiaoliang.simukraft.utils.CityMessageUtils.sendToCityGroup(
                            server, cityId,
                            net.minecraft.network.chat.Component.translatable("message.logistics.destroyed", npcName)
                        );
                    }
                }

                // 从雇佣记录中移除
                LogisticsHiredData.removeServerBoxHired(server, pos);
            }

            // 移除仓库数据
            LogisticsData data = LogisticsData.get(serverLevel);
            LogisticsData.Warehouse warehouse = data.getWarehouseByBlockPos(pos);
            if (warehouse != null) {
                data.removeWarehouse(warehouse.getWarehouseId());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
