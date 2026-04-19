package com.xiaoliang.simukraft.client;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC居住信息客户端缓存
 * 用于存储从服务端获取的NPC居住信息
 */
public class NPCResidenceCache {
    
    public static class ResidenceInfo {
        public final boolean hasResidence;
        public final String position;
        public final long timestamp;
        
        public ResidenceInfo(boolean hasResidence, String position) {
            this.hasResidence = hasResidence;
            this.position = position;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private static final Map<String, ResidenceInfo> cache = new HashMap<>();
    private static final long CACHE_VALIDITY = 30000; // 缓存有效期30秒
    
    /**
     * 设置NPC居住信息
     */
    public static void setResidenceInfo(String npcName, boolean hasResidence, String position) {
        cache.put(npcName, new ResidenceInfo(hasResidence, position));
    }
    
    /**
     * 获取NPC居住信息
     */
    public static ResidenceInfo getResidenceInfo(String npcName) {
        ResidenceInfo info = cache.get(npcName);
        if (info == null) {
            return null;
        }
        
        // 检查缓存是否过期
        if (System.currentTimeMillis() - info.timestamp > CACHE_VALIDITY) {
            cache.remove(npcName);
            return null;
        }
        
        return info;
    }
    
    /**
     * 检查是否有缓存的居住信息
     */
    public static boolean hasCachedInfo(String npcName) {
        return getResidenceInfo(npcName) != null;
    }
    
    /**
     * 清除所有缓存
     */
    public static void clearCache() {
        cache.clear();
    }
}
