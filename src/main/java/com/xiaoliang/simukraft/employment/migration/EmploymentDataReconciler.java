package com.xiaoliang.simukraft.employment.migration;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.persistence.JsonEmploymentRepository;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public final class EmploymentDataReconciler {
    private static final String REPORT_FILE = "employment_reconcile_report.log";

    private EmploymentDataReconciler() {
    }

    public static void writeReport(MinecraftServer server) {
        try {
            String dimensionId = server.overworld().dimension().location().toString();

            List<LegacyRow> legacyRows = loadLegacyRows(server, dimensionId);
            Map<String, LegacyRow> legacyBySlot = new HashMap<>();
            Map<String, Integer> legacySlotCount = new HashMap<>();
            for (LegacyRow row : legacyRows) {
                String key = workplaceSlotKey(row.dimensionId(), row.pos(), row.jobType());
                legacySlotCount.merge(key, 1, Integer::sum);
                legacyBySlot.putIfAbsent(key, row);
            }

            List<EmploymentAssignment> v2All = new JsonEmploymentRepository(server).loadAll();
            List<EmploymentAssignment> v2Assigned = v2All.stream().filter(EmploymentAssignment::isAssigned).toList();
            Map<String, EmploymentAssignment> v2BySlot = new HashMap<>();
            for (EmploymentAssignment a : v2Assigned) {
                v2BySlot.put(workplaceSlotKey(a.dimensionId(), a.workplacePos(), a.jobType()), a);
            }

            List<String> lines = new ArrayList<>();
            lines.add("# Employment Reconcile Report");
            lines.add("generatedAt=" + Instant.now());
            lines.add("legacyTotal=" + legacyRows.size());
            lines.add("v2AssignedTotal=" + v2Assigned.size());

            int legacyDuplicateWorkplace = 0;
            for (var e : legacySlotCount.entrySet()) {
                if (e.getValue() > 1) {
                    legacyDuplicateWorkplace++;
                }
            }
            lines.add("legacyDuplicateWorkplace=" + legacyDuplicateWorkplace);

            List<String> missingInV2 = new ArrayList<>();
            for (LegacyRow row : legacyRows) {
                String key = workplaceSlotKey(row.dimensionId(), row.pos(), row.jobType());
                if (!v2BySlot.containsKey(key)) {
                    missingInV2.add("missingInV2 workplace=" + key + " npc=" + row.npcUuid() + " source=" + row.source());
                }
            }

            List<String> missingInLegacy = new ArrayList<>();
            for (EmploymentAssignment assignment : v2Assigned) {
                String key = workplaceSlotKey(assignment.dimensionId(), assignment.workplacePos(), assignment.jobType());
                if (!legacyBySlot.containsKey(key)) {
                    missingInLegacy.add("missingInLegacy workplace=" + key + " npc=" + assignment.npcUuid() + " job=" + assignment.jobType());
                }
            }

            List<String> npcMismatch = new ArrayList<>();
            for (var entry : v2BySlot.entrySet()) {
                LegacyRow legacy = legacyBySlot.get(entry.getKey());
                if (legacy == null) {
                    continue;
                }
                EmploymentAssignment v2 = entry.getValue();
                if (!v2.npcUuid().equals(legacy.npcUuid())) {
                    npcMismatch.add("npcMismatch workplace=" + entry.getKey() + " legacyNpc=" + legacy.npcUuid() + " v2Npc=" + v2.npcUuid());
                }
            }

            lines.add("missingInV2Count=" + missingInV2.size());
            lines.add("missingInLegacyCount=" + missingInLegacy.size());
            lines.add("npcMismatchCount=" + npcMismatch.size());
            lines.add("");

            appendSection(lines, "missingInV2", missingInV2);
            appendSection(lines, "missingInLegacy", missingInLegacy);
            appendSection(lines, "npcMismatch", npcMismatch);

            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            Path simukraftDir = worldDir.resolve(FileUtils.MODE_DIR);
            if (!Files.exists(simukraftDir)) {
                Files.createDirectories(simukraftDir);
            }
            Path reportPath = simukraftDir.resolve(REPORT_FILE);
            Files.write(reportPath, lines, StandardCharsets.UTF_8);
            Simukraft.LOGGER.info("[Employment] reconcile report written to {}", reportPath.toAbsolutePath());
        } catch (Exception e) {
            Simukraft.LOGGER.error("[Employment] failed to write reconcile report: {}", e.getMessage());
        }
    }

    private static void appendSection(List<String> lines, String title, List<String> data) {
        final int limit = 200;
        lines.add("## " + title);
        if (data.isEmpty()) {
            lines.add("- none");
            lines.add("");
            return;
        }

        int size = Math.min(limit, data.size());
        for (int i = 0; i < size; i++) {
            lines.add("- " + data.get(i));
        }
        if (data.size() > limit) {
            lines.add("- ...truncated " + (data.size() - limit) + " rows");
        }
        lines.add("");
    }

    private static List<LegacyRow> loadLegacyRows(MinecraftServer server, String dimensionId) {
        List<LegacyRow> rows = new ArrayList<>();

        for (var entry : BuildBoxHiredData.loadHiredBuilders(server).entrySet()) {
            rows.add(new LegacyRow(dimensionId, entry.getKey(), entry.getValue(), "buildbox_builder", JobType.BUILDER));
        }
        for (var entry : BuildBoxHiredData.loadHiredPlanners(server).entrySet()) {
            rows.add(new LegacyRow(dimensionId, entry.getKey(), entry.getValue(), "buildbox_planner", JobType.PLANNER));
        }

        FarmlandHiredData.loadAllFarmlandData(server);
        for (var entry : FarmlandHiredData.getHiredFarmers().entrySet()) {
            rows.add(new LegacyRow(dimensionId, entry.getKey(), entry.getValue(), "farmland", JobType.FARMER));
        }

        for (var entry : IndustrialHiredData.loadHiredEmployees(server).entrySet()) {
            var info = entry.getValue();
            if (info != null && info.getNpcUuid() != null) {
                rows.add(new LegacyRow(
                        dimensionId,
                        entry.getKey(),
                        info.getNpcUuid(),
                        "industrial:" + info.getJobType(),
                        LegacyJobTypeMapper.fromLegacy(info.getJobType(), "industrial")
                ));
            }
        }

        for (var entry : CommercialHiredData.loadHiredEmployees(server).entrySet()) {
            var info = entry.getValue();
            if (info != null && info.getNpcUuid() != null) {
                rows.add(new LegacyRow(
                        dimensionId,
                        entry.getKey(),
                        info.getNpcUuid(),
                        "commercial:" + info.getJobType(),
                        LegacyJobTypeMapper.fromLegacy(info.getJobType(), inferLegacyHint(info.getJobType()))
                ));
            }
        }

        return rows;
    }

    private static String inferLegacyHint(String legacyJob) {
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

    private static String workplaceKey(String dimensionId, BlockPos pos) {
        String dim = (dimensionId == null || dimensionId.isBlank()) ? "minecraft:overworld" : dimensionId;
        return dim + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String workplaceSlotKey(String dimensionId, BlockPos pos, JobType jobType) {
        String slot = jobType == JobType.PLANNER ? "BUILD_BOX:PLANNER"
                : jobType == JobType.BUILDER ? "BUILD_BOX:BUILDER"
                : "DEFAULT:" + jobType.name();
        return workplaceKey(dimensionId, pos) + "|" + slot;
    }

    private record LegacyRow(String dimensionId, BlockPos pos, UUID npcUuid, String source, JobType jobType) {
    }
}
