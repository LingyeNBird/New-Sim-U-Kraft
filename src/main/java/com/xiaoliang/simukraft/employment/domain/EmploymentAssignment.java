package com.xiaoliang.simukraft.employment.domain;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record EmploymentAssignment(
        UUID npcUuid,
        String dimensionId,
        BlockPos workplacePos,
        WorkBlockType workBlockType,
        JobType jobType,
        EmploymentStatus status,
        long version,
        long updatedAtEpochMs
) {
    public boolean isAssigned() {
        return status == EmploymentStatus.ASSIGNED;
    }
}

