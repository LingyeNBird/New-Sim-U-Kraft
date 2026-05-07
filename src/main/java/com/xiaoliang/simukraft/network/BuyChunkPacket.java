package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.MoneyManager;
import com.xiaoliang.simukraft.world.CityChunkData;
import com.xiaoliang.simukraft.world.CityData;
import com.xiaoliang.simukraft.world.claiming.CityClaimManager;
import com.xiaoliang.simukraft.world.claiming.ClaimResult;
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
    private static final double CHUNK_COST = 10.0;

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
            if (player == null || player.getServer() == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            CityData cityData = CityData.get(level);
            CityChunkData cityChunkData = CityChunkData.get(level);
            CityData.CityInfo cityInfo = cityData.getCity(message.cityId);

            boolean success = false;
            String errorMessage = "";

            if (cityInfo == null) {
                errorMessage = "message.simukraft.buy_chunk.error.no_city";
            } else if (cityInfo.getCityLevel() < 2) {
                errorMessage = "message.simukraft.buy_chunk.error.city_level_locked";
            } else if (!MoneyManager.hasEnoughMoney(player, CHUNK_COST)) {
                errorMessage = "message.simukraft.buy_chunk.error.insufficient_funds";
            } else {
                ClaimResult claimResult = CityClaimManager.getInstance().claimChunk(player.getServer(), player, message.cityId, message.chunkPos);
                if (claimResult.isSuccess()) {
                    MoneyManager.deductMoney(player, CHUNK_COST);
                    success = true;
                } else {
                    errorMessage = toBuyChunkErrorKey(claimResult);
                }
            }

            Set<Long> cityChunks = cityChunkData.getCityChunks(message.cityId);
            BuyChunkResponsePacket responsePacket = new BuyChunkResponsePacket(
                    success,
                    message.cityId,
                    new HashSet<>(cityChunks),
                    errorMessage,
                    message.chunkPos.toLong(),
                    CHUNK_COST
            );
            NetworkManager.sendToPlayer(responsePacket, player);
        });
        ctx.get().setPacketHandled(true);
    }

    private static String toBuyChunkErrorKey(ClaimResult claimResult) {
        ClaimResult.Problem problem = claimResult.getProblem();
        if (problem == null) {
            return "message.simukraft.buy_chunk.error.internal_error";
        }
        return switch (problem) {
            case ALREADY_CLAIMED -> "message.simukraft.buy_chunk.error.chunk_occupied";
            case NOT_ADJACENT -> "message.simukraft.buy_chunk.error.not_adjacent";
            case MAX_CHUNKS_REACHED -> "message.simukraft.buy_chunk.error.max_chunks";
            case NO_PERMISSION -> "message.simukraft.buy_chunk.error.no_permission";
            case NO_CITY -> "message.simukraft.buy_chunk.error.no_city";
            case WRONG_DIMENSION -> "message.simukraft.buy_chunk.error.wrong_dimension";
            case PROTECTED -> "message.simukraft.buy_chunk.error.protected";
            default -> "message.simukraft.buy_chunk.error.internal_error";
        };
    }
}
