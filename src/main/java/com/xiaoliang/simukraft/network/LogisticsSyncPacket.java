package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S→C 物流数据同步包。
 * 服务端将物流盒状态（是否有 NPC、是否有仓库/端口、容器数量等）同步给客户端 GUI。
 */
@SuppressWarnings({"null", "unused"})
public class LogisticsSyncPacket {

    public enum SyncType { SERVER_STATUS, CLIENT_STATUS }

    private final SyncType syncType;
    private final BlockPos blockPos;

    // 服务端状态
    private final boolean hasNpc;
    private final boolean hasWarehouse;
    private final int containerCount;
    private final UUID warehouseId;

    // 客户端状态
    private final boolean hasPorts;
    private final int portCount;
    private final UUID clientId;

    /** 服务端状态构造 */
    public static LogisticsSyncPacket serverStatus(BlockPos pos, boolean hasNpc, boolean hasWarehouse,
                                                     int containerCount, UUID warehouseId) {
        return new LogisticsSyncPacket(SyncType.SERVER_STATUS, pos, hasNpc, hasWarehouse, containerCount, warehouseId,
                false, 0, null);
    }

    /** 客户端状态构造 */
    public static LogisticsSyncPacket clientStatus(BlockPos pos, boolean hasPorts, int portCount, UUID clientId) {
        return new LogisticsSyncPacket(SyncType.CLIENT_STATUS, pos, false, false, 0, null,
                hasPorts, portCount, clientId);
    }

    private LogisticsSyncPacket(SyncType syncType, BlockPos blockPos,
                                 boolean hasNpc, boolean hasWarehouse, int containerCount, UUID warehouseId,
                                 boolean hasPorts, int portCount, UUID clientId) {
        this.syncType = syncType;
        this.blockPos = blockPos;
        this.hasNpc = hasNpc;
        this.hasWarehouse = hasWarehouse;
        this.containerCount = containerCount;
        this.warehouseId = warehouseId;
        this.hasPorts = hasPorts;
        this.portCount = portCount;
        this.clientId = clientId;
    }

    public LogisticsSyncPacket(FriendlyByteBuf buf) {
        this.syncType = SyncType.values()[buf.readVarInt()];
        this.blockPos = buf.readBlockPos();
        this.hasNpc = buf.readBoolean();
        this.hasWarehouse = buf.readBoolean();
        this.containerCount = buf.readVarInt();
        this.warehouseId = buf.readBoolean() ? buf.readUUID() : null;
        this.hasPorts = buf.readBoolean();
        this.portCount = buf.readVarInt();
        this.clientId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(syncType.ordinal());
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(hasNpc);
        buf.writeBoolean(hasWarehouse);
        buf.writeVarInt(containerCount);
        buf.writeBoolean(warehouseId != null);
        if (warehouseId != null) buf.writeUUID(warehouseId);
        buf.writeBoolean(hasPorts);
        buf.writeVarInt(portCount);
        buf.writeBoolean(clientId != null);
        if (clientId != null) buf.writeUUID(clientId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 客户端接收：通知当前打开的 GUI 更新状态
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof LogisticsSyncReceiver receiver) {
                receiver.onLogisticsSync(this);
            } else {
                Simukraft.LOGGER.debug("[LogisticsSyncPacket] 当前屏幕不是 LogisticsSyncReceiver: {}", 
                    (mc.screen != null ? mc.screen.getClass().getName() : "null"));
            }
        });
        ctx.setPacketHandled(true);
    }

    // ── Getters ──
    public SyncType getSyncType() { return syncType; }
    public BlockPos getBlockPos() { return blockPos; }
    public boolean hasNpc() { return hasNpc; }
    public boolean hasWarehouse() { return hasWarehouse; }
    public int getContainerCount() { return containerCount; }
    public UUID getWarehouseId() { return warehouseId; }
    public boolean hasPorts() { return hasPorts; }
    public int getPortCount() { return portCount; }
    public UUID getClientId() { return clientId; }

    /** GUI 实现此接口以接收同步数据 */
    public interface LogisticsSyncReceiver {
        void onLogisticsSync(LogisticsSyncPacket packet);
    }
}
