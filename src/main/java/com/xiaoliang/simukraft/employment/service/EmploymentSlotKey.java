package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import net.minecraft.core.BlockPos;

final class EmploymentSlotKey {
    private EmploymentSlotKey() {
    }

    static String forAssignment(EmploymentAssignment assignment) {
        return forValues(assignment.workBlockType(), assignment.jobType());
    }

    static String forValues(WorkBlockType workBlockType, JobType jobType) {
        if (workBlockType == WorkBlockType.BUILD_BOX) {
            if (jobType == JobType.PLANNER) {
                return "BUILD_BOX:PLANNER";
            }
            return "BUILD_BOX:BUILDER";
        }
        return workBlockType.name() + ":DEFAULT";
    }

    static String workplaceKey(String dimensionId, BlockPos pos) {
        String dim = normalizeDimension(dimensionId);
        return dim + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    static String workplaceSlotKey(String dimensionId, BlockPos pos, WorkBlockType workBlockType, JobType jobType) {
        return workplaceKey(dimensionId, pos) + "|" + forValues(workBlockType, jobType);
    }

    static String normalizeDimension(String dimensionId) {
        return dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
    }
}

