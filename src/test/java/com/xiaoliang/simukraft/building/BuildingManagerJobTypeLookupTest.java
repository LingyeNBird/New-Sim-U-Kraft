package com.xiaoliang.simukraft.building;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingManagerJobTypeLookupTest {

    @AfterEach
    void cleanup() throws Exception {
        getIndustrialConfigs().clear();
        getCommercialConfigs().clear();
    }

    @Test
    void industrialLookupSkipsConfigsWithoutJobType() throws Exception {
        IndustrialBuildingConfig malformed = new IndustrialBuildingConfig();
        malformed.setBuildingId("broken-industrial");

        IndustrialBuildingConfig valid = new IndustrialBuildingConfig();
        valid.setBuildingId("valid-industrial");
        valid.setJobType("shepherd");
        valid.setJobName("牧羊人");

        Map<String, IndustrialBuildingConfig> configs = getIndustrialConfigs();
        configs.put("broken-industrial", malformed);
        configs.put("valid-industrial", valid);

        assertEquals(1, IndustrialBuildingManager.getConfigsByJobType("shepherd").size());
        assertEquals("valid-industrial", IndustrialBuildingManager.getConfigsByJobType("shepherd").get(0).getBuildingId());
        assertTrue(IndustrialBuildingManager.getConfigsByJobType(null).isEmpty());
        assertTrue(IndustrialBuildingManager.getConfigsByJobType("   ").isEmpty());
    }

    @Test
    void commercialLookupSkipsConfigsWithoutJobType() throws Exception {
        CommercialBuildingConfig malformed = new CommercialBuildingConfig();
        malformed.setBuildingId("broken-commercial");

        CommercialBuildingConfig valid = new CommercialBuildingConfig();
        valid.setBuildingId("valid-commercial");
        valid.setJobType("doctor");
        valid.setJobName("医生");

        Map<String, CommercialBuildingConfig> configs = getCommercialConfigs();
        configs.put("broken-commercial", malformed);
        configs.put("valid-commercial", valid);

        assertEquals(1, CommercialBuildingManager.getConfigsByJobType("doctor").size());
        assertEquals("valid-commercial", CommercialBuildingManager.getConfigsByJobType("doctor").get(0).getBuildingId());
        assertTrue(CommercialBuildingManager.getConfigsByJobType(null).isEmpty());
        assertTrue(CommercialBuildingManager.getConfigsByJobType("   ").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, IndustrialBuildingConfig> getIndustrialConfigs() throws Exception {
        Field field = IndustrialBuildingManager.class.getDeclaredField("buildingConfigs");
        field.setAccessible(true);
        return (Map<String, IndustrialBuildingConfig>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CommercialBuildingConfig> getCommercialConfigs() throws Exception {
        Field field = CommercialBuildingManager.class.getDeclaredField("buildingConfigs");
        field.setAccessible(true);
        return (Map<String, CommercialBuildingConfig>) field.get(null);
    }
}

