package com.xiaoliang.simukraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * 建筑控制盒数据管理器
 * 持久化存储控制盒位置和城市ID、建筑名称的映射关系
 */
public class ConstructionBoxData extends SavedData {
    private static final String DATA_NAME = "simukraft_construction_box_data";
    
    // 控制盒信息类
    public static class BoxInfo {
        public final UUID cityId;
        public final String buildingName;      // 建筑显示名称（中文）
        public final String buildingFileName;  // 建筑文件名称（英文，如"2br"）
        
        public BoxInfo(UUID cityId, String buildingName, String buildingFileName) {
            this.cityId = cityId;
            this.buildingName = buildingName;
            this.buildingFileName = buildingFileName;
        }
        
        // 兼容旧版本构造函数
        public BoxInfo(UUID cityId, String buildingName) {
            this(cityId, buildingName, buildingName);
        }
    }
    
    // 待分配的控制盒：位置 -> BoxInfo
    private final Map<BlockPos, BoxInfo> pendingBoxes = new HashMap<>();
    
    public static ConstructionBoxData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Level must be a ServerLevel");
        }
        
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(
            ConstructionBoxData::load,
            ConstructionBoxData::new,
            DATA_NAME
        );
    }
    
    /**
     * 注册待分配的控制盒位置
     * @param pos 控制盒位置
     * @param cityId 城市ID
     * @param buildingName 建筑显示名称（中文）
     * @param buildingFileName 建筑文件名称（英文，如"2br"）
     */
    public void registerPendingBox(BlockPos pos, UUID cityId, String buildingName, String buildingFileName) {
        pendingBoxes.put(pos, new BoxInfo(cityId, buildingName, buildingFileName));
        setDirty();
    }
    
    /**
     * 注册待分配的控制盒位置（兼容旧版本）
     * @param pos 控制盒位置
     * @param cityId 城市ID
     * @param buildingName 建筑名称
     */
    public void registerPendingBox(BlockPos pos, UUID cityId, String buildingName) {
        registerPendingBox(pos, cityId, buildingName, buildingName);
    }
    
    /**
     * 获取控制盒对应的城市ID
     * @param pos 控制盒位置
     * @return 城市ID，如果没有找到则返回null
     */
    public UUID getCityIdForBox(BlockPos pos) {
        BoxInfo info = pendingBoxes.get(pos);
        return info != null ? info.cityId : null;
    }
    
    /**
     * 获取控制盒对应的建筑名称
     * @param pos 控制盒位置
     * @return 建筑名称，如果没有找到则返回null
     */
    public String getBuildingNameForBox(BlockPos pos) {
        BoxInfo info = pendingBoxes.get(pos);
        return info != null ? info.buildingName : null;
    }
    
    /**
     * 获取控制盒的完整信息
     * @param pos 控制盒位置
     * @return BoxInfo，如果没有找到则返回null
     */
    public BoxInfo getBoxInfo(BlockPos pos) {
        return pendingBoxes.get(pos);
    }
    
    /**
     * 移除已处理的控制盒
     * @param pos 控制盒位置
     */
    public void removePendingBox(BlockPos pos) {
        if (pendingBoxes.remove(pos) != null) {
            setDirty();
        }
    }
    
    /**
     * 清空所有待分配的控制盒
     */
    public void clear() {
        pendingBoxes.clear();
        setDirty();
    }
    
    /**
     * 检查是否有待分配的控制盒
     * @return true如果有待分配的控制盒
     */
    public boolean hasPendingBoxes() {
        return !pendingBoxes.isEmpty();
    }
    
    /**
     * 获取待分配控制盒数量
     * @return 待分配控制盒数量
     */
    public int getPendingBoxCount() {
        return pendingBoxes.size();
    }
    
    /**
     * 获取所有控制盒位置
     * @return 控制盒位置集合
     */
    public Map<BlockPos, BoxInfo> getAllBoxes() {
        return new HashMap<>(pendingBoxes);
    }
    
    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (Map.Entry<BlockPos, BoxInfo> entry : pendingBoxes.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("pos", Objects.requireNonNull(NbtUtils.writeBlockPos(Objects.requireNonNull(entry.getKey()))));
            BoxInfo info = entry.getValue();
            entryTag.putUUID("cityId", Objects.requireNonNull(info.cityId));
            entryTag.putString("buildingName", Objects.requireNonNull(info.buildingName));
            entryTag.putString("buildingFileName", Objects.requireNonNull(info.buildingFileName));
            listTag.add(entryTag);
        }
        tag.put("pendingBoxes", listTag);
        return tag;
    }
    
    public static ConstructionBoxData load(CompoundTag tag) {
        ConstructionBoxData data = new ConstructionBoxData();
        if (tag.contains("pendingBoxes")) {
            ListTag listTag = tag.getList("pendingBoxes", 10); // 10 = CompoundTag type
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                BlockPos pos = Objects.requireNonNull(
                    NbtUtils.readBlockPos(Objects.requireNonNull(entryTag.getCompound("pos")))
                );
                UUID cityId = entryTag.getUUID("cityId");
                String buildingName = entryTag.getString("buildingName");
                String buildingFileName = entryTag.contains("buildingFileName") 
                    ? entryTag.getString("buildingFileName") 
                    : buildingName;
                data.pendingBoxes.put(pos, new BoxInfo(cityId, buildingName, buildingFileName));
            }
        }
        return data;
    }
}
