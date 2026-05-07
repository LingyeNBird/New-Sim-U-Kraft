package com.xiaoliang.simukraft.building;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 医疗机构配置管理器
 * 复用现有 sk 元数据，避免再创建一套平行配置系统。
 */
public final class MedicalBuildingManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<String> RESOURCE_CATEGORIES = List.of("public", "other");
    private static final Map<String, MedicalBuildingConfig> CACHE = new ConcurrentHashMap<>();

    private MedicalBuildingManager() {
    }

    @Nullable
    public static MedicalBuildingConfig getConfig(@Nullable String buildingFileName) {
        String normalized = normalizeBuildingFileName(buildingFileName);
        if (normalized == null) {
            return null;
        }
        return CACHE.computeIfAbsent(normalized, MedicalBuildingManager::loadConfigInternal);
    }

    public static boolean isMedicalBuilding(@Nullable String buildingFileName) {
        MedicalBuildingConfig config = getConfig(buildingFileName);
        return config != null && config.medical();
    }

    @Nullable
    public static String getBuildingName(@Nullable String buildingFileName) {
        MedicalBuildingConfig config = getConfig(buildingFileName);
        return config != null ? config.buildingName() : null;
    }

    @Nullable
    private static MedicalBuildingConfig loadConfigInternal(String buildingFileName) {
        for (String category : RESOURCE_CATEGORIES) {
            MedicalBuildingConfig fromUserFile = readFromUserFile(buildingFileName, category);
            if (fromUserFile != null) {
                return fromUserFile;
            }

            MedicalBuildingConfig fromResource = readFromResource(buildingFileName, category);
            if (fromResource != null) {
                return fromResource;
            }
        }
        return null;
    }

    @Nullable
    private static MedicalBuildingConfig readFromUserFile(String buildingFileName, String category) {
        File skFile = new File("simukraftbuilding/" + category + "/" + buildingFileName + ".sk");
        if (!skFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(skFile), StandardCharsets.UTF_8))) {
            return parse(reader, buildingFileName);
        } catch (Exception e) {
            LOGGER.warn("[MedicalBuildingManager] 读取用户医疗建筑配置失败: {}", skFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Nullable
    private static MedicalBuildingConfig readFromResource(String buildingFileName, String category) {
        String resourcePath = "assets/simukraft/building/" + category + "/" + buildingFileName + ".sk";
        try (InputStream stream = MedicalBuildingManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return parse(reader, buildingFileName);
            }
        } catch (Exception e) {
            LOGGER.warn("[MedicalBuildingManager] 读取资源医疗建筑配置失败: {}", resourcePath, e);
            return null;
        }
    }

    @Nullable
    private static MedicalBuildingConfig parse(BufferedReader reader, String buildingFileName) {
        String buildingName = buildingFileName;
        String jobType = "";
        String description = "";
        boolean canParturition = false;

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("name:")) {
                    buildingName = trimmed.substring("name:".length()).trim();
                } else if (trimmed.startsWith("job_type:")) {
                    jobType = trimmed.substring("job_type:".length()).trim();
                } else if (trimmed.startsWith("description:")) {
                    description = trimmed.substring("description:".length()).trim();
                } else if (trimmed.startsWith("canParturition:")) {
                    canParturition = Boolean.parseBoolean(trimmed.substring("canParturition:".length()).trim());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[MedicalBuildingManager] 解析医疗建筑配置失败: {}", buildingFileName, e);
            return null;
        }

        String loweredName = buildingName.toLowerCase(Locale.ROOT);
        String loweredDesc = description.toLowerCase(Locale.ROOT);
        boolean medical = "doctor".equalsIgnoreCase(jobType)
                || canParturition
                || loweredName.contains("医院")
                || loweredName.contains("诊所")
                || loweredName.contains("hospital")
                || loweredName.contains("clinic")
                || loweredDesc.contains("分娩")
                || loweredDesc.contains("治疗")
                || loweredDesc.contains("hospital")
                || loweredDesc.contains("clinic");
        if (!medical) {
            return null;
        }

        return new MedicalBuildingConfig(buildingFileName, buildingName, jobType, canParturition, true);
    }

    @Nullable
    private static String normalizeBuildingFileName(@Nullable String buildingFileName) {
        if (buildingFileName == null || buildingFileName.isBlank()) {
            return null;
        }
        String normalized = buildingFileName.trim();
        if (normalized.endsWith(".sk")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized.isBlank() ? null : normalized;
    }
}
