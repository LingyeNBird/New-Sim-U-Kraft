package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.building.CommercialBuildingConfig;
import com.xiaoliang.simukraft.building.CommercialBuildingManager;
import net.minecraft.core.BlockPos;

/**
 * 商业建筑雇佣界面兼容类
 * 为旧版商业建筑Screen提供兼容层，委托给新的 HireCommercialScreen
 * @deprecated 请使用 HireCommercialScreen
 */
@Deprecated
public class HireCommercialEmployeeScreen extends HireCommercialScreen {

    /**
     * 构造函数
     * @param controlBoxPos 控制盒位置
     * @param buildingType 建筑类型（从JSON配置文件读取）
     * @deprecated 请使用 {@link HireCommercialScreen#HireCommercialScreen(BlockPos, String, String)}
     */
    @Deprecated
    public HireCommercialEmployeeScreen(BlockPos controlBoxPos, String buildingType) {
        super(controlBoxPos, resolveJobTypeFromConfig(buildingType), buildingType);
    }

    /**
     * 从JSON配置解析职业类型
     * @deprecated 内部方法，不建议使用
     */
    @Deprecated
    private static String resolveJobTypeFromConfig(String buildingType) {
        // 优先从JSON配置文件读取职业类型
        CommercialBuildingConfig config = CommercialBuildingManager.getConfig(buildingType);
        if (config != null) {
            String jobType = config.getJobType();
            if (jobType != null && !jobType.isBlank()) {
                return jobType;
            }
        }
        // 默认返回通用shopkeeper
        return "shopkeeper";
    }
}
