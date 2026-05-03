package com.xiaoliang.simukraft.integration.ftb;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * FTB Chunks 软依赖集成。
 * 所有 FTB Chunks API 调用都封装在此类中，通过反射/try-catch 保证在 FTB Chunks 不存在时不会崩溃。
 *
 * <p>功能:</p>
 * <ul>
 *   <li>同步 Simukraft 城市区块认领到 FTB Chunks</li>
 *   <li>同步取消认领</li>
 *   <li>查询 FTB Chunks 的认领状态</li>
 * </ul>
 */
@SuppressWarnings("null")
public class FTBChunksIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static FTBChunksIntegration instance;

    private FTBChunksIntegration() {
    }

    public static FTBChunksIntegration getInstance() {
        if (instance == null) {
            instance = new FTBChunksIntegration();
        }
        return instance;
    }

    public static void init() {
        getInstance();
        LOGGER.info("Simukraft: FTB Chunks integration initialized.");
    }

    /**
     * 将城市区块同步认领到 FTB Chunks。
     * 使用 FTB Chunks 的 API 以玩家（市长）名义认领区块。
     *
     * @param server     服务器实例
     * @param mayorId    市长 UUID
     * @param chunkLongs 区块 long 值集合
     */
    public void claimCityChunks(MinecraftServer server, UUID mayorId, Set<Long> chunkLongs) {
        try {
            dev.ftb.mods.ftbchunks.api.FTBChunksAPI.API api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            if (!api.isManagerLoaded()) {
                LOGGER.warn("Simukraft: FTB Chunks manager not loaded, skipping claim sync");
                return;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(mayorId);
            if (player == null) {
                LOGGER.debug("Simukraft: Mayor {} not online, FTB claim sync deferred", mayorId);
                return;
            }

            ResourceKey<Level> dimension = server.overworld().dimension();

            for (long chunkLong : chunkLongs) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);
                ChunkPos pos = new ChunkPos(cx, cz);

                try {
                    dev.ftb.mods.ftbchunks.api.ClaimResult result = api.claimAsPlayer(player, dimension, pos, false);
                    if (!result.isSuccess()) {
                        LOGGER.debug("Simukraft: FTB claim for ({},{}) returned: {}", cx, cz, result);
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Simukraft: FTB claim failed for ({},{}): {}", cx, cz, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: FTB Chunks claimCityChunks failed", t);
        }
    }

    /**
     * 将城市区块从 FTB Chunks 中取消认领。
     *
     * @param server     服务器实例
     * @param cityId     城市 ID
     * @param chunkLongs 区块 long 值集合
     */
    public void unclaimCityChunks(MinecraftServer server, UUID cityId, Set<Long> chunkLongs) {
        try {
            dev.ftb.mods.ftbchunks.api.FTBChunksAPI.API api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            if (!api.isManagerLoaded()) return;

            dev.ftb.mods.ftbchunks.api.ClaimedChunkManager manager = api.getManager();
            ResourceKey<Level> dimension = server.overworld().dimension();

            for (long chunkLong : chunkLongs) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);

                try {
                    dev.ftb.mods.ftblibrary.math.ChunkDimPos dimPos =
                            new dev.ftb.mods.ftblibrary.math.ChunkDimPos(dimension, cx, cz);
                    dev.ftb.mods.ftbchunks.api.ClaimedChunk claimed = manager.getChunk(dimPos);
                    if (claimed != null) {
                        claimed.unclaim(null, false);
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Simukraft: FTB unclaim failed for ({},{}): {}", cx, cz, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: FTB Chunks unclaimCityChunks failed", t);
        }
    }

    /**
     * 服务器启动时同步所有城市区块到 FTB Chunks。
     */
    public void syncAllCityChunks(MinecraftServer server, Map<UUID, Set<Long>> allCityChunks,
                                   Function<UUID, UUID> cityIdToMayorId) {
        try {
            dev.ftb.mods.ftbchunks.api.FTBChunksAPI.API api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            if (!api.isManagerLoaded()) {
                LOGGER.info("Simukraft: FTB Chunks manager not ready, will sync later");
                return;
            }

            int synced = 0;
            for (Map.Entry<UUID, Set<Long>> entry : allCityChunks.entrySet()) {
                UUID cityId = entry.getKey();
                UUID mayorId = cityIdToMayorId.apply(cityId);
                if (mayorId == null) mayorId = cityId;

                ServerPlayer player = server.getPlayerList().getPlayer(mayorId);
                if (player == null) continue;

                ResourceKey<Level> dimension = server.overworld().dimension();
                for (long chunkLong : entry.getValue()) {
                    int cx = ChunkPos.getX(chunkLong);
                    int cz = ChunkPos.getZ(chunkLong);

                    try {
                        dev.ftb.mods.ftblibrary.math.ChunkDimPos dimPos =
                                new dev.ftb.mods.ftblibrary.math.ChunkDimPos(dimension, cx, cz);
                        if (api.getManager().getChunk(dimPos) == null) {
                            api.claimAsPlayer(player, dimension, new ChunkPos(cx, cz), false);
                            synced++;
                        }
                    } catch (Throwable t) {
                        LOGGER.debug("Simukraft: FTB sync claim skip ({},{}): {}", cx, cz, t.getMessage());
                    }
                }
            }

            if (synced > 0) {
                LOGGER.info("Simukraft: Synced {} chunks to FTB Chunks", synced);
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: FTB Chunks syncAllCityChunks failed", t);
        }
    }

    /**
     * 检查指定区块是否被 FTB Chunks 认领。
     */
    public boolean isChunkClaimedByFTB(MinecraftServer server, int chunkX, int chunkZ) {
        try {
            dev.ftb.mods.ftbchunks.api.FTBChunksAPI.API api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            if (!api.isManagerLoaded()) return false;

            ResourceKey<Level> dimension = server.overworld().dimension();
            dev.ftb.mods.ftblibrary.math.ChunkDimPos dimPos =
                    new dev.ftb.mods.ftblibrary.math.ChunkDimPos(dimension, chunkX, chunkZ);
            return api.getManager().getChunk(dimPos) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
