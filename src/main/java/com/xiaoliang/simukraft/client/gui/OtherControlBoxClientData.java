package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.building.MedicalBuildingConfig;
import com.xiaoliang.simukraft.building.MedicalBuildingManager;
import com.xiaoliang.simukraft.entity.CustomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 其他控制盒客户端缓存
 */
public final class OtherControlBoxClientData {
    private static final Map<BlockPos, UUID> HIRED_DOCTORS = new HashMap<>();
    private static final Map<BlockPos, String> BUILDING_FILE_NAMES = new HashMap<>();

    private OtherControlBoxClientData() {
    }

    public static void setHiredDoctor(BlockPos pos, @Nullable UUID npcUuid) {
        if (pos == null) {
            return;
        }
        if (npcUuid == null) {
            HIRED_DOCTORS.remove(pos);
            return;
        }
        HIRED_DOCTORS.put(pos, npcUuid);
    }

    public static void clearHiredDoctor(BlockPos pos) {
        if (pos != null) {
            HIRED_DOCTORS.remove(pos);
        }
    }

    public static boolean hasHiredDoctor(BlockPos pos) {
        return pos != null && HIRED_DOCTORS.containsKey(pos);
    }

    @Nullable
    public static UUID getHiredDoctorUuid(BlockPos pos) {
        return pos != null ? HIRED_DOCTORS.get(pos) : null;
    }

    @Nullable
    public static CustomEntity getHiredDoctor(BlockPos pos) {
        UUID npcUuid = getHiredDoctorUuid(pos);
        if (npcUuid == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof CustomEntity customEntity && npcUuid.equals(customEntity.getUUID())) {
                return customEntity;
            }
        }
        return null;
    }

    public static void setBuildingFileName(BlockPos pos, @Nullable String buildingFileName) {
        if (pos == null || buildingFileName == null || buildingFileName.isBlank()) {
            return;
        }
        BUILDING_FILE_NAMES.put(pos, buildingFileName);
    }

    @Nullable
    public static String getBuildingFileName(BlockPos pos) {
        return pos != null ? BUILDING_FILE_NAMES.get(pos) : null;
    }

    @Nullable
    public static MedicalBuildingConfig getMedicalConfig(BlockPos pos, @Nullable String fallbackBuildingFileName) {
        String buildingFileName = getBuildingFileName(pos);
        if (buildingFileName == null || buildingFileName.isBlank()) {
            buildingFileName = fallbackBuildingFileName;
        }
        return MedicalBuildingManager.getConfig(buildingFileName);
    }

    public static void clear(BlockPos pos) {
        if (pos == null) {
            return;
        }
        HIRED_DOCTORS.remove(pos);
        BUILDING_FILE_NAMES.remove(pos);
    }
}
