package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

public class SyncAllCityCoresPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<UUID, CoreInfo> allCityCores;

    public static class CoreInfo {
        public final BlockPos pos;
        public final String cityName;

        public CoreInfo(BlockPos pos, String cityName) {
            this.pos = pos;
            this.cityName = cityName;
        }
    }

    public SyncAllCityCoresPacket(Map<UUID, CoreInfo> allCityCores) {
        this.allCityCores = allCityCores;
    }

    public SyncAllCityCoresPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.allCityCores = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            UUID cityId = Objects.requireNonNull(buf.readUUID());
            BlockPos pos = Objects.requireNonNull(buf.readBlockPos());
            String cityName = Objects.requireNonNull(buf.readUtf(256));
            allCityCores.put(cityId, new CoreInfo(pos, cityName));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(allCityCores.size());
        for (Map.Entry<UUID, CoreInfo> entry : allCityCores.entrySet()) {
            UUID cityId = Objects.requireNonNull(entry.getKey());
            CoreInfo coreInfo = Objects.requireNonNull(entry.getValue());
            buf.writeUUID(cityId);
            buf.writeBlockPos(Objects.requireNonNull(coreInfo.pos));
            buf.writeUtf(Objects.requireNonNull(coreInfo.cityName), 256);
        }
    }

    public static SyncAllCityCoresPacket decode(FriendlyByteBuf buf) {
        return new SyncAllCityCoresPacket(buf);
    }

    public static void handle(SyncAllCityCoresPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                LOGGER.info("[SyncAllCityCores] Received {} city cores from server", message.allCityCores.size());
                ClientCityChunkData ccd = ClientCityChunkData.getInstance();
                Map<UUID, ClientCityChunkData.CityCoreCacheEntry> cores = new HashMap<>();
                for (Map.Entry<UUID, CoreInfo> entry : message.allCityCores.entrySet()) {
                    LOGGER.info("[SyncAllCityCores] City: {} at ({}, {}, {}), name: {}",
                            entry.getKey(), entry.getValue().pos.getX(),
                            entry.getValue().pos.getY(), entry.getValue().pos.getZ(),
                            entry.getValue().cityName);
                    cores.put(entry.getKey(), new ClientCityChunkData.CityCoreCacheEntry(
                            entry.getValue().pos, entry.getValue().cityName));
                }
                ccd.updateAllCityCores(cores);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
