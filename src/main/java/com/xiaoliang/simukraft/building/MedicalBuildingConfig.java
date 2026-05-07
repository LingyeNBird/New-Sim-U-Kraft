package com.xiaoliang.simukraft.building;

/**
 * 医疗建筑轻量配置
 */
public record MedicalBuildingConfig(
        String buildingFileName,
        String buildingName,
        String jobType,
        boolean canParturition,
        boolean medical
) {
    public boolean isDoctorJob() {
        return "doctor".equalsIgnoreCase(jobType);
    }
}
