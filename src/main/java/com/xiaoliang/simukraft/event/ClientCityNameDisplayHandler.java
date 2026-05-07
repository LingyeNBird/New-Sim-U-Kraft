package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "simukraft", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCityNameDisplayHandler {
    private static UUID lastCityId;
    private static long lastChunkLong = Long.MIN_VALUE;

    private ClientCityNameDisplayHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastCityId = null;
            lastChunkLong = Long.MIN_VALUE;
            return;
        }

        ChunkPos chunkPos = player.chunkPosition();
        long currentChunkLong = chunkPos.toLong();
        if (currentChunkLong == lastChunkLong) {
            return;
        }
        lastChunkLong = currentChunkLong;

        ClientCityChunkData cityChunkData = ClientCityChunkData.getInstance();
        UUID cityId = cityChunkData.getChunkOwner(currentChunkLong);
        if (cityId == null || cityId.equals(lastCityId)) {
            lastCityId = cityId;
            return;
        }

        Map<UUID, ClientCityChunkData.CityCoreCacheEntry> cores = cityChunkData.getAllCityCores();
        ClientCityChunkData.CityCoreCacheEntry entry = cores.get(cityId);
        if (entry != null && entry.getCityName() != null && !entry.getCityName().isBlank()) {
            com.xiaoliang.simukraft.client.ClientToastHUDOverlay.showCityName(entry.getCityName(), player.getUUID());
        }
        lastCityId = cityId;
    }
}
