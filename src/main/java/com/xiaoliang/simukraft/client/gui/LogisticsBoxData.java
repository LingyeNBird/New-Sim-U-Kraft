package com.xiaoliang.simukraft.client.gui;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 物流盒子客户端数据缓存 - 类似 BuildBoxData
 */
public class LogisticsBoxData {
    // 服务端盒子雇佣状态: 位置 -> NPC UUID
    private static final Map<BlockPos, UUID> serverBoxHiredNpcs = new HashMap<>();
    // 客户端盒子雇佣状态: 位置 -> NPC UUID
    private static final Map<BlockPos, UUID> clientBoxHiredNpcs = new HashMap<>();

    public static void setServerBoxHired(BlockPos pos, UUID npcUuid) {
        serverBoxHiredNpcs.put(pos, npcUuid);
    }

    public static void setClientBoxHired(BlockPos pos, UUID npcUuid) {
        clientBoxHiredNpcs.put(pos, npcUuid);
    }

    public static boolean hasServerBoxHired(BlockPos pos) {
        return serverBoxHiredNpcs.containsKey(pos);
    }

    public static boolean hasClientBoxHired(BlockPos pos) {
        return clientBoxHiredNpcs.containsKey(pos);
    }

    public static UUID getServerBoxHiredUuid(BlockPos pos) {
        return serverBoxHiredNpcs.get(pos);
    }

    public static UUID getClientBoxHiredUuid(BlockPos pos) {
        return clientBoxHiredNpcs.get(pos);
    }

    public static void clearServerBoxHired(BlockPos pos) {
        serverBoxHiredNpcs.remove(pos);
    }

    public static void clearClientBoxHired(BlockPos pos) {
        clientBoxHiredNpcs.remove(pos);
    }
}
