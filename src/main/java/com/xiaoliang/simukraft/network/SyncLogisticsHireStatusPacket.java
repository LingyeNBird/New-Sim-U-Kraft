package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.LogisticsBoxData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步物流盒子雇佣状态 - 照抄 SyncBuildBoxHireStatusPacket
 */
public class SyncLogisticsHireStatusPacket {
    private final BlockPos serverBoxPos;
    private final UUID serverNpcUuid;
    private final BlockPos clientBoxPos;
    private final UUID clientNpcUuid;

    public SyncLogisticsHireStatusPacket(BlockPos serverBoxPos, UUID serverNpcUuid,
                                          BlockPos clientBoxPos, UUID clientNpcUuid) {
        this.serverBoxPos = serverBoxPos;
        this.serverNpcUuid = serverNpcUuid;
        this.clientBoxPos = clientBoxPos;
        this.clientNpcUuid = clientNpcUuid;
    }

    public SyncLogisticsHireStatusPacket(FriendlyByteBuf buf) {
        this.serverBoxPos = buf.readBoolean() ? buf.readBlockPos() : null;
        this.serverNpcUuid = buf.readBoolean() ? buf.readUUID() : null;
        this.clientBoxPos = buf.readBoolean() ? buf.readBlockPos() : null;
        this.clientNpcUuid = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(serverBoxPos != null);
        if (serverBoxPos != null) buf.writeBlockPos(serverBoxPos);
        buf.writeBoolean(serverNpcUuid != null);
        if (serverNpcUuid != null) buf.writeUUID(serverNpcUuid);
        buf.writeBoolean(clientBoxPos != null);
        if (clientBoxPos != null) buf.writeBlockPos(clientBoxPos);
        buf.writeBoolean(clientNpcUuid != null);
        if (clientNpcUuid != null) buf.writeUUID(clientNpcUuid);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::applyClient));
        ctx.get().setPacketHandled(true);
    }

    private void applyClient() {
        if (serverBoxPos != null) {
            if (serverNpcUuid != null) {
                LogisticsBoxData.setServerBoxHired(serverBoxPos, serverNpcUuid);
            } else {
                LogisticsBoxData.clearServerBoxHired(serverBoxPos);
            }
        }
        if (clientBoxPos != null) {
            if (clientNpcUuid != null) {
                LogisticsBoxData.setClientBoxHired(clientBoxPos, clientNpcUuid);
            } else {
                LogisticsBoxData.clearClientBoxHired(clientBoxPos);
            }
        }
    }
}
