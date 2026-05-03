package com.xiaoliang.simukraft.planning;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 规划任务类
 * 记录规划任务的类型、目标方块、执行状态等信息
 */
public class PlanningTask {

    public enum TaskType {
        REPLACE,    // 替换方块
        FILL,       // 填充方块
        REMOVE      // 拆除方块
    }

    public enum TaskStatus {
        PENDING,    // 等待中
        IN_PROGRESS,// 进行中
        COMPLETED,  // 已完成
        CANCELLED   // 已取消
    }

    private UUID taskId;
    private UUID npcId;
    private BlockPos buildBoxPos;
    private TaskType type;
    private TaskStatus status;
    private List<BlockPos> targetBlocks;
    private int currentBlockIndex;
    private long createTime;
    private long completeTime;

    // 用于替换/填充任务的额外数据
    private String targetBlockId;  // 目标方块ID（替换/填充用）

    // 用于方块替换任务的映射关系：原方块ID -> 目标方块ID
    private Map<String, String> replacementMap;

    // 变更回调（用于持久化）
    private transient Runnable onChangedCallback;

    public PlanningTask() {
        this.taskId = UUID.randomUUID();
        this.targetBlocks = new ArrayList<>();
        this.currentBlockIndex = 0;
        this.status = TaskStatus.PENDING;
        this.createTime = System.currentTimeMillis();
        this.replacementMap = new HashMap<>();
    }

    public PlanningTask(UUID npcId, BlockPos buildBoxPos, TaskType type, List<BlockPos> targetBlocks) {
        this();
        this.npcId = npcId;
        this.buildBoxPos = buildBoxPos;
        this.type = type;
        this.targetBlocks = new ArrayList<>(targetBlocks);
    }

    /**
     * 获取下一个要处理的方块
     */
    public BlockPos getNextBlock() {
        if (currentBlockIndex < targetBlocks.size()) {
            return targetBlocks.get(currentBlockIndex);
        }
        return null;
    }

    /**
     * 标记当前方块已完成，移动到下一个
     */
    public void markCurrentBlockComplete() {
        currentBlockIndex++;
        if (currentBlockIndex >= targetBlocks.size()) {
            status = TaskStatus.COMPLETED;
            completeTime = System.currentTimeMillis();
        }
        // 触发变更回调
        if (onChangedCallback != null) {
            onChangedCallback.run();
        }
    }

    /**
     * 设置变更回调
     */
    public void setOnChangedCallback(Runnable callback) {
        this.onChangedCallback = callback;
    }

    /**
     * 获取剩余方块数量
     */
    public int getRemainingBlocks() {
        return targetBlocks.size() - currentBlockIndex;
    }

    /**
     * 获取总方块数量
     */
    public int getTotalBlocks() {
        return targetBlocks.size();
    }

    /**
     * 获取已完成方块数量
     */
    public int getCompletedBlocks() {
        return currentBlockIndex;
    }

    /**
     * 获取进度百分比
     */
    public float getProgress() {
        if (targetBlocks.isEmpty()) return 100f;
        return (currentBlockIndex * 100f) / targetBlocks.size();
    }

    /**
     * 序列化为NBT
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("taskId", Objects.requireNonNull(taskId));
        if (npcId != null) {
            tag.putUUID("npcId", Objects.requireNonNull(npcId));
        }
        if (buildBoxPos != null) {
            tag.put("buildBoxPos", Objects.requireNonNull(NbtUtils.writeBlockPos(buildBoxPos)));
        }
        tag.putString("type", Objects.requireNonNull(Objects.requireNonNull(type).name()));
        tag.putString("status", Objects.requireNonNull(Objects.requireNonNull(status).name()));
        tag.putInt("currentBlockIndex", currentBlockIndex);
        tag.putLong("createTime", createTime);
        tag.putLong("completeTime", completeTime);

        // 保存目标方块列表
        ListTag blocksList = new ListTag();
        for (BlockPos pos : targetBlocks) {
            blocksList.add(Objects.requireNonNull(NbtUtils.writeBlockPos(Objects.requireNonNull(pos))));
        }
        tag.put("targetBlocks", blocksList);

        if (targetBlockId != null) {
            tag.putString("targetBlockId", targetBlockId);
        }
        
        // 保存替换映射
        if (replacementMap != null && !replacementMap.isEmpty()) {
            CompoundTag mapTag = new CompoundTag();
            for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
                mapTag.putString(
                        Objects.requireNonNull(entry.getKey()),
                        Objects.requireNonNull(entry.getValue())
                );
            }
            tag.put("replacementMap", mapTag);
        }

        return tag;
    }

    /**
     * 从NBT反序列化
     */
    public static PlanningTask load(CompoundTag tag) {
        PlanningTask task = new PlanningTask();

        if (tag.hasUUID("taskId")) {
            task.taskId = tag.getUUID("taskId");
        }
        if (tag.hasUUID("npcId")) {
            task.npcId = tag.getUUID("npcId");
        }
        if (tag.contains("buildBoxPos")) {
            task.buildBoxPos = Objects.requireNonNull(
                    NbtUtils.readBlockPos(Objects.requireNonNull(tag.getCompound("buildBoxPos")))
            );
        }
        if (tag.contains("type")) {
            task.type = TaskType.valueOf(Objects.requireNonNull(tag.getString("type")));
        }
        if (tag.contains("status")) {
            task.status = TaskStatus.valueOf(Objects.requireNonNull(tag.getString("status")));
        }
        task.currentBlockIndex = tag.getInt("currentBlockIndex");
        task.createTime = tag.getLong("createTime");
        task.completeTime = tag.getLong("completeTime");

        // 加载目标方块列表
        task.targetBlocks = new ArrayList<>();
        if (tag.contains("targetBlocks", Tag.TAG_LIST)) {
            ListTag blocksList = tag.getList("targetBlocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocksList.size(); i++) {
                task.targetBlocks.add(
                        Objects.requireNonNull(
                                NbtUtils.readBlockPos(Objects.requireNonNull(blocksList.getCompound(i)))
                        )
                );
            }
        }

        if (tag.contains("targetBlockId")) {
            task.targetBlockId = tag.getString("targetBlockId");
        }
        
        // 加载替换映射
        task.replacementMap = new HashMap<>();
        if (tag.contains("replacementMap", Tag.TAG_COMPOUND)) {
            CompoundTag mapTag = tag.getCompound("replacementMap");
            for (String key : mapTag.getAllKeys()) {
                task.replacementMap.put(
                        Objects.requireNonNull(key),
                        Objects.requireNonNull(mapTag.getString(key))
                );
            }
        }

        return task;
    }

    // Getters and Setters
    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public void setCurrentBlockIndex(int index) {
        this.currentBlockIndex = index;
        // 触发变更回调
        if (onChangedCallback != null) {
            onChangedCallback.run();
        }
    }

    public UUID getNpcId() {
        return npcId;
    }

    public void setNpcId(UUID npcId) {
        this.npcId = npcId;
    }

    public BlockPos getBuildBoxPos() {
        return buildBoxPos;
    }

    public void setBuildBoxPos(BlockPos buildBoxPos) {
        this.buildBoxPos = buildBoxPos;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<BlockPos> getTargetBlocks() {
        return targetBlocks;
    }

    public void setTargetBlocks(List<BlockPos> targetBlocks) {
        this.targetBlocks = targetBlocks;
    }

    public String getTargetBlockId() {
        return targetBlockId;
    }

    public void setTargetBlockId(String targetBlockId) {
        this.targetBlockId = targetBlockId;
    }
    
    public Map<String, String> getReplacementMap() {
        return replacementMap;
    }
    
    public void setReplacementMap(Map<String, String> replacementMap) {
        this.replacementMap = replacementMap != null ? replacementMap : new HashMap<>();
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getCompleteTime() {
        return completeTime;
    }
}
