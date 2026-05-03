package com.xiaoliang.simukraft.employment;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import com.xiaoliang.simukraft.employment.persistence.EmploymentRepository;
import com.xiaoliang.simukraft.employment.service.DefaultEmploymentService;
import com.xiaoliang.simukraft.employment.service.EmploymentCommands;
import com.xiaoliang.simukraft.employment.service.EmploymentErrorCode;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmploymentServiceDualSlotTest {

    @Test
    void buildBoxAllowsBuilderAndPlannerAtSamePosition() {
        var service = new DefaultEmploymentService(new InMemoryEmploymentRepository());
        BlockPos pos = new BlockPos(10, 64, 20);

        var builderResult = service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.BUILD_BOX,
                JobType.BUILDER
        ));
        var plannerResult = service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.BUILD_BOX,
                JobType.PLANNER
        ));

        assertTrue(builderResult.success(), "builder should be hired into the first build box slot");
        assertTrue(plannerResult.success(), "planner should be hired into the second build box slot");
        assertEquals(2, service.listAssignedByWorkplace("minecraft:overworld", pos).size());
        assertTrue(service.findByWorkplaceAndJob("minecraft:overworld", pos, JobType.BUILDER).isPresent());
        assertTrue(service.findByWorkplaceAndJob("minecraft:overworld", pos, JobType.PLANNER).isPresent());
    }

    @Test
    void buildBoxRejectsSecondBuilderAtSamePosition() {
        var service = new DefaultEmploymentService(new InMemoryEmploymentRepository());
        BlockPos pos = new BlockPos(1, 70, 1);

        assertTrue(service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.BUILD_BOX,
                JobType.BUILDER
        )).success());

        var secondBuilder = service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.BUILD_BOX,
                JobType.BUILDER
        ));

        assertFalse(secondBuilder.success());
        assertEquals(EmploymentErrorCode.WORKPLACE_ALREADY_OCCUPIED, secondBuilder.code());
    }

    @Test
    void nonBuildBoxStillAllowsOnlyOneAssignmentPerWorkplace() {
        var service = new DefaultEmploymentService(new InMemoryEmploymentRepository());
        BlockPos pos = new BlockPos(5, 65, 5);

        assertTrue(service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.INDUSTRIAL_CONTROL_BOX,
                JobType.INDUSTRIAL_GENERIC
        )).success());

        var secondIndustrialHire = service.hire(new EmploymentCommands.HireCommand(
                UUID.randomUUID(),
                "minecraft:overworld",
                pos,
                WorkBlockType.INDUSTRIAL_CONTROL_BOX,
                JobType.COMMERCIAL_GENERIC
        ));

        assertFalse(secondIndustrialHire.success());
        assertEquals(EmploymentErrorCode.WORKPLACE_ALREADY_OCCUPIED, secondIndustrialHire.code());
    }

    private static final class InMemoryEmploymentRepository implements EmploymentRepository {
        private List<EmploymentAssignment> assignments = new ArrayList<>();

        @Override
        public List<EmploymentAssignment> loadAll() {
            return new ArrayList<>(assignments);
        }

        @Override
        public boolean saveAll(List<EmploymentAssignment> assignments) {
            this.assignments = new ArrayList<>(assignments);
            return true;
        }
    }
}

