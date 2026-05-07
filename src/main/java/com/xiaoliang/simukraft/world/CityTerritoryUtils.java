package com.xiaoliang.simukraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;
import java.util.UUID;

public final class CityTerritoryUtils {
    private CityTerritoryUtils() {
    }

    public static boolean isPosInCityTerritory(ServerLevel level, UUID cityId, BlockPos pos) {
        if (level == null || cityId == null || pos == null) {
            return false;
        }
        CityChunkData chunkData = CityChunkData.get(level);
        UUID owner = chunkData.getChunkOwner(new ChunkPos(pos).toLong());
        return cityId.equals(owner);
    }

    public static boolean areAllPositionsInCityTerritory(ServerLevel level, UUID cityId, Iterable<BlockPos> positions) {
        if (level == null || cityId == null || positions == null) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (!isPosInCityTerritory(level, cityId, pos)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isChunkSetInsideCityTerritory(ServerLevel level, UUID cityId, Set<ChunkPos> chunks) {
        if (level == null || cityId == null || chunks == null) {
            return false;
        }
        CityChunkData chunkData = CityChunkData.get(level);
        for (ChunkPos chunkPos : chunks) {
            UUID owner = chunkData.getChunkOwner(chunkPos.toLong());
            if (!cityId.equals(owner)) {
                return false;
            }
        }
        return true;
    }
}
