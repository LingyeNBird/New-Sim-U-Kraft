package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.JobType;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmploymentQueryService {
    Optional<EmploymentAssignment> findByNpc(UUID npcUuid);

    Optional<EmploymentAssignment> findByWorkplace(String dimensionId, BlockPos pos);

    Optional<EmploymentAssignment> findByWorkplaceAndJob(String dimensionId, BlockPos pos, JobType jobType);

    List<EmploymentAssignment> listAssignedByWorkplace(String dimensionId, BlockPos pos);

    List<EmploymentAssignment> listByCity(UUID cityId);
}

