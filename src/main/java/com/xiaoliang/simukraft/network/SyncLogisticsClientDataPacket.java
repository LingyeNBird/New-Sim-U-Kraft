package com.xiaoliang.simukraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S→C 物流客户端数据同步包
 * 同步客户端盒子的容器位置数据到客户端
 */
public class SyncLogisticsClientDataPacket {
    private final BlockPos blockPos;      // 客户端盒子位置
    private final UUID clientId;          // 客户端ID
    private final List<BlockPos> containerPositions; // 容器位置列表

    public SyncLogisticsClientDataPacket(BlockPos blockPos, UUID clientId, List<BlockPos> containerPositions) {
        this.blockPos = blockPos;
        this.clientId = clientId;
        this.containerPositions = new ArrayList<>(containerPositions);
    }

    public SyncLogisticsClientDataPacket(FriendlyByteBuf buf) {
        this.blockPos = Objects.requireNonNull(buf.readBlockPos());
        this.clientId = Objects.requireNonNull(buf.readUUID());
        int count = buf.readVarInt();
        this.containerPositions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.containerPositions.add(Objects.requireNonNull(buf.readBlockPos()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(blockPos));
        buf.writeUUID(Objects.requireNonNull(clientId));
        buf.writeVarInt(containerPositions.size());
        for (BlockPos pos : containerPositions) {
            buf.writeBlockPos(Objects.requireNonNull(pos));
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端接收数据，缓存到 ClientConnectionData
            com.xiaoliang.simukraft.client.gui.ClientConnectionData.updateClientData(
                    blockPos, clientId, containerPositions
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public UUID getClientId() {
        return clientId;
    }

    public List<BlockPos> getContainerPositions() {
        return containerPositions;
    }
}
