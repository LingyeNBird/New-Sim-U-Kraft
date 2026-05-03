package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.ContainerUtils;
import com.xiaoliang.simukraft.world.LogisticsData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * C→S 解绑单个容器
 */
public class UnbindContainerPacket {
    private final BlockPos clientBlockPos;
    private final BlockPos containerPos;

    public UnbindContainerPacket(BlockPos clientBlockPos, BlockPos containerPos) {
        this.clientBlockPos = clientBlockPos;
        this.containerPos = containerPos;
    }

    public UnbindContainerPacket(FriendlyByteBuf buf) {
        this.clientBlockPos = Objects.requireNonNull(buf.readBlockPos());
        this.containerPos = Objects.requireNonNull(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(clientBlockPos));
        buf.writeBlockPos(Objects.requireNonNull(containerPos));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel().getServer().overworld();
            LogisticsData data = LogisticsData.get(level);
            LogisticsData.LogisticsClient client = data.getClientByBlockPos(clientBlockPos);
            if (client == null) return;

            client.getPortPositions().remove(containerPos);
            data.setDirty();

            // 返回更新后的容器列表
            List<ContainerListResponsePacket.ContainerEntry> entries = new ArrayList<>();
            for (BlockPos pos : client.getPortPositions()) {
                BlockPos safePos = Objects.requireNonNull(pos);
                String blockName = level.getBlockState(safePos).getBlock().getName().getString();
                List<ItemStack> items = ContainerUtils.getAllItems(level, safePos);
                int kinds = 0, total = 0;
                for (ItemStack stack : items) {
                    if (!stack.isEmpty()) { kinds++; total += stack.getCount(); }
                }
                entries.add(new ContainerListResponsePacket.ContainerEntry(safePos, blockName, kinds, total));
            }
            NetworkManager.sendToPlayer(new ContainerListResponsePacket(clientBlockPos, entries), player);

            // 同步客户端状态
            boolean hasPorts = client.hasPorts();
            int portCount = client.getPortPositions().size();
            var syncPacket = LogisticsSyncPacket.clientStatus(clientBlockPos, hasPorts, portCount, client.getClientId());
            NetworkManager.sendToPlayer(syncPacket, player);

            // 同步容器位置数据
            if (client.hasPorts()) {
                var clientDataPacket = new SyncLogisticsClientDataPacket(clientBlockPos, client.getClientId(), client.getPortPositions());
                NetworkManager.sendToPlayer(clientDataPacket, player);
            } else {
                // 如果没有容器了，发送空的位置列表
                var clientDataPacket = new SyncLogisticsClientDataPacket(clientBlockPos, client.getClientId(), new ArrayList<>());
                NetworkManager.sendToPlayer(clientDataPacket, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
