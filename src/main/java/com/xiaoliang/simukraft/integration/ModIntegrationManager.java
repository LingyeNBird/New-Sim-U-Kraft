package com.xiaoliang.simukraft.integration;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 模组集成管理器。
 * 在启动时通过 Class.forName 检测可选依赖模组是否存在，
 * 供其他系统安全地按需调用对应集成逻辑。
 */
public class ModIntegrationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean xaeroWorldMapPresent = false;
    private static boolean xaeroMinimapPresent = false;
    private static boolean openPACPresent = false;
    private static boolean ftbChunksPresent = false;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Xaero's World Map
        try {
            Class.forName("xaero.map.WorldMap");
            xaeroWorldMapPresent = true;
            LOGGER.info("Simukraft: Xaero's World Map detected, enabling map integration.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Simukraft: Xaero's World Map not found, map integration disabled.");
        }

        // Xaero's Minimap
        try {
            Class.forName("xaero.common.XaeroMinimapSession");
            xaeroMinimapPresent = true;
            LOGGER.info("Simukraft: Xaero's Minimap detected, enabling minimap integration.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Simukraft: Xaero's Minimap not found, minimap integration disabled.");
        }

        // Open Parties And Claims
        try {
            Class.forName("xaero.pac.OpenPartiesAndClaims");
            openPACPresent = true;
            LOGGER.info("Simukraft: Open Parties And Claims detected, enabling claims integration.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Simukraft: Open Parties And Claims not found, claims integration disabled.");
        }

        // FTB Chunks
        try {
            Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            ftbChunksPresent = true;
            LOGGER.info("Simukraft: FTB Chunks detected, enabling FTB claims integration.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Simukraft: FTB Chunks not found, FTB claims integration disabled.");
        }
    }

    /** Xaero's World Map 是否存在 */
    public static boolean isXaeroWorldMapPresent() {
        return xaeroWorldMapPresent;
    }

    /** Xaero's Minimap 是否存在 */
    public static boolean isXaeroMinimapPresent() {
        return xaeroMinimapPresent;
    }

    /** 是否有任何 Xaero 地图模组存在 */
    public static boolean isAnyXaeroPresent() {
        return xaeroWorldMapPresent || xaeroMinimapPresent;
    }

    /** Open Parties And Claims 是否存在 */
    public static boolean isOpenPACPresent() {
        return openPACPresent;
    }

    /** FTB Chunks 是否存在 */
    public static boolean isFTBChunksPresent() {
        return ftbChunksPresent;
    }
}
