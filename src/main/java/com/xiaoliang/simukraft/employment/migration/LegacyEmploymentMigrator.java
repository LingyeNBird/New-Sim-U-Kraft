package com.xiaoliang.simukraft.employment.migration;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import com.xiaoliang.simukraft.employment.persistence.JsonEmploymentRepository;
import com.xiaoliang.simukraft.employment.service.EmploymentCommands;
import com.xiaoliang.simukraft.employment.service.EmploymentErrorCode;
import com.xiaoliang.simukraft.employment.service.EmploymentServices;
import com.xiaoliang.simukraft.employment.service.LegacyJobTypeMapper;
import com.xiaoliang.simukraft.utils.FileUtils;
import com.xiaoliang.simukraft.world.BuildBoxHiredData;
import com.xiaoliang.simukraft.world.CommercialHiredData;
import com.xiaoliang.simukraft.world.FarmlandHiredData;
import com.xiaoliang.simukraft.world.IndustrialHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public final class LegacyEmploymentMigrator {
    private static final String CONFLICT_FILE = "employment_migration_conflicts.log";

    private LegacyEmploymentMigrator() {
    }

    public static void migrateIfNeeded(MinecraftServer server) {
        boolean forceMigration = Boolean.getBoolean("simukraft.employment.v2.forceMigration");
        var repo = new JsonEmploymentRepository(server);
        var existing = repo.loadAll();

        if (!forceMigration && !existing.isEmpty()) {
            Simukraft.LOGGER.info("[Employment] v2 migration skipped: repository already has {} entries", existing.size());
            return;
        }

        String dimensionId = server.overworld().dimension().location().toString();
        var service = EmploymentServices.get(server);
        List<String> conflicts = new ArrayList<>();

        int migrated = 0;
        migrated += migrateSimpleMap(service, BuildBoxHiredData.loadHiredBuilders(server), dimensionId, WorkBlockType.BUILD_BOX, JobType.BUILDER, "build_box", conflicts);
        migrated += migrateSimpleMap(service, BuildBoxHiredData.loadHiredPlanners(server), dimensionId, WorkBlockType.BUILD_BOX, JobType.PLANNER, "build_box", conflicts);

        FarmlandHiredData.loadAllFarmlandData(server);
        migrated += migrateSimpleMap(service, FarmlandHiredData.getHiredFarmers(), dimensionId, WorkBlockType.FARMLAND_BOX, JobType.FARMER, "farmland", conflicts);

        migrated += migrateIndustrialMap(service, IndustrialHiredData.loadHiredEmployees(server), dimensionId, conflicts);
        migrated += migrateCommercialMap(service, CommercialHiredData.loadHiredEmployees(server), dimensionId, conflicts);

        if (!conflicts.isEmpty()) {
            writeConflicts(server, conflicts);
        }

        Simukraft.LOGGER.info("[Employment] v2 migration completed: migrated={}, conflicts={}", migrated, conflicts.size());
    }

    private static int migrateSimpleMap(
            com.xiaoliang.simukraft.employment.service.DefaultEmploymentService service,
            Map<BlockPos, UUID> source,
            String dimensionId,
            WorkBlockType workBlockType,
            JobType jobType,
            String hint,
            List<String> conflicts
    ) {
        int count = 0;
        for (var entry : source.entrySet()) {
            var result = service.hire(new EmploymentCommands.HireCommand(
                    entry.getValue(),
                    dimensionId,
                    entry.getKey(),
                    workBlockType,
                    jobType
            ));
            if (result.success()) {
                count++;
            } else if (result.code() != EmploymentErrorCode.NPC_ALREADY_ASSIGNED
                    && result.code() != EmploymentErrorCode.WORKPLACE_ALREADY_OCCUPIED) {
                conflicts.add("[" + hint + "] pos=" + entry.getKey() + ", npc=" + entry.getValue() + ", code=" + result.code() + ", message=" + result.message());
            } else {
                conflicts.add("[" + hint + "] pos=" + entry.getKey() + ", npc=" + entry.getValue() + ", conflict=" + result.code());
            }
        }
        return count;
    }

    private static int migrateIndustrialMap(
            com.xiaoliang.simukraft.employment.service.DefaultEmploymentService service,
            Map<BlockPos, IndustrialHiredData.IndustrialHireInfo> source,
            String dimensionId,
            List<String> conflicts
    ) {
        int count = 0;
        for (var entry : source.entrySet()) {
            var info = entry.getValue();
            if (info == null || info.getNpcUuid() == null) {
                continue;
            }
            String hint = "industrial";
            if ("shepherd".equals(info.getJobType())) {
                hint = "wool_farm";
            } else if ("butcher".equals(info.getJobType())) {
                hint = "beef_farm";
            }

            var result = service.hire(new EmploymentCommands.HireCommand(
                    info.getNpcUuid(),
                    dimensionId,
                    entry.getKey(),
                    WorkBlockType.INDUSTRIAL_CONTROL_BOX,
                    LegacyJobTypeMapper.fromLegacy(info.getJobType(), hint)
            ));
            if (result.success()) {
                count++;
            } else {
                conflicts.add("[industrial] pos=" + entry.getKey() + ", npc=" + info.getNpcUuid() + ", job=" + info.getJobType() + ", result=" + result.code());
            }
        }
        return count;
    }

    private static int migrateCommercialMap(
            com.xiaoliang.simukraft.employment.service.DefaultEmploymentService service,
            Map<BlockPos, CommercialHiredData.CommercialHireInfo> source,
            String dimensionId,
            List<String> conflicts
    ) {
        int count = 0;
        for (var entry : source.entrySet()) {
            var info = entry.getValue();
            if (info == null || info.getNpcUuid() == null) {
                continue;
            }

            String hint = inferCommercialHint(info.getJobType());
            var result = service.hire(new EmploymentCommands.HireCommand(
                    info.getNpcUuid(),
                    dimensionId,
                    entry.getKey(),
                    WorkBlockType.COMMERCIAL_CONTROL_BOX,
                    LegacyJobTypeMapper.fromLegacy(info.getJobType(), hint)
            ));
            if (result.success()) {
                count++;
            } else {
                conflicts.add("[commercial] pos=" + entry.getKey() + ", npc=" + info.getNpcUuid() + ", job=" + info.getJobType() + ", result=" + result.code());
            }
        }
        return count;
    }

    private static String inferCommercialHint(String legacyJob) {
        // 从JSON配置查找建筑类型
        if (legacyJob != null && !legacyJob.isBlank()) {
            var configs = com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfigsByJobType(legacyJob);
            if (!configs.isEmpty()) {
                String buildingType = configs.get(0).getBuildingName();
                if (buildingType != null && !buildingType.isBlank()) {
                    return buildingType;
                }
            }
        }
        return "commercial";
    }

    private static void writeConflicts(MinecraftServer server, List<String> conflicts) {
        try {
            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            Path simukraftDir = worldDir.resolve(FileUtils.MODE_DIR);
            if (!Files.exists(simukraftDir)) {
                Files.createDirectories(simukraftDir);
            }
            Path conflictFile = simukraftDir.resolve(CONFLICT_FILE);
            Files.write(conflictFile, conflicts, StandardCharsets.UTF_8);
            Simukraft.LOGGER.info("[Employment] migration conflicts written to {}", conflictFile.toAbsolutePath());
        } catch (Exception e) {
            Simukraft.LOGGER.error("[Employment] failed to write migration conflicts: {}", e.getMessage());
        }
    }
}

