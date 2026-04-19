package com.xiaoliang.simukraft.building;

import com.xiaoliang.simukraft.utils.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BuildingMetadataSafetyTest {

    @Test
    void normalizeBuildingFileNameRemovesExtensionAndWhitespace() {
        assertEquals("YYJ", FileUtils.normalizeBuildingFileName(" YYJ.sk "));
        assertEquals("NNJ", FileUtils.normalizeBuildingFileName("NNJ"));
        assertEquals("kc", FileUtils.normalizeBuildingFileName("kc.SK"));
    }

    @Test
    void normalizeBuildingFileNameReturnsNullForEmptyValues() {
        assertNull(FileUtils.normalizeBuildingFileName(null));
        assertNull(FileUtils.normalizeBuildingFileName(""));
        assertNull(FileUtils.normalizeBuildingFileName("   "));
        assertNull(FileUtils.normalizeBuildingFileName(".sk"));
    }

    @Test
    void managersReturnNullForBlankBuildingIds() {
        assertNull(IndustrialBuildingManager.getConfig(null));
        assertNull(IndustrialBuildingManager.getConfig("   "));
        assertNull(CommercialBuildingManager.getConfig(null));
        assertNull(CommercialBuildingManager.getConfig("   "));
    }
}

