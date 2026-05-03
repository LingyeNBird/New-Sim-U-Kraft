package com.xiaoliang.simukraft.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;

public class CityChunkData extends SavedData {
    private static final String DATA_NAME = "simukraft_city_chunk_data";
    private final Map<UUID, Set<Long>> cityChunks = new HashMap<>(); // cityUUID -> chunkPos as long
    private final Map<Long, UUID> chunkCityMap = new HashMap<>(); // chunkPos as long -> cityUUID

    public CityChunkData() {}

    public static CityChunkData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(CityChunkData::load, CityChunkData::new, DATA_NAME);
    }

    // 检查以中心区块为中心的9个区块是否被占用
    public boolean isAreaAvailable(ChunkPos centerChunk) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                if (chunkCityMap.containsKey(chunkPos.toLong())) {
                    return false;
                }
            }
        }
        return true;
    }

    // 分配以中心区块为中心的9个区块给城市（用于城市创建）
    public void assignAreaToCity(UUID cityUUID, ChunkPos centerChunk) {
        Set<Long> chunks = cityChunks.computeIfAbsent(cityUUID, k -> new HashSet<>());
        // 分配中心区块及其周围的8个区块，共9个区块
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                long chunkLong = chunkPos.toLong();
                chunks.add(chunkLong);
                chunkCityMap.put(chunkLong, cityUUID);
            }
        }
        setDirty();
    }
    
    // 分配单个区块给城市（用于购买区块）
    public void assignSingleChunkToCity(UUID cityUUID, ChunkPos chunkPos) {
        Set<Long> chunks = cityChunks.computeIfAbsent(cityUUID, k -> new HashSet<>());
        long chunkLong = chunkPos.toLong();
        chunks.add(chunkLong);
        chunkCityMap.put(chunkLong, cityUUID);
        setDirty();
    }

    // 移除城市的所有区块
    public void removeCityChunks(UUID cityUUID) {
        Set<Long> chunks = cityChunks.remove(cityUUID);
        if (chunks != null) {
            for (long chunkLong : chunks) {
                chunkCityMap.remove(chunkLong);
            }
            setDirty();
        }
    }

    // 获取区块所属城市
    public UUID getChunkOwner(long chunkLong) {
        return chunkCityMap.get(chunkLong);
    }

    // 获取城市的所有区块
    public Set<Long> getCityChunks(UUID cityUUID) {
        return cityChunks.getOrDefault(cityUUID, Collections.emptySet());
    }

    public Map<UUID, Set<Long>> getAllCityChunks() {
        return Collections.unmodifiableMap(cityChunks);
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag cityList = new ListTag();
        for (Map.Entry<UUID, Set<Long>> entry : cityChunks.entrySet()) {
            CompoundTag cityTag = new CompoundTag();
            cityTag.putUUID("cityId", Objects.requireNonNull(entry.getKey()));
            ListTag chunksList = new ListTag();
            for (long chunkLong : entry.getValue()) {
                chunksList.add(LongTag.valueOf(chunkLong));
            }
            cityTag.put("chunks", chunksList);
            cityList.add(cityTag);
        }
        tag.put("cityChunks", cityList);
        return tag;
    }

    public static CityChunkData load(CompoundTag tag) {
        CityChunkData data = new CityChunkData();
        ListTag cityList = tag.getList("cityChunks", Tag.TAG_COMPOUND);
        for (Tag cityTag : cityList) {
            CompoundTag cityCompound = (CompoundTag) cityTag;
            UUID cityId = cityCompound.getUUID("cityId");
            ListTag chunksList = cityCompound.getList("chunks", Tag.TAG_LONG);
            Set<Long> chunks = new HashSet<>();
            for (Tag chunkTag : chunksList) {
                long chunkLong = ((LongTag) chunkTag).getAsLong();
                chunks.add(chunkLong);
                data.chunkCityMap.put(chunkLong, cityId);
            }
            data.cityChunks.put(cityId, chunks);
        }
        return data;
    }
}
