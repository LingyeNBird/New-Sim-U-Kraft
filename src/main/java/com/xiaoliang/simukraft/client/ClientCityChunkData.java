package com.xiaoliang.simukraft.client;

import net.minecraft.core.BlockPos;

import java.util.*;

public class ClientCityChunkData {
    private static final ClientCityChunkData INSTANCE = new ClientCityChunkData();
    private Set<Long> currentCityChunks = new HashSet<>();
    private Map<UUID, Set<Long>> allCityChunks = new HashMap<>();
    private Set<Long> allOwnedChunks = new HashSet<>();
    private Map<Long, UUID> chunkOwnerIndex = new HashMap<>();
    private Map<UUID, CityCoreCacheEntry> allCityCores = new HashMap<>();
    private UUID cityId;

    public static class CityCoreCacheEntry {
        private final BlockPos pos;
        private final String cityName;

        public CityCoreCacheEntry(BlockPos pos, String cityName) {
            this.pos = pos;
            this.cityName = cityName;
        }

        public BlockPos getPos() {
            return pos;
        }

        public String getCityName() {
            return cityName;
        }
    }

    private ClientCityChunkData() {
    }

    public static ClientCityChunkData getInstance() {
        return INSTANCE;
    }

    /**
     * 更新当前城市的区块数据和城市ID（兼容旧版本）
     * @param cityId 城市ID
     * @param chunks 区块列表
     */
    public void updateCityChunks(UUID cityId, Set<Long> chunks) {
        this.cityId = cityId;
        this.currentCityChunks = new HashSet<>(chunks);
        this.allCityChunks.put(cityId, new HashSet<>(chunks));
        rebuildChunkIndexes();
    }

    /**
     * 更新所有城市的区块数据
     * @param currentCityId 当前城市ID
     * @param allChunks 所有城市的区块数据
     */
    public void updateAllCityChunks(UUID currentCityId, Map<UUID, Set<Long>> allChunks) {
        this.cityId = currentCityId;
        this.allCityChunks = new HashMap<>(allChunks);
        this.currentCityChunks = new HashSet<>(allChunks.getOrDefault(currentCityId, Set.of()));
        rebuildChunkIndexes();
    }

    /**
     * 获取当前城市的区块列表
     * @return 区块列表
     */
    public Set<Long> getCurrentCityChunks() {
        return currentCityChunks;
    }

    /**
     * 获取所有城市的区块数据
     * @return 所有城市的区块数据
     */
    public Map<UUID, Set<Long>> getAllCityChunks() {
        return allCityChunks;
    }

    /**
     * 获取当前城市ID
     * @return 城市ID
     */
    public UUID getCityId() {
        return cityId;
    }

    /**
     * 检查区块是否属于当前城市
     * @param chunkLong 区块的long表示
     * @return 如果区块属于当前城市则返回true，否则返回false
     */
    public boolean isChunkInCurrentCity(long chunkLong) {
        return currentCityChunks.contains(chunkLong);
    }

    /**
     * 检查区块是否被任何城市占有（包括其他城市）
     * @param chunkLong 区块的long表示
     * @return 如果区块被任何城市占有则返回true，否则返回false
     */
    public boolean isChunkOwned(long chunkLong) {
        return allOwnedChunks.contains(chunkLong);
    }

    /**
     * 获取区块所属的城市ID
     * @param chunkLong 区块的long表示
     * @return 城市ID，如果区块不属于任何城市则返回null
     */
    public UUID getChunkOwner(long chunkLong) {
        return chunkOwnerIndex.get(chunkLong);
    }

    /**
     * 更新所有城市核心位置数据
     * @param cores 所有城市的核心位置和名称
     */
    public void updateAllCityCores(Map<UUID, CityCoreCacheEntry> cores) {
        this.allCityCores = new HashMap<>(cores);
    }

    /**
     * 获取所有城市核心位置数据
     * @return 城市ID到核心位置的映射
     */
    public Map<UUID, CityCoreCacheEntry> getAllCityCores() {
        return allCityCores;
    }

    /**
     * 清除当前城市的区块数据
     */
    public void clear() {
        currentCityChunks.clear();
        allCityChunks.clear();
        allOwnedChunks.clear();
        chunkOwnerIndex.clear();
        allCityCores.clear();
        cityId = null;
    }

    private void rebuildChunkIndexes() {
        allOwnedChunks = new HashSet<>();
        chunkOwnerIndex = new HashMap<>();

        for (Map.Entry<UUID, Set<Long>> entry : allCityChunks.entrySet()) {
            UUID ownerId = entry.getKey();
            for (Long chunkLong : entry.getValue()) {
                allOwnedChunks.add(chunkLong);
                chunkOwnerIndex.put(chunkLong, ownerId);
            }
        }
    }
}
