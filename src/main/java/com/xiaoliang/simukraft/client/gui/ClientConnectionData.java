package com.xiaoliang.simukraft.client.gui;

import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * 客户端连接数据缓存
 * 用于在客户端缓存物流客户端盒子的容器连接信息
 */
public class ClientConnectionData {
    // 缓存每个客户端盒子的数据：blockPos -> ClientData
    private static final Map<BlockPos, ClientData> clientDataMap = new HashMap<>();

    /**
     * 客户端数据
     */
    public static class ClientData {
        private final UUID clientId;
        private final List<BlockPos> containerPositions;

        public ClientData(UUID clientId, List<BlockPos> containerPositions) {
            this.clientId = clientId;
            this.containerPositions = new ArrayList<>(containerPositions);
        }

        public UUID getClientId() {
            return clientId;
        }

        public List<BlockPos> getContainerPositions() {
            return Collections.unmodifiableList(containerPositions);
        }

        public boolean hasContainers() {
            return !containerPositions.isEmpty();
        }

        public int getContainerCount() {
            return containerPositions.size();
        }
    }

    /**
     * 更新客户端数据
     */
    public static void updateClientData(BlockPos blockPos, UUID clientId, List<BlockPos> containerPositions) {
        clientDataMap.put(blockPos, new ClientData(clientId, containerPositions));
    }

    /**
     * 获取客户端数据
     */
    public static ClientData getClientData(BlockPos blockPos) {
        return clientDataMap.get(blockPos);
    }

    /**
     * 检查是否有容器连接
     */
    public static boolean hasConnectedContainers(BlockPos blockPos) {
        ClientData data = clientDataMap.get(blockPos);
        return data != null && data.hasContainers();
    }

    /**
     * 获取连接的容器数量
     */
    public static int getContainerCount(BlockPos blockPos) {
        ClientData data = clientDataMap.get(blockPos);
        return data != null ? data.getContainerCount() : 0;
    }

    /**
     * 获取容器位置列表
     */
    public static List<BlockPos> getContainerPositions(BlockPos blockPos) {
        ClientData data = clientDataMap.get(blockPos);
        return data != null ? data.getContainerPositions() : Collections.emptyList();
    }

    /**
     * 清除指定客户端盒子的数据
     */
    public static void removeClientData(BlockPos blockPos) {
        clientDataMap.remove(blockPos);
    }

    /**
     * 清除所有缓存数据
     */
    public static void clear() {
        clientDataMap.clear();
    }
}
