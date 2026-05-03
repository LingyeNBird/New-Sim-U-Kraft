package com.xiaoliang.simukraft.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 规划任务数据管理器
 * 用于持久化存储规划师的规划任务信息
 * 解决局域网开放模式下NPC休息后规划任务丢失的问题
 */
public class PlanningTaskData {
    private static final Gson gson = new Gson();
    private static final String FILE_NAME = "planning_tasks.json";
    private static final String MODE_DIR = "simukraft";

    /**
     * 规划任务信息类
     */
    public static class TaskInfo {
        public final UUID taskId;
        public final UUID npcId;
        public final BlockPos buildBoxPos;
        public final String taskType;  // REPLACE, FILL, REMOVE
        public final String status;    // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
        public final List<BlockPos> targetBlocks;
        public final int currentBlockIndex;
        public final String targetBlockId;  // 目标方块ID（替换/填充用）
        public final Map<String, String> replacementMap;  // 替换映射
        public final long createTime;

        public TaskInfo(UUID taskId, UUID npcId, BlockPos buildBoxPos, String taskType,
                       String status, List<BlockPos> targetBlocks, int currentBlockIndex,
                       String targetBlockId, Map<String, String> replacementMap, long createTime) {
            this.taskId = taskId;
            this.npcId = npcId;
            this.buildBoxPos = buildBoxPos;
            this.taskType = taskType;
            this.status = status;
            this.targetBlocks = new ArrayList<>(targetBlocks);
            this.currentBlockIndex = currentBlockIndex;
            this.targetBlockId = targetBlockId;
            this.replacementMap = replacementMap != null ? new HashMap<>(replacementMap) : new HashMap<>();
            this.createTime = createTime;
        }
    }

    /**
     * 保存规划任务
     * @param server 服务器实例
     * @param taskInfo 任务信息
     */
    public static void saveTask(MinecraftServer server, TaskInfo taskInfo) {
        if (server == null || taskInfo == null) return;

        try {
            Map<UUID, TaskInfo> allTasks = loadAllTasks(server);
            allTasks.put(taskInfo.taskId, taskInfo);
            saveAllTasks(server, allTasks);
            Simukraft.LOGGER.info("[PlanningTaskData] 保存规划任务 - Task: {}, NPC: {}, 类型: {}",
                taskInfo.taskId.toString().substring(0, 8),
                taskInfo.npcId.toString().substring(0, 8),
                taskInfo.taskType);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 保存规划任务失败", e);
        }
    }

    /**
     * 加载规划任务
     * @param server 服务器实例
     * @param taskId 任务ID
     * @return 任务信息，如果没有找到返回null
     */
    public static TaskInfo loadTask(MinecraftServer server, UUID taskId) {
        if (server == null || taskId == null) return null;

        try {
            Map<UUID, TaskInfo> allTasks = loadAllTasks(server);
            return allTasks.get(taskId);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 加载规划任务失败", e);
            return null;
        }
    }

    /**
     * 根据NPC UUID加载规划任务
     * @param server 服务器实例
     * @param npcUuid NPC的UUID
     * @return 任务信息，如果没有找到返回null
     */
    public static TaskInfo loadTaskByNpc(MinecraftServer server, UUID npcUuid) {
        if (server == null || npcUuid == null) return null;

        try {
            Map<UUID, TaskInfo> allTasks = loadAllTasks(server);
            for (TaskInfo task : allTasks.values()) {
                if (task.npcId.equals(npcUuid)) {
                    return task;
                }
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 加载规划任务失败", e);
        }
        return null;
    }

    /**
     * 移除规划任务
     * @param server 服务器实例
     * @param taskId 任务ID
     */
    public static void removeTask(MinecraftServer server, UUID taskId) {
        if (server == null || taskId == null) return;

        try {
            Map<UUID, TaskInfo> allTasks = loadAllTasks(server);
            if (allTasks.remove(taskId) != null) {
                saveAllTasks(server, allTasks);
                Simukraft.LOGGER.info("[PlanningTaskData] 移除规划任务 - Task: {}",
                    taskId.toString().substring(0, 8));
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 移除规划任务失败", e);
        }
    }

    /**
     * 根据NPC UUID移除规划任务
     * @param server 服务器实例
     * @param npcUuid NPC的UUID
     */
    public static void removeTaskByNpc(MinecraftServer server, UUID npcUuid) {
        if (server == null || npcUuid == null) return;

        try {
            Map<UUID, TaskInfo> allTasks = loadAllTasks(server);
            boolean removed = allTasks.values().removeIf(task -> task.npcId.equals(npcUuid));
            if (removed) {
                saveAllTasks(server, allTasks);
                Simukraft.LOGGER.info("[PlanningTaskData] 移除规划任务 - NPC: {}",
                    npcUuid.toString().substring(0, 8));
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 移除规划任务失败", e);
        }
    }

    /**
     * 检查是否有规划任务
     * @param server 服务器实例
     * @param taskId 任务ID
     * @return 如果有任务返回true
     */
    public static boolean hasTask(MinecraftServer server, UUID taskId) {
        return loadTask(server, taskId) != null;
    }

    /**
     * 检查NPC是否有规划任务
     * @param server 服务器实例
     * @param npcUuid NPC的UUID
     * @return 如果有任务返回true
     */
    public static boolean hasTaskByNpc(MinecraftServer server, UUID npcUuid) {
        return loadTaskByNpc(server, npcUuid) != null;
    }

    @SuppressWarnings("null")
    private static Path getWorldPath(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return server.getWorldPath(LevelResource.ROOT);
    }

    /**
     * 加载所有规划任务
     */
    private static Map<UUID, TaskInfo> loadAllTasks(MinecraftServer server) {
        Map<UUID, TaskInfo> result = new HashMap<>();

        try {
            Path worldDir = getWorldPath(server);
            if (worldDir == null) {
                return result;
            }
            Path simukraftDir = worldDir.resolve(MODE_DIR);
            Path dataFile = simukraftDir.resolve(FILE_NAME);

            if (!Files.exists(dataFile) || Files.size(dataFile) == 0L) {
                return result;
            }

            JsonObject data;
            try (var reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
                data = JsonParser.parseReader(reader).getAsJsonObject();
            }

            for (String key : data.keySet()) {
                try {
                    UUID taskId = UUID.fromString(key);
                    JsonObject taskObj = data.getAsJsonObject(key);

                    UUID npcId = UUID.fromString(taskObj.get("npcId").getAsString());
                    String taskType = taskObj.get("taskType").getAsString();
                    String status = taskObj.get("status").getAsString();
                    int currentBlockIndex = taskObj.get("currentBlockIndex").getAsInt();
                    long createTime = taskObj.get("createTime").getAsLong();

                    BlockPos buildBoxPos = new BlockPos(
                        taskObj.get("buildBoxPosX").getAsInt(),
                        taskObj.get("buildBoxPosY").getAsInt(),
                        taskObj.get("buildBoxPosZ").getAsInt()
                    );

                    // 加载目标方块列表
                    List<BlockPos> targetBlocks = new ArrayList<>();
                    if (taskObj.has("targetBlocks")) {
                        JsonArray blocksArray = taskObj.getAsJsonArray("targetBlocks");
                        for (int i = 0; i < blocksArray.size(); i++) {
                            JsonObject posObj = blocksArray.get(i).getAsJsonObject();
                            BlockPos pos = new BlockPos(
                                posObj.get("x").getAsInt(),
                                posObj.get("y").getAsInt(),
                                posObj.get("z").getAsInt()
                            );
                            targetBlocks.add(pos);
                        }
                    }

                    // 加载目标方块ID
                    String targetBlockId = null;
                    if (taskObj.has("targetBlockId")) {
                        targetBlockId = taskObj.get("targetBlockId").getAsString();
                    }

                    // 加载替换映射
                    Map<String, String> replacementMap = new HashMap<>();
                    if (taskObj.has("replacementMap")) {
                        JsonObject mapObj = taskObj.getAsJsonObject("replacementMap");
                        for (String mapKey : mapObj.keySet()) {
                            replacementMap.put(mapKey, mapObj.get(mapKey).getAsString());
                        }
                    }

                    TaskInfo taskInfo = new TaskInfo(
                        taskId, npcId, buildBoxPos, taskType, status,
                        targetBlocks, currentBlockIndex, targetBlockId,
                        replacementMap, createTime
                    );

                    result.put(taskId, taskInfo);
                } catch (Exception e) {
                    Simukraft.LOGGER.error("[PlanningTaskData] 解析任务数据失败: {}", key, e);
                }
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 加载所有任务失败", e);
        }

        return result;
    }

    /**
     * 保存所有规划任务
     */
    private static void saveAllTasks(MinecraftServer server, Map<UUID, TaskInfo> tasks) {
        try {
            Path worldDir = getWorldPath(server);
            if (worldDir == null) {
                return;
            }
            Path simukraftDir = worldDir.resolve(MODE_DIR);
            Path dataFile = simukraftDir.resolve(FILE_NAME);

            if (!Files.exists(simukraftDir)) {
                Files.createDirectories(simukraftDir);
            }

            JsonObject data = new JsonObject();

            for (Map.Entry<UUID, TaskInfo> entry : tasks.entrySet()) {
                TaskInfo task = entry.getValue();
                UUID taskId = entry.getKey();
                // 跳过必要字段为空的任务
                if (task == null || taskId == null || task.npcId == null || task.buildBoxPos == null || task.targetBlocks == null) {
                    continue;
                }

                JsonObject taskObj = new JsonObject();

                taskObj.addProperty("npcId", task.npcId.toString());
                taskObj.addProperty("taskType", task.taskType);
                taskObj.addProperty("status", task.status);
                taskObj.addProperty("currentBlockIndex", task.currentBlockIndex);
                taskObj.addProperty("createTime", task.createTime);

                taskObj.addProperty("buildBoxPosX", task.buildBoxPos.getX());
                taskObj.addProperty("buildBoxPosY", task.buildBoxPos.getY());
                taskObj.addProperty("buildBoxPosZ", task.buildBoxPos.getZ());

                // 保存目标方块列表
                JsonArray blocksArray = new JsonArray();
                for (BlockPos pos : task.targetBlocks) {
                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("x", pos.getX());
                    posObj.addProperty("y", pos.getY());
                    posObj.addProperty("z", pos.getZ());
                    blocksArray.add(posObj);
                }
                taskObj.add("targetBlocks", blocksArray);

                // 保存目标方块ID
                if (task.targetBlockId != null) {
                    taskObj.addProperty("targetBlockId", task.targetBlockId);
                }

                // 保存替换映射
                if (!task.replacementMap.isEmpty()) {
                    JsonObject mapObj = new JsonObject();
                    for (Map.Entry<String, String> mapEntry : task.replacementMap.entrySet()) {
                        mapObj.addProperty(mapEntry.getKey(), mapEntry.getValue());
                    }
                    taskObj.add("replacementMap", mapObj);
                }

                data.add(taskId.toString(), taskObj);
            }

            try (var writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlanningTaskData] 保存所有任务失败", e);
        }
    }
}
