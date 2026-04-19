package com.xiaoliang.simukraft.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialHiredDataCacheTest {

    @Test
    void reusesCacheOnlyWhenSourceTimestampIsUnchanged() {
        assertTrue(
                CommercialHiredData.shouldReuseHiredEmployeesCache(true, 123L, 123L),
                "unchanged employment data should keep the commercial hire cache hot"
        );
        assertFalse(
                CommercialHiredData.shouldReuseHiredEmployeesCache(true, 123L, 124L),
                "changed employment data must invalidate the commercial hire cache"
        );
        assertFalse(
                CommercialHiredData.shouldReuseHiredEmployeesCache(false, 123L, 123L),
                "an unloaded cache must not be reused even if timestamps match"
        );
    }
}
