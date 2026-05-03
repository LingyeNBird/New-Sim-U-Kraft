package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.world.CityChunkData;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

public class GetCityChunksPacket {
    private final BlockPos cityCorePos;

    public GetCityChunksPacket(BlockPos cityCorePos) {
        this.cityCorePos = cityCorePos;
    }

    public GetCityChunksPacket(FriendlyByteBuf buf) {
        this.cityCorePos = Objects.requireNonNull(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(this.cityCorePos));
    }

    public static GetCityChunksPacket decode(FriendlyByteBuf buf) {
        return new GetCityChunksPacket(buf);
    }

    public static void handle(GetCityChunksPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                
                // 获取城市数据
                CityData cityData = CityData.get(level);
                CityData.CityInfo cityInfo = cityData.getCityByCorePos(message.cityCorePos);
                
                if (cityInfo != null) {
                    // 获取城市区块数据
                    CityChunkData cityChunkData = CityChunkData.get(level);
                    
                    // 获取所有城市的区块数据
                    Map<UUID, Set<Long>> allCityChunks = new HashMap<>();
                    
                    // 添加当前城市的区块
                    Set<Long> currentCityChunks = cityChunkData.getCityChunks(cityInfo.getCityId());
                    allCityChunks.put(cityInfo.getCityId(), new HashSet<>(currentCityChunks));
                    
                    // 添加其他城市的区块
                    for (CityData.CityInfo otherCity : cityData.getAllCities()) {
                        if (!otherCity.getCityId().equals(cityInfo.getCityId())) {
                            Set<Long> otherCityChunks = cityChunkData.getCityChunks(otherCity.getCityId());
                            if (!otherCityChunks.isEmpty()) {
                                allCityChunks.put(otherCity.getCityId(), new HashSet<>(otherCityChunks));
                            }
                        }
                    }
                    
                    // 发送所有城市区块数据到客户端
                    CityChunksResponsePacket responsePacket = new CityChunksResponsePacket(cityInfo.getCityId(), allCityChunks);
                    NetworkManager.sendToPlayer(responsePacket, player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
