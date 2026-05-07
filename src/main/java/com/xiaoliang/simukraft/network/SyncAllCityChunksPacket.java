package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

public class SyncAllCityChunksPacket {
    private final UUID currentCityId;
    private final Map<UUID, Set<Long>> allCityChunks;

    public SyncAllCityChunksPacket(UUID currentCityId, Map<UUID, Set<Long>> allCityChunks) {
        this.currentCityId = currentCityId;
        this.allCityChunks = allCityChunks;
    }

    public SyncAllCityChunksPacket(FriendlyByteBuf buf) {
        this.currentCityId = buf.readUUID();
        int cityCount = buf.readInt();
        this.allCityChunks = new HashMap<>(cityCount);
        for (int i = 0; i < cityCount; i++) {
            UUID cityId = Objects.requireNonNull(buf.readUUID());
            int chunkCount = buf.readInt();
            Set<Long> chunks = new HashSet<>(chunkCount);
            for (int j = 0; j < chunkCount; j++) {
                chunks.add(buf.readLong());
            }
            allCityChunks.put(cityId, chunks);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(currentCityId != null ? currentCityId : new UUID(0, 0));
        buf.writeInt(allCityChunks.size());
        for (Map.Entry<UUID, Set<Long>> entry : allCityChunks.entrySet()) {
            buf.writeUUID(Objects.requireNonNull(entry.getKey()));
            buf.writeInt(entry.getValue().size());
            for (long chunk : entry.getValue()) {
                buf.writeLong(chunk);
            }
        }
    }

    public static SyncAllCityChunksPacket decode(FriendlyByteBuf buf) {
        return new SyncAllCityChunksPacket(buf);
    }

    public static void handle(SyncAllCityChunksPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientCityChunkData ccd = ClientCityChunkData.getInstance();
                // 使用服务端发送的当前城市ID，而不是客户端缓存的
                UUID cityId = message.currentCityId;
                if (cityId != null && cityId.equals(new UUID(0, 0))) {
                    cityId = null;
                }
                ccd.updateAllCityChunks(cityId, message.allCityChunks);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
