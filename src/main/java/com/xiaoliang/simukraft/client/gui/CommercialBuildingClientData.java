package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 商业建筑客户端数据兼容类
 * 为旧版商业建筑Screen提供兼容层，委托给新的 CommercialClientData
 * @deprecated 请使用 CommercialClientData
 */
@Deprecated
public class CommercialBuildingClientData {

    /**
     * 检查指定位置是否已雇佣员工
     * @param pos 建筑位置
     * @param buildingType 建筑类型（bakery, fruit_shop, meat_shop, building_material_store等）
     * @return 是否已雇佣
     * @deprecated 请使用 {@link CommercialClientData#hasHiredEmployee(BlockPos)}
     */
    @Deprecated
    public static boolean hasHiredEmployee(BlockPos pos, String buildingType) {
        return CommercialClientData.hasHiredEmployee(pos);
    }

    /**
     * 获取指定位置雇佣的员工
     * @param pos 建筑位置
     * @param buildingType 建筑类型
     * @return 雇佣的NPC实体，如果没有则返回null
     * @deprecated 请使用 {@link CommercialClientData#getHiredEmployee(BlockPos)}
     */
    @Deprecated
    public static CustomEntity getHiredEmployee(BlockPos pos, String buildingType) {
        return CommercialClientData.getHiredEmployee(pos);
    }

    /**
     * 设置雇佣员工
     * @param pos 建筑位置
     * @param npcUuid NPC的UUID
     * @param buildingType 建筑类型
     * @deprecated 请使用 {@link CommercialClientData#setHiredEmployee(BlockPos, UUID, String)}
     */
    @Deprecated
    public static void setHiredEmployee(BlockPos pos, UUID npcUuid, String buildingType) {
        String jobType = convertBuildingTypeToJobType(buildingType);
        CommercialClientData.setHiredEmployee(pos, npcUuid, jobType);
    }

    /**
     * 清除指定位置的雇佣员工
     * @param pos 建筑位置
     * @param buildingType 建筑类型
     * @deprecated 请使用 {@link CommercialClientData#clearHiredEmployee(BlockPos)}
     */
    @Deprecated
    public static void clearHiredEmployee(BlockPos pos, String buildingType) {
        CommercialClientData.clearHiredEmployee(pos);
    }

    /**
     * 同步已加载的数据
     * @param buildingType 建筑类型
     * @deprecated 请使用 {@link CommercialClientData#syncLoadedData()}
     */
    @Deprecated
    public static void syncLoadedData(String buildingType) {
        CommercialClientData.syncLoadedData();
    }

    /**
     * 获取所有雇佣员工的UUID
     * @param buildingType 建筑类型
     * @return 位置到UUID的映射
     * @deprecated 请使用 {@link CommercialClientData#getAllHiredEmployeeUuids()}
     */
    @Deprecated
    public static Map<BlockPos, UUID> getAllHiredEmployeeUuids(String buildingType) {
        Map<BlockPos, CommercialClientData.HireInfo> allHires = CommercialClientData.getAllHiredEmployeeUuids();
        Map<BlockPos, UUID> result = new HashMap<>();

        for (Map.Entry<BlockPos, CommercialClientData.HireInfo> entry : allHires.entrySet()) {
            // 根据建筑类型过滤
            String jobType = entry.getValue().getJobType();
            if (matchesBuildingType(jobType, buildingType)) {
                result.put(entry.getKey(), entry.getValue().getNpcUuid());
            }
        }

        return result;
    }

    /**
     * 将建筑类型转换为职业类型
     * @deprecated 内部方法，不建议使用
     */
    @Deprecated
    private static String convertBuildingTypeToJobType(String buildingType) {
        return switch (buildingType) {
            case "bakery" -> "chef";
            case "fruit_shop" -> "shopkeeper";
            case "meat_shop" -> "shopkeeper";
            case "building_material_store" -> "merchant";
            default -> "shopkeeper";
        };
    }

    /**
     * 检查职业类型是否匹配建筑类型
     * @deprecated 内部方法，不建议使用
     */
    @Deprecated
    private static boolean matchesBuildingType(String jobType, String buildingType) {
        return switch (buildingType) {
            case "bakery" -> "chef".equals(jobType);
            case "fruit_shop", "meat_shop" -> "shopkeeper".equals(jobType);
            case "building_material_store" -> "merchant".equals(jobType);
            default -> false;
        };
    }
}
