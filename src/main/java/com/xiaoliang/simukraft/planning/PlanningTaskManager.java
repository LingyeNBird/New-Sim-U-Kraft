package com.xiaoliang.simukraft.planning;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规划任务管理器
 * 管理所有规划任务的创建、执行以及持久化
 */
public class PlanningTaskManager extends SavedData {

    private static final String DATA_NAME = "simukraft_planning_tasks";
    private static PlanningTaskManager instance;

    // 所有任务按任务ID存储
    private final Map<UUID, PlanningTask> tasks = new ConcurrentHashMap<>();

    // 按建筑盒位置索引的任务
    private final Map<BlockPos, List<UUID>> tasksByBuildBox = new ConcurrentHashMap<>();

    // 按NPC索引的任务
    private final Map<UUID, List<UUID>> tasksByNpc = new ConcurrentHashMap<>();

    // 活跃任务（进行中）
    private final Set<UUID> activeTasks = ConcurrentHashMap.newKeySet();

    public PlanningTaskManager() {
    }

    /**
     * 获取管理器实例
     */
    public static PlanningTaskManager get(Level level) {
        if (level.isClientSide) {
            return instance != null ? instance : new PlanningTaskManager();
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return new PlanningTaskManager();
        }

        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            PlanningTaskManager::load,
            PlanningTaskManager::new,
            DATA_NAME
        );
    }

    /**
     * 设置实例（客户端使用）
     */
    public static void setInstance(PlanningTaskManager manager) {
        instance = manager;
    }

    /**
     * 创建新任务
     */
    public PlanningTask createTask(UUID npcId, BlockPos buildBoxPos, PlanningTask.TaskType type, List<BlockPos> targetBlocks) {
        PlanningTask task = new PlanningTask(npcId, buildBoxPos, type, targetBlocks);
        task.setStatus(PlanningTask.TaskStatus.PENDING);

        // 设置变更回调
        task.setOnChangedCallback(this::setDirty);

        tasks.put(task.getTaskId(), task);

        // 添加到索引
        tasksByBuildBox.computeIfAbsent(buildBoxPos, k -> new ArrayList<>()).add(task.getTaskId());
        tasksByNpc.computeIfAbsent(npcId, k -> new ArrayList<>()).add(task.getTaskId());

        setDirty();
        Simukraft.LOGGER.info("[PlanningTaskManager] 创建任务: {} 类型: {} 方块数: {}",
            task.getTaskId(), type, targetBlocks.size());

        return task;
    }

    /**
     * 添加已存在的任务（用于从JSON恢复）
     */
    public void addTask(PlanningTask task) {
        if (task == null || task.getTaskId() == null) return;

        // 设置变更回调
        task.setOnChangedCallback(this::setDirty);

        tasks.put(task.getTaskId(), task);

        // 添加到索引
        if (task.getBuildBoxPos() != null) {
            tasksByBuildBox.computeIfAbsent(task.getBuildBoxPos(), k -> new ArrayList<>()).add(task.getTaskId());
        }
        if (task.getNpcId() != null) {
            tasksByNpc.computeIfAbsent(task.getNpcId(), k -> new ArrayList<>()).add(task.getTaskId());
        }

        // 如果是进行中状态，添加到活跃任务集合
        if (task.getStatus() == PlanningTask.TaskStatus.IN_PROGRESS) {
            activeTasks.add(task.getTaskId());
        }

        setDirty();
        Simukraft.LOGGER.info("[PlanningTaskManager] 添加任务: {} 类型: {} 状态: {}",
            task.getTaskId(), task.getType(), task.getStatus());
    }

    /**
     * 开始执行任务
     */
    public boolean startTask(UUID taskId) {
        PlanningTask task = tasks.get(taskId);
        if (task == null) return false;

        if (task.getStatus() == PlanningTask.TaskStatus.PENDING) {
            task.setStatus(PlanningTask.TaskStatus.IN_PROGRESS);
            activeTasks.add(taskId);
            setDirty();

            Simukraft.LOGGER.info("[PlanningTaskManager] 开始执行任务: {}", taskId);
            return true;
        }
        return false;
    }

    /**
     * 完成任务
     */
    public boolean completeTask(UUID taskId) {
        PlanningTask task = tasks.get(taskId);
        if (task == null) return false;

        task.setStatus(PlanningTask.TaskStatus.COMPLETED);
        activeTasks.remove(taskId);
        setDirty();

        Simukraft.LOGGER.info("[PlanningTaskManager] 完成任务: {}", taskId);
        return true;
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(UUID taskId) {
        PlanningTask task = tasks.get(taskId);
        if (task == null) return false;

        task.setStatus(PlanningTask.TaskStatus.CANCELLED);
        activeTasks.remove(taskId);
        setDirty();

        Simukraft.LOGGER.info("[PlanningTaskManager] 取消任务: {}", taskId);
        return true;
    }

    /**
     * 获取任务
     */
    public PlanningTask getTask(UUID taskId) {
        return tasks.get(taskId);
    }

    /**
     * 获取建筑盒的所有任务
     */
    public List<PlanningTask> getTasksByBuildBox(BlockPos buildBoxPos) {
        List<UUID> taskIds = tasksByBuildBox.get(buildBoxPos);
        if (taskIds == null) return new ArrayList<>();

        List<PlanningTask> result = new ArrayList<>();
        for (UUID taskId : taskIds) {
            PlanningTask task = tasks.get(taskId);
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 获取NPC的所有任务
     */
    public List<PlanningTask> getTasksByNpc(UUID npcId) {
        List<UUID> taskIds = tasksByNpc.get(npcId);
        if (taskIds == null) return new ArrayList<>();

        List<PlanningTask> result = new ArrayList<>();
        for (UUID taskId : taskIds) {
            PlanningTask task = tasks.get(taskId);
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 获取NPC的活跃任务
     */
    public PlanningTask getActiveTaskByNpc(UUID npcId) {
        for (UUID taskId : activeTasks) {
            PlanningTask task = tasks.get(taskId);
            if (task != null && npcId.equals(task.getNpcId())) {
                return task;
            }
        }
        return null;
    }

    /**
     * 获取所有活跃任务
     */
    public List<PlanningTask> getAllActiveTasks() {
        List<PlanningTask> result = new ArrayList<>();
        for (UUID taskId : activeTasks) {
            PlanningTask task = tasks.get(taskId);
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 获取建筑盒的活跃任务
     */
    public PlanningTask getActiveTaskByBuildBox(BlockPos buildBoxPos) {
        for (UUID taskId : activeTasks) {
            PlanningTask task = tasks.get(taskId);
            if (task != null && buildBoxPos.equals(task.getBuildBoxPos())) {
                return task;
            }
        }
        return null;
    }

    /**
     * 清理已完成的任务
     */
    public void cleanupCompletedTasks() {
        Iterator<Map.Entry<UUID, PlanningTask>> iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlanningTask> entry = iterator.next();
            PlanningTask task = entry.getValue();

            if (task.getStatus() == PlanningTask.TaskStatus.COMPLETED ||
                task.getStatus() == PlanningTask.TaskStatus.CANCELLED) {
                // 从索引中移除
                tasksByBuildBox.getOrDefault(task.getBuildBoxPos(), new ArrayList<>())
                    .remove(task.getTaskId());
                tasksByNpc.getOrDefault(task.getNpcId(), new ArrayList<>())
                    .remove(task.getTaskId());
                activeTasks.remove(task.getTaskId());

                iterator.remove();
            }
        }
        setDirty();
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag tasksList = new ListTag();

        for (PlanningTask task : tasks.values()) {
            CompoundTag taskTag = new CompoundTag();
            task.save(taskTag);
            tasksList.add(taskTag);
        }

        tag.put("tasks", tasksList);
        return tag;
    }

    /**
     * 从NBT加载
     */
    public static PlanningTaskManager load(CompoundTag tag) {
        PlanningTaskManager manager = new PlanningTaskManager();

        if (tag.contains("tasks", Tag.TAG_LIST)) {
            ListTag tasksList = tag.getList("tasks", Tag.TAG_COMPOUND);

            for (int i = 0; i < tasksList.size(); i++) {
                CompoundTag taskTag = tasksList.getCompound(i);
                PlanningTask task = PlanningTask.load(taskTag);

                // 设置变更回调
                task.setOnChangedCallback(manager::setDirty);

                manager.tasks.put(task.getTaskId(), task);

                // 重建索引
                if (task.getBuildBoxPos() != null) {
                    manager.tasksByBuildBox
                        .computeIfAbsent(task.getBuildBoxPos(), k -> new ArrayList<>())
                        .add(task.getTaskId());
                }
                if (task.getNpcId() != null) {
                    manager.tasksByNpc
                        .computeIfAbsent(task.getNpcId(), k -> new ArrayList<>())
                        .add(task.getTaskId());
                }

                // 重建活跃任务集合
                if (task.getStatus() == PlanningTask.TaskStatus.IN_PROGRESS) {
                    manager.activeTasks.add(task.getTaskId());
                }
            }
        }

        Simukraft.LOGGER.info("[PlanningTaskManager] 加载了 {} 个任务", manager.tasks.size());
        return manager;
    }
}
