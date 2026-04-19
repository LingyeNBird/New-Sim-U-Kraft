package com.xiaoliang.simukraft.employment.client;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WorkBlockHireClientCache {
    private static final Map<String, EmploymentAssignment> BY_WORKPLACE_SLOT = new HashMap<>();
    private static final Map<UUID, EmploymentAssignment> BY_NPC = new HashMap<>();

    private WorkBlockHireClientCache() {
    }

    public static synchronized void upsert(EmploymentAssignment assignment) {
        if (assignment == null || !assignment.isAssigned()) {
            return;
        }
        String workplaceKey = slotKeyOf(assignment.dimensionId(), assignment.workplacePos(), assignment.workBlockType(), assignment.jobType());

        EmploymentAssignment oldByWorkplace = BY_WORKPLACE_SLOT.put(workplaceKey, assignment);
        if (oldByWorkplace != null) {
            BY_NPC.remove(oldByWorkplace.npcUuid());
        }

        EmploymentAssignment oldByNpc = BY_NPC.put(assignment.npcUuid(), assignment);
        if (oldByNpc != null) {
            BY_WORKPLACE_SLOT.remove(slotKeyOf(oldByNpc.dimensionId(), oldByNpc.workplacePos(), oldByNpc.workBlockType(), oldByNpc.jobType()));
        }
    }

    public static synchronized void remove(EmploymentAssignment assignment) {
        if (assignment == null) {
            return;
        }
        EmploymentAssignment removed = BY_WORKPLACE_SLOT.remove(slotKeyOf(assignment.dimensionId(), assignment.workplacePos(), assignment.workBlockType(), assignment.jobType()));
        if (removed != null) {
            BY_NPC.remove(removed.npcUuid());
        }
    }

    public static synchronized void removeByWorkplace(String dimensionId, BlockPos pos) {
        String prefix = workplaceKeyOf(dimensionId, pos) + "|";
        var toRemove = BY_WORKPLACE_SLOT.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .toList();
        toRemove.forEach(WorkBlockHireClientCache::remove);
    }

    public static synchronized Optional<EmploymentAssignment> findByWorkplace(String dimensionId, BlockPos pos) {
        String prefix = workplaceKeyOf(dimensionId, pos) + "|";
        return BY_WORKPLACE_SLOT.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public static synchronized Optional<EmploymentAssignment> findByNpc(UUID npcUuid) {
        if (npcUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NPC.get(npcUuid));
    }

    public static synchronized void clearAll() {
        BY_WORKPLACE_SLOT.clear();
        BY_NPC.clear();
    }

    private static String slotKeyOf(String dimensionId, BlockPos pos, WorkBlockType workBlockType, JobType jobType) {
        return workplaceKeyOf(dimensionId, pos) + "|" + slotOf(workBlockType, jobType);
    }

    private static String workplaceKeyOf(String dimensionId, BlockPos pos) {
        String dimension = (dimensionId == null || dimensionId.isBlank()) ? "minecraft:overworld" : dimensionId;
        return dimension + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String slotOf(WorkBlockType workBlockType, JobType jobType) {
        if (workBlockType == WorkBlockType.BUILD_BOX) {
            return jobType == JobType.PLANNER ? "BUILD_BOX:PLANNER" : "BUILD_BOX:BUILDER";
        }
        return workBlockType.name() + ":DEFAULT";
    }
}
