package com.xiaoliang.simukraft.integration.pac;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("null")
public class OPACIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    private static OPACIntegration instance;

    private OPACIntegration() {
    }

    public static OPACIntegration getInstance() {
        if (instance == null) {
            instance = new OPACIntegration();
        }
        return instance;
    }

    public static void init() {
        getInstance();
        LOGGER.info("Simukraft: OPAC integration initialized.");
    }

    public void claimCityChunks(MinecraftServer server, UUID mayorId, Set<Long> chunkLongs) {
        try {
            OpenPACServerAPI pacApi = OpenPACServerAPI.get(server);
            IServerClaimsManagerAPI claimsManager = pacApi.getServerClaimsManager();
            for (long chunkLong : chunkLongs) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);
                try {
                    claimsManager.claim(OVERWORLD, mayorId, 0, cx, cz, true);
                } catch (Throwable t) {
                    LOGGER.debug("Simukraft: Could not claim chunk ({},{}) for mayor {}: {}", cx, cz, mayorId, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: OPAC claimCityChunks failed for mayor {}", mayorId, t);
        }
    }

    public void unclaimCityChunks(MinecraftServer server, UUID cityId, Set<Long> chunkLongs) {
        try {
            OpenPACServerAPI pacApi = OpenPACServerAPI.get(server);
            IServerClaimsManagerAPI claimsManager = pacApi.getServerClaimsManager();
            for (long chunkLong : chunkLongs) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);
                try {
                    claimsManager.unclaim(OVERWORLD, cx, cz);
                } catch (Throwable t) {
                    LOGGER.debug("Simukraft: Could not unclaim chunk ({},{}) for city {}: {}", cx, cz, cityId, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: OPAC unclaimCityChunks failed for city {}", cityId, t);
        }
    }

    public void syncAllCityChunks(MinecraftServer server, Map<UUID, Set<Long>> allCityChunks,
                                   java.util.function.Function<UUID, UUID> cityIdToMayorId) {
        try {
            OpenPACServerAPI pacApi = OpenPACServerAPI.get(server);
            IServerClaimsManagerAPI claimsManager = pacApi.getServerClaimsManager();
            for (Map.Entry<UUID, Set<Long>> entry : allCityChunks.entrySet()) {
                UUID cityId = entry.getKey();
                UUID mayorId = cityIdToMayorId.apply(cityId);
                if (mayorId == null) mayorId = cityId; // 退退退居，保证不为 null
                for (long chunkLong : entry.getValue()) {
                    int cx = ChunkPos.getX(chunkLong);
                    int cz = ChunkPos.getZ(chunkLong);
                    if (claimsManager.get(OVERWORLD, cx, cz) == null) {
                        try {
                            claimsManager.claim(OVERWORLD, mayorId, 0, cx, cz, false);
                        } catch (Throwable t) {
                            LOGGER.debug("Simukraft: OPAC sync claim skip ({},{}): {}", cx, cz, t.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Simukraft: OPAC syncAllCityChunks failed", t);
        }
    }
}
