package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.MoneyManager;
import com.xiaoliang.simukraft.world.CityChunkData;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class BuyChunkPacket {
    private final UUID cityId;
    private final ChunkPos chunkPos;

    public BuyChunkPacket(UUID cityId, ChunkPos chunkPos) {
        this.cityId = cityId;
        this.chunkPos = chunkPos;
    }

    public BuyChunkPacket(FriendlyByteBuf buf) {
        this.cityId = buf.readUUID();
        this.chunkPos = buf.readChunkPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.cityId);
        buf.writeChunkPos(this.chunkPos);
    }

    public static BuyChunkPacket decode(FriendlyByteBuf buf) {
        return new BuyChunkPacket(buf);
    }

    public static void handle(BuyChunkPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                
                // 获取城市区块数据
                CityChunkData cityChunkData = CityChunkData.get(level);
                CityData cityData = CityData.get(level);
                
                // 检查区块是否可用
                long chunkLong = ChunkPos.asLong(message.chunkPos.x, message.chunkPos.z);
                boolean isAvailable = cityChunkData.getChunkOwner(chunkLong) == null;
                String errorMessage = "";
                boolean success = false;
                
                // 计算购买成本
                double cost = 10.0;
                
                if (!isAvailable) {
                    errorMessage = "message.simukraft.buy_chunk.error.chunk_occupied";
                } else {
                    // 检查玩家是否有足够资金
                    if (!MoneyManager.hasEnoughMoney(player, cost)) {
                        errorMessage = "message.simukraft.buy_chunk.error.insufficient_funds";
                    } else {
                        // 扣除资金
                        MoneyManager.deductMoney(player, cost);
                        
                        // 分配单个区块给城市
                        cityChunkData.assignSingleChunkToCity(message.cityId, message.chunkPos);
                        
                        // 调用模组集成：认领区块并广播更新
                        // 获取市长UUID
                        CityData.CityInfo cityInfo = cityData.getCity(message.cityId);
                        java.util.UUID mayorId = cityInfo != null ? cityInfo.getMayorId() : message.cityId;
                        com.xiaoliang.simukraft.integration.IntegrationBridge.onCityChunksClaimed(
                            player.getServer(),
                            message.cityId,
                            mayorId,
                            java.util.Collections.singleton(message.chunkPos.toLong())
                        );
                        
                        success = true;
                    }
                }
                
                // 获取更新后的城市区块数据
                Set<Long> cityChunks = cityChunkData.getCityChunks(message.cityId);
                
                // 发送购买结果到客户端
                BuyChunkResponsePacket responsePacket = new BuyChunkResponsePacket(
                        success, 
                        message.cityId, 
                        new HashSet<>(cityChunks), 
                        errorMessage,
                        message.chunkPos.toLong(),
                        cost
                );
                NetworkManager.sendToPlayer(responsePacket, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}