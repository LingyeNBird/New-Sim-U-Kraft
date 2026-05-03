package com.xiaoliang.simukraft.integration.xaero;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import xaero.map.highlight.AbstractHighlighter;

import java.util.List;

public class XaeroWorldMapIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static XaeroWorldMapIntegration instance;

    private XaeroWorldMapIntegration() {
    }

    public static XaeroWorldMapIntegration getInstance() {
        if (instance == null) {
            instance = new XaeroWorldMapIntegration();
        }
        return instance;
    }

    public static void init() {
        getInstance();
        LOGGER.info("Simukraft: XaeroWorldMap integration initialized.");
    }

    public void registerHighlighters(List<AbstractHighlighter> highlighters) {
        try {
            highlighters.add(new SimukraftCityHighlighter());
            LOGGER.info("Simukraft: City highlighter registered to Xaero's World Map.");
        } catch (Throwable t) {
            LOGGER.error("Simukraft: Failed to register city highlighter.", t);
        }
    }

    public static void onSessionInit(List<AbstractHighlighter> highlighters) {
        if (instance != null) {
            instance.registerHighlighters(highlighters);
        }
    }
}
