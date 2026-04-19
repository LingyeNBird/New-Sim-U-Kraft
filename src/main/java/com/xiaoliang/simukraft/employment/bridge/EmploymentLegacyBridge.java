package com.xiaoliang.simukraft.employment.bridge;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.EmploymentStatus;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import com.xiaoliang.simukraft.employment.persistence.JsonEmploymentRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 兼容旧接口，但所有职业数据都以 v2 仓储为唯一事实源。
 */
public final class EmploymentLegacyBridge {
    private EmploymentLegacyBridge() {
    }

    public record AssignmentInput(UUID npcUuid, JobType jobType) {
    }

    public static synchronized Map<BlockPos, UUID> loadAssignments(MinecraftServer server, WorkBlockType workBlockType, JobType jobType) {
        return latestAssignmentsForJob(
                repository(server).loadAll(),
                primaryDimensionId(server),
                workBlockType,
                jobType
        );
    }

    public static synchronized void saveAssignments(MinecraftServer server,
                                                    WorkBlockType workBlockType,
                                                    JobType jobType,
                                                    Map<BlockPos, UUID> desiredAssignments) {
        if (server == null) {
            return;
        }

        List<EmploymentAssignment> all = new ArrayList<>(repository(server).loadAll());
        String dimensionId = primaryDimensionId(server);
        all.removeIf(assignment -> matches(assignment, dimensionId, workBlockType) && assignment.jobType() == jobType);

        long nextVersion = nextVersion(all);
        long now = System.currentTimeMillis();
        List<Map.Entry<BlockPos, UUID>> sortedEntries = desiredAssignments.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .sorted(blockPosEntryComparator())
                .toList();

        for (Map.Entry<BlockPos, UUID> entry : sortedEntries) {
            all.add(new EmploymentAssignment(
                    entry.getValue(),
                    dimensionId,
                    entry.getKey(),
                    workBlockType,
                    jobType,
                    EmploymentStatus.ASSIGNED,
                    nextVersion++,
                    now
            ));
        }

        repository(server).saveAll(all);
    }

    public static synchronized Map<BlockPos, EmploymentAssignment> loadLatestByWorkBlock(MinecraftServer server, WorkBlockType workBlockType) {
        String dimensionId = primaryDimensionId(server);
        Map<BlockPos, EmploymentAssignment> result = new HashMap<>();
        for (EmploymentAssignment assignment : repository(server).loadAll()) {
            if (!matches(assignment, dimensionId, workBlockType)) {
                continue;
            }
            result.merge(
                    assignment.workplacePos(),
                    assignment,
                    (left, right) -> left.version() >= right.version() ? left : right
            );
        }
        return result;
    }

    public static synchronized void saveAssignmentsByWorkBlock(MinecraftServer server,
                                                               WorkBlockType workBlockType,
                                                               Map<BlockPos, AssignmentInput> desiredAssignments) {
        if (server == null) {
            return;
        }

        List<EmploymentAssignment> all = new ArrayList<>(repository(server).loadAll());
        String dimensionId = primaryDimensionId(server);
        all.removeIf(assignment -> matches(assignment, dimensionId, workBlockType));

        long nextVersion = nextVersion(all);
        long now = System.currentTimeMillis();
        List<Map.Entry<BlockPos, AssignmentInput>> sortedEntries = desiredAssignments.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> entry.getValue().npcUuid() != null && entry.getValue().jobType() != null)
                .sorted(blockPosAssignmentComparator())
                .toList();

        for (Map.Entry<BlockPos, AssignmentInput> entry : sortedEntries) {
            AssignmentInput value = entry.getValue();
            all.add(new EmploymentAssignment(
                    value.npcUuid(),
                    dimensionId,
                    entry.getKey(),
                    workBlockType,
                    value.jobType(),
                    EmploymentStatus.ASSIGNED,
                    nextVersion++,
                    now
            ));
        }

        repository(server).saveAll(all);
    }

    static Map<BlockPos, UUID> latestAssignmentsForJob(Iterable<EmploymentAssignment> assignments,
                                                       String dimensionId,
                                                       WorkBlockType workBlockType,
                                                       JobType jobType) {
        String normalized = normalizeDimension(dimensionId);
        Map<BlockPos, EmploymentAssignment> latestByWorkplace = new HashMap<>();
        for (EmploymentAssignment assignment : assignments) {
            if (!matches(assignment, normalized, workBlockType) || assignment.jobType() != jobType) {
                continue;
            }
            latestByWorkplace.merge(
                    assignment.workplacePos(),
                    assignment,
                    (left, right) -> left.version() >= right.version() ? left : right
            );
        }

        Map<BlockPos, UUID> result = new HashMap<>();
        for (EmploymentAssignment assignment : latestByWorkplace.values()) {
            result.put(assignment.workplacePos(), assignment.npcUuid());
        }
        return result;
    }

    private static boolean matches(EmploymentAssignment assignment, String dimensionId, WorkBlockType workBlockType) {
        return assignment != null
                && assignment.isAssigned()
                && assignment.workBlockType() == workBlockType
                && normalizeDimension(assignment.dimensionId()).equals(dimensionId);
    }

    private static long nextVersion(List<EmploymentAssignment> assignments) {
        return assignments.stream().mapToLong(EmploymentAssignment::version).max().orElse(0L) + 1L;
    }

    private static Comparator<Map.Entry<BlockPos, UUID>> blockPosEntryComparator() {
        return Comparator.comparingInt((Map.Entry<BlockPos, UUID> entry) -> entry.getKey().getX())
                .thenComparingInt(entry -> entry.getKey().getY())
                .thenComparingInt(entry -> entry.getKey().getZ());
    }

    private static Comparator<Map.Entry<BlockPos, AssignmentInput>> blockPosAssignmentComparator() {
        return Comparator.comparingInt((Map.Entry<BlockPos, AssignmentInput> entry) -> entry.getKey().getX())
                .thenComparingInt(entry -> entry.getKey().getY())
                .thenComparingInt(entry -> entry.getKey().getZ());
    }

    private static JsonEmploymentRepository repository(MinecraftServer server) {
        return new JsonEmploymentRepository(server);
    }

    private static String primaryDimensionId(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return "minecraft:overworld";
        }
        return normalizeDimension(server.overworld().dimension().location().toString());
    }

    private static String normalizeDimension(String dimensionId) {
        return dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
    }

    /**
     * 获取所有已雇佣的NPC UUID列表（按最新版本）
     * 用于替代从jobdata.sk读取雇佣状态
     */
    public static synchronized Map<UUID, EmploymentAssignment> loadAllHiredNPCs(MinecraftServer server) {
        String dimensionId = primaryDimensionId(server);
        Map<UUID, EmploymentAssignment> result = new HashMap<>();
        for (EmploymentAssignment assignment : repository(server).loadAll()) {
            if (!assignment.isAssigned()) {
                continue;
            }
            if (!normalizeDimension(assignment.dimensionId()).equals(dimensionId)) {
                continue;
            }
            // 每个NPC只保留最新版本的分配
            result.merge(
                    assignment.npcUuid(),
                    assignment,
                    (left, right) -> left.version() >= right.version() ? left : right
            );
        }
        return result;
    }
}
