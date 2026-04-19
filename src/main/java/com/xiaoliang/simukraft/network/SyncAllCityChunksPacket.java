package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

public class SyncAllCityChunksPacket {
    private final Map<UUID, Set<Long>> allCityChunks;

    public SyncAllCityChunksPacket(Map<UUID, Set<Long>> allCityChunks) {
        this.allCityChunks = allCityChunks;
    }

    public SyncAllCityChunksPacket(FriendlyByteBuf buf) {
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
                UUID currentCityId = ccd.getCityId();
                ccd.updateAllCityChunks(currentCityId, message.allCityChunks);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
