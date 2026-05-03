package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public final class EmploymentCommands {
    private EmploymentCommands() {
    }

    public record HireCommand(
            UUID npcUuid,
            String dimensionId,
            BlockPos workplacePos,
            WorkBlockType workBlockType,
            JobType jobType
    ) {
    }

    public record FireByNpcCommand(UUID npcUuid) {
    }

    public record FireByWorkplaceCommand(String dimensionId, BlockPos workplacePos) {
    }

    public record WorkBlockRemovedCommand(String dimensionId, BlockPos workplacePos) {
    }
}

