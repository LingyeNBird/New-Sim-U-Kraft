package com.xiaoliang.simukraft.employment.bridge;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.EmploymentStatus;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmploymentLegacyBridgeDualSlotTest {

    @Test
    void loadAssignmentsKeepsBuilderWhenPlannerIsNewestAtSameBuildBox() {
        BlockPos pos = new BlockPos(12, 64, 34);
        UUID builderUuid = UUID.randomUUID();
        UUID plannerUuid = UUID.randomUUID();

        List<EmploymentAssignment> assignments = List.of(
                new EmploymentAssignment(
                        builderUuid,
                        "minecraft:overworld",
                        pos,
                        WorkBlockType.BUILD_BOX,
                        JobType.BUILDER,
                        EmploymentStatus.ASSIGNED,
                        1L,
                        1000L
                ),
                new EmploymentAssignment(
                        plannerUuid,
                        "minecraft:overworld",
                        pos,
                        WorkBlockType.BUILD_BOX,
                        JobType.PLANNER,
                        EmploymentStatus.ASSIGNED,
                        2L,
                        2000L
                )
        );

        Map<BlockPos, UUID> builders = EmploymentLegacyBridge.latestAssignmentsForJob(
                assignments,
                "minecraft:overworld",
                WorkBlockType.BUILD_BOX,
                JobType.BUILDER
        );

        assertEquals(builderUuid, builders.get(pos));
    }
}
