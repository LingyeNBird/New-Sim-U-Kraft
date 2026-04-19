package com.xiaoliang.simukraft.building;

import com.xiaoliang.simukraft.world.ConstructionBoxData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * 建筑控制盒位置映射管理器
 * 用于在建筑师建造前预注册控制盒位置和城市ID的映射关系
 * 
 * 修复：现在使用ConstructionBoxData进行持久化存储，服务器重启后数据不会丢失
 */
public class ConstructionBoxMapping {
    
    /**
     * 注册待分配的控制盒位置
     * @param level 世界
     * @param positions 控制盒位置列表
     * @param cityId 城市ID
     * @param buildingName 建筑显示名称（中文）
     * @param buildingFileName 建筑文件名称（英文，如"2br"）
     */
    public static void registerPendingBoxes(Level level, List<BlockPos> positions, UUID cityId, String buildingName, String buildingFileName) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        for (BlockPos pos : positions) {
            data.registerPendingBox(pos, cityId, buildingName, buildingFileName);
        }
    }

    /**
     * 注册待分配的控制盒位置（兼容旧版本）
     * @param level 世界
     * @param positions 控制盒位置列表
     * @param cityId 城市ID
     * @param buildingName 建筑名称
     */
    public static void registerPendingBoxes(Level level, List<BlockPos> positions, UUID cityId, String buildingName) {
        registerPendingBoxes(level, positions, cityId, buildingName, buildingName);
    }

    /**
     * 注册待分配的控制盒位置（兼容旧版本，建筑名称为unknown）
     * @param level 世界
     * @param positions 控制盒位置列表
     * @param cityId 城市ID
     */
    public static void registerPendingBoxes(Level level, List<BlockPos> positions, UUID cityId) {
        registerPendingBoxes(level, positions, cityId, "unknown", "unknown");
    }

    /**
     * 获取控制盒对应的城市ID
     * @param level 世界
     * @param pos 控制盒位置
     * @return 城市ID，如果没有找到则返回null
     */
    public static UUID getCityIdForBox(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        return data.getCityIdForBox(pos);
    }
    
    /**
     * 获取控制盒对应的建筑名称
     * @param level 世界
     * @param pos 控制盒位置
     * @return 建筑名称，如果没有找到则返回null
     */
    public static String getBuildingNameForBox(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        return data.getBuildingNameForBox(pos);
    }
    
    /**
     * 获取控制盒的完整信息
     * @param level 世界
     * @param pos 控制盒位置
     * @return BoxInfo，如果没有找到则返回null
     */
    public static ConstructionBoxData.BoxInfo getBoxInfo(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        return data.getBoxInfo(pos);
    }

    /**
     * 移除已处理的控制盒
     * @param level 世界
     * @param pos 控制盒位置
     */
    public static void removePendingBox(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        data.removePendingBox(pos);
    }

    /**
     * 清空所有待分配的控制盒
     * @param level 世界
     */
    public static void clear(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        data.clear();
    }

    /**
     * 检查是否有待分配的控制盒
     * @param level 世界
     * @return true如果有待分配的控制盒
     */
    public static boolean hasPendingBoxes(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        return data.hasPendingBoxes();
    }

    /**
     * 获取待分配控制盒数量
     * @param level 世界
     * @return 待分配控制盒数量
     */
    public static int getPendingBoxCount(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        
        ConstructionBoxData data = ConstructionBoxData.get(serverLevel);
        return data.getPendingBoxCount();
    }
}
