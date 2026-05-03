package com.xiaoliang.simukraft.integration;

import com.mojang.logging.LogUtils;
import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;

/**
 * 集成桥接层。
 * 统一处理 Simukraft 内部事件 → 外部模组同步调用。
 * 所有外部模组调用都包裹在 try-catch 中确保软依赖安全。
 */
public class IntegrationBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private IntegrationBridge() {
    }

    /**
     * 城市区块被认领时调用。
     * 同步到 OPAC + FTB Chunks，然后广播更新。
     */
    public static void onCityChunksClaimed(MinecraftServer server, UUID cityId, UUID mayorId, Set<Long> chunkLongs) {
        // OPAC 同步
        if (ModIntegrationManager.isOpenPACPresent() && ServerConfig.isOpacClaimsEnabled()) {
            try {
                com.xiaoliang.simukraft.integration.pac.OPACIntegration.getInstance()
                        .claimCityChunks(server, mayorId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onCityChunksClaimed OPAC integration error", t);
            }
        }

        // FTB Chunks 同步
        if (ModIntegrationManager.isFTBChunksPresent()) {
            try {
                com.xiaoliang.simukraft.integration.ftb.FTBChunksIntegration.getInstance()
                        .claimCityChunks(server, mayorId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onCityChunksClaimed FTB integration error", t);
            }
        }

        broadcastCityChunksUpdate(server);
    }

    /**
     * 城市区块被取消认领时调用。
     * 同步到 OPAC + FTB Chunks，然后广播更新。
     */
    public static void onCityChunksUnclaimed(MinecraftServer server, UUID cityId, Set<Long> chunkLongs) {
        // OPAC 取消同步
        if (ModIntegrationManager.isOpenPACPresent() && ServerConfig.isOpacClaimsEnabled()) {
            try {
                com.xiaoliang.simukraft.integration.pac.OPACIntegration.getInstance()
                        .unclaimCityChunks(server, cityId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onCityChunksUnclaimed OPAC integration error", t);
            }
        }

        // FTB Chunks 取消同步
        if (ModIntegrationManager.isFTBChunksPresent()) {
            try {
                com.xiaoliang.simukraft.integration.ftb.FTBChunksIntegration.getInstance()
                        .unclaimCityChunks(server, cityId, chunkLongs);
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onCityChunksUnclaimed FTB integration error", t);
            }
        }

        broadcastCityChunksUpdate(server);
    }

    /**
     * 服务器启动时同步所有城市区块到外部模组。
     */
    public static void onServerStarted(MinecraftServer server) {
        // OPAC 全量同步
        if (ModIntegrationManager.isOpenPACPresent() && ServerConfig.isOpacClaimsEnabled()) {
            try {
                com.xiaoliang.simukraft.world.CityChunkData cityChunkData =
                        com.xiaoliang.simukraft.world.CityChunkData.get(server.overworld());
                CityData cityData = CityData.get(server.overworld());
                com.xiaoliang.simukraft.integration.pac.OPACIntegration.getInstance()
                        .syncAllCityChunks(server, cityChunkData.getAllCityChunks(),
                                cityId -> {
                                    CityData.CityInfo info = cityData.getCity(cityId);
                                    return info != null ? info.getMayorId() : cityId;
                                });
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onServerStarted OPAC sync error", t);
            }
        }

        // FTB Chunks 全量同步
        if (ModIntegrationManager.isFTBChunksPresent()) {
            try {
                com.xiaoliang.simukraft.world.CityChunkData cityChunkData =
                        com.xiaoliang.simukraft.world.CityChunkData.get(server.overworld());
                CityData cityData = CityData.get(server.overworld());
                com.xiaoliang.simukraft.integration.ftb.FTBChunksIntegration.getInstance()
                        .syncAllCityChunks(server, cityChunkData.getAllCityChunks(),
                                cityId -> {
                                    CityData.CityInfo info = cityData.getCity(cityId);
                                    return info != null ? info.getMayorId() : cityId;
                                });
            } catch (Throwable t) {
                LOGGER.error("Simukraft: onServerStarted FTB sync error", t);
            }
        }
    }

    /**
     * 广播城市区块更新到所有客户端。
     */
    public static void broadcastCityChunksUpdate(MinecraftServer server) {
        try {
            com.xiaoliang.simukraft.network.NetworkManager.broadcastAllCityChunks(server);
        } catch (Throwable t) {
            LOGGER.error("Simukraft: broadcastCityChunksUpdate error", t);
        }
    }
}
