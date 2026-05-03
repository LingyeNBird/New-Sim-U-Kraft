package com.xiaoliang.simukraft.world.claiming;

import com.mojang.logging.LogUtils;
import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.integration.IntegrationBridge;
import com.xiaoliang.simukraft.integration.ModIntegrationManager;
import com.xiaoliang.simukraft.world.CityChunkData;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.*;

/**
 * 统一城市区块认领管理器。
 * 协调 Simukraft 内部认领系统、FTB Chunks 和 Open Parties And Claims 三者的区块认领。
 *
 * <p>认领流程:</p>
 * <ol>
 *   <li>验证权限和条件（资金、相邻、数量限制等）</li>
 *   <li>在 Simukraft 内部系统中认领</li>
 *   <li>如果 FTB Chunks 存在 → 同步认领到 FTB</li>
 *   <li>如果 OPAC 存在 → 同步认领到 OPAC</li>
 *   <li>广播更新到所有客户端</li>
 * </ol>
 */
public class CityClaimManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CityClaimManager INSTANCE = new CityClaimManager();

    private CityClaimManager() {
    }

    public static CityClaimManager getInstance() {
        return INSTANCE;
    }

    /**
     * 认领单个区块。
     *
     * @param server 服务器实例
     * @param player 操作的玩家
     * @param cityId 城市ID
     * @param chunkPos 要认领的区块
     * @return 认领结果
     */
    public ClaimResult claimChunk(MinecraftServer server, ServerPlayer player, UUID cityId, ChunkPos chunkPos) {
        ServerLevel level = server.overworld();

        CityData cityData = CityData.get(level);
        CityData.CityInfo cityInfo = cityData.getCity(cityId);
        if (cityInfo == null) {
            return ClaimResult.fail(ClaimResult.Problem.NO_CITY);
        }

        if (!cityInfo.canManageCity(player.getUUID()) && !cityInfo.canManageCity(player.getName().getString())) {
            return ClaimResult.fail(ClaimResult.Problem.NO_PERMISSION);
        }

        CityChunkData chunkData = CityChunkData.get(level);
        UUID existingOwner = chunkData.getChunkOwner(chunkPos.toLong());
        if (existingOwner != null) {
            return ClaimResult.fail(ClaimResult.Problem.ALREADY_CLAIMED,
                    existingOwner.equals(cityId) ? "already yours" : "owned by another city");
        }

        if (!isAdjacentToCity(chunkData, cityId, chunkPos)) {
            return ClaimResult.fail(ClaimResult.Problem.NOT_ADJACENT);
        }

        Set<Long> currentChunks = chunkData.getCityChunks(cityId);
        int maxChunks = calculateMaxChunks(cityInfo.getCityLevel());
        if (currentChunks.size() >= maxChunks) {
            return ClaimResult.fail(ClaimResult.Problem.MAX_CHUNKS_REACHED,
                    currentChunks.size() + "/" + maxChunks);
        }

        chunkData.assignSingleChunkToCity(cityId, chunkPos);

        Set<Long> newChunks = Collections.singleton(chunkPos.toLong());
        syncClaimToIntegrations(server, cityId, cityInfo.getMayorId(), newChunks);

        IntegrationBridge.broadcastCityChunksUpdate(server);

        LOGGER.info("Simukraft: Chunk ({}, {}) claimed for city {} by {}",
                chunkPos.x, chunkPos.z, cityId, player.getName().getString());

        return ClaimResult.success();
    }

    /**
     * 取消认领单个区块。
     *
     * @param server 服务器实例
     * @param player 操作的玩家
     * @param cityId 城市ID
     * @param chunkPos 要取消认领的区块
     * @return 认领结果
     */
    public ClaimResult unclaimChunk(MinecraftServer server, ServerPlayer player, UUID cityId, ChunkPos chunkPos) {
        ServerLevel level = server.overworld();

        CityData cityData = CityData.get(level);
        CityData.CityInfo cityInfo = cityData.getCity(cityId);
        if (cityInfo == null) {
            return ClaimResult.fail(ClaimResult.Problem.NO_CITY);
        }

        if (!cityInfo.canManageCity(player.getUUID()) && !cityInfo.canManageCity(player.getName().getString())) {
            return ClaimResult.fail(ClaimResult.Problem.NO_PERMISSION);
        }

        CityChunkData chunkData = CityChunkData.get(level);
        UUID owner = chunkData.getChunkOwner(chunkPos.toLong());
        if (!cityId.equals(owner)) {
            return ClaimResult.fail(ClaimResult.Problem.ALREADY_CLAIMED, "not owned by your city");
        }

        Set<Long> chunks = new HashSet<>(chunkData.getCityChunks(cityId));
        chunks.remove(chunkPos.toLong());
        chunkData.removeCityChunks(cityId);
        for (long cl : chunks) {
            chunkData.assignSingleChunkToCity(cityId, new ChunkPos(cl));
        }

        // 同步到集成模组
        Set<Long> removedChunks = Collections.singleton(chunkPos.toLong());
        IntegrationBridge.onCityChunksUnclaimed(server, cityId, removedChunks);

        LOGGER.info("Simukraft: Chunk ({}, {}) unclaimed from city {}",
                chunkPos.x, chunkPos.z, cityId);

        return ClaimResult.success();
    }

    /**
     * 批量认领区块（用于城市创建时的初始 9 区块分配）。
     */
    public ClaimResult claimArea(MinecraftServer server, UUID cityId, UUID mayorId, ChunkPos center) {
        ServerLevel level = server.overworld();
        CityChunkData chunkData = CityChunkData.get(level);

        // 验证区域可用
        if (!chunkData.isAreaAvailable(center)) {
            return ClaimResult.fail(ClaimResult.Problem.ALREADY_CLAIMED);
        }

        // 分配 3x3 区域
        chunkData.assignAreaToCity(cityId, center);

        // 收集所有新认领的区块
        Set<Long> newChunks = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                newChunks.add(ChunkPos.asLong(center.x + dx, center.z + dz));
            }
        }

        // 同步到集成模组
        syncClaimToIntegrations(server, cityId, mayorId, newChunks);
        IntegrationBridge.broadcastCityChunksUpdate(server);

        LOGGER.info("Simukraft: Area 3x3 around ({}, {}) claimed for city {}",
                center.x, center.z, cityId);

        return ClaimResult.success();
    }

    /**
     * 同步认领到集成模组（FTB Chunks + OPAC）。
     */
    private void syncClaimToIntegrations(MinecraftServer server, UUID cityId, UUID mayorId, Set<Long> chunkLongs) {
        // OPAC 同步
        if (ModIntegrationManager.isOpenPACPresent() && ServerConfig.isOpacClaimsEnabled()) {
            try {
                com.xiaoliang.simukraft.integration.pac.OPACIntegration.getInstance()
                        .claimCityChunks(server, mayorId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: OPAC sync claim failed", t);
            }
        }

        // FTB Chunks 同步
        if (ModIntegrationManager.isFTBChunksPresent()) {
            try {
                com.xiaoliang.simukraft.integration.ftb.FTBChunksIntegration.getInstance()
                        .claimCityChunks(server, mayorId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: FTB Chunks sync claim failed", t);
            }
        }
    }

    /**
     * 检查区块是否与城市已有区块相邻。
     */
    private boolean isAdjacentToCity(CityChunkData chunkData, UUID cityId, ChunkPos chunkPos) {
        Set<Long> cityChunks = chunkData.getCityChunks(cityId);
        if (cityChunks.isEmpty()) return true; // 没有区块时允许任意位置

        long north = ChunkPos.asLong(chunkPos.x, chunkPos.z - 1);
        long south = ChunkPos.asLong(chunkPos.x, chunkPos.z + 1);
        long east = ChunkPos.asLong(chunkPos.x + 1, chunkPos.z);
        long west = ChunkPos.asLong(chunkPos.x - 1, chunkPos.z);

        return cityChunks.contains(north) || cityChunks.contains(south)
                || cityChunks.contains(east) || cityChunks.contains(west);
    }

    /**
     * 根据城市等级计算最大可拥有区块数。
     * 等级 0-1: 初始 3x3 = 9 区块
     * 等级 2-10: 每级扩展一圈, 边长 = 2*level+1, 最大 = (2*level+1)^2
     * 等级 11+: 无限制
     */
    private int calculateMaxChunks(int cityLevel) {
        if (cityLevel < 2) return 9;
        if (cityLevel >= 11) return Integer.MAX_VALUE;
        int side = 2 * cityLevel + 1;
        return side * side;
    }

    /**
     * 服务器启动时同步所有城市区块到集成模组。
     */
    public void syncAllOnServerStart(MinecraftServer server) {
        IntegrationBridge.onServerStarted(server);
    }
}
