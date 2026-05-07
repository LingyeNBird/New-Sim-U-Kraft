package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.building.MedicalBuildingManager;
import com.xiaoliang.simukraft.employment.domain.JobType;

public final class LegacyJobTypeMapper {
    private LegacyJobTypeMapper() {
    }

    public static JobType fromLegacy(String legacyJob, String workBlockTypeHint) {
        if (legacyJob == null || legacyJob.isEmpty()) {
            return defaultByWorkBlock(workBlockTypeHint);
        }
        
        // 优先从JSON配置查找
        JobType fromConfig = resolveFromConfig(legacyJob);
        if (fromConfig != null) {
            return fromConfig;
        }
        
        return switch (legacyJob) {
            case "builder" -> JobType.BUILDER;
            case "planner" -> JobType.PLANNER;
            case "farmer" -> JobType.FARMER;
            case "doctor" -> JobType.DOCTOR;
            case "warehouse_manager" -> JobType.WAREHOUSE_MANAGER;
            default -> defaultByWorkBlock(workBlockTypeHint);
        };
    }

    public static String toLegacy(JobType jobType) {
        if (jobType == null) {
            return "worker";
        }
        return switch (jobType) {
            case BUILDER -> "builder";
            case PLANNER -> "planner";
            case FARMER -> "farmer";
            case COMMERCIAL_GENERIC -> "shopkeeper";
            case INDUSTRIAL_GENERIC -> "worker";
            case DOCTOR -> "doctor";
            case WAREHOUSE_MANAGER -> "warehouse_manager";
            default -> "worker";
        };
    }
    
    /**
     * 从JSON配置解析职业类型
     */
    private static JobType resolveFromConfig(String legacyJob) {
        // 从商业建筑配置查找
        var commercialConfigs = com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfigsByJobType(legacyJob);
        if (!commercialConfigs.isEmpty()) {
            // 商业建筑统一返回 COMMERCIAL_GENERIC
            return JobType.COMMERCIAL_GENERIC;
        }
        
        // 从工业建筑配置查找
        var industrialConfigs = com.xiaoliang.simukraft.building.IndustrialBuildingManager.getConfigsByJobType(legacyJob);
        if (!industrialConfigs.isEmpty()) {
            // 工业建筑统一返回 INDUSTRIAL_GENERIC
            return JobType.INDUSTRIAL_GENERIC;
        }

        if (MedicalBuildingManager.isMedicalBuilding(legacyJob) || "doctor".equalsIgnoreCase(legacyJob)) {
            return JobType.DOCTOR;
        }
        
        return null;
    }

    private static JobType defaultByWorkBlock(String workBlockTypeHint) {
        // 从JSON配置检查是否是商业建筑
        if (workBlockTypeHint != null && !workBlockTypeHint.isBlank()) {
            var config = com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfig(workBlockTypeHint);
            if (config != null) {
                return JobType.COMMERCIAL_GENERIC;
            }
        }
        
        if ("industrial".equals(workBlockTypeHint) || "wool_farm".equals(workBlockTypeHint) || "beef_farm".equals(workBlockTypeHint)) {
            return JobType.INDUSTRIAL_GENERIC;
        }
        if ("commercial".equals(workBlockTypeHint)) {
            return JobType.COMMERCIAL_GENERIC;
        }
        if ("other".equals(workBlockTypeHint) || "other_control_box".equals(workBlockTypeHint)) {
            return JobType.DOCTOR;
        }
        if ("logistics".equals(workBlockTypeHint)) {
            return JobType.WAREHOUSE_MANAGER;
        }
        return JobType.INDUSTRIAL_GENERIC;
    }
}

