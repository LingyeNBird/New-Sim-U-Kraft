package com.xiaoliang.simukraft.client;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NPCFamilyInfoCache {
    private static final Map<UUID, FamilyInfo> CACHE = new ConcurrentHashMap<>();

    private NPCFamilyInfoCache() {
    }

    public record FamilyInfo(String spouseName, String pregnancyStage) {
    }

    @Nullable
    public static FamilyInfo get(UUID npcUuid) {
        return npcUuid == null ? null : CACHE.get(npcUuid);
    }

    public static void put(UUID npcUuid, String spouseName, String pregnancyStage) {
        if (npcUuid == null) {
            return;
        }
        CACHE.put(npcUuid, new FamilyInfo(spouseName, pregnancyStage));
    }

    public static void clear() {
        CACHE.clear();
    }
}
