package com.xiaoliang.simukraft.client.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 物流客户端数据缓存
 * 用于在客户端缓存物流客户端的名称等信息
 */
public class LogisticsClientData {
    private static final Map<UUID, String> clientNames = new HashMap<>();

    /**
     * 更新客户端名称
     */
    public static void updateClientName(UUID clientId, String name) {
        clientNames.put(clientId, name);
    }

    /**
     * 获取客户端名称
     */
    public static String getClientName(UUID clientId) {
        return clientNames.getOrDefault(clientId, "");
    }

    /**
     * 清除所有缓存数据
     */
    public static void clear() {
        clientNames.clear();
    }
}
