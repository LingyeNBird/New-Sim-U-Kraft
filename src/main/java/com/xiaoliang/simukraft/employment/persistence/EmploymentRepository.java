package com.xiaoliang.simukraft.employment.persistence;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;

import java.util.List;

public interface EmploymentRepository {
    List<EmploymentAssignment> loadAll();

    boolean saveAll(List<EmploymentAssignment> assignments);
}

