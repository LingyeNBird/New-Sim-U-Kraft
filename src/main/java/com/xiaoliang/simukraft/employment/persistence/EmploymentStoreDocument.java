package com.xiaoliang.simukraft.employment.persistence;

import java.util.ArrayList;
import java.util.List;

class EmploymentStoreDocument {
    int schemaVersion = 2;
    List<StoredAssignment> assignments = new ArrayList<>();

    static class StoredAssignment {
        String npcUuid;
        String dimensionId;
        int x;
        int y;
        int z;
        String workBlockType;
        String jobType;
        String status;
        long version;
        long updatedAtEpochMs;
    }
}

