package com.xiaoliang.simukraft.utils;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * NPC原材料收集任务类
 * 用于存储NPC原材料收集任务的信息
 */
public class NPCMaterialCollectionTask {

    public enum TaskStatus {
        PENDING("等待中"),
        MOVING_TO_INPUT("前往输入建筑"),
        COLLECTING("收集中"),
        MOVING_TO_TARGET("返回目标建筑"),
        COMPLETED("已完成"),
        FAILED("失败");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final UUID npcId;
    private final BlockPos targetPos;
    private final BlockPos inputPos;
    private final String itemId;
    private final int quantity;
    private TaskStatus status;
    private final long createTime;

    public NPCMaterialCollectionTask(UUID npcId, BlockPos targetPos, BlockPos inputPos, String itemId, int quantity) {
        this.npcId = npcId;
        this.targetPos = targetPos;
        this.inputPos = inputPos;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = TaskStatus.PENDING;
        this.createTime = System.currentTimeMillis();
    }

    public UUID getNpcId() {
        return npcId;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public BlockPos getInputPos() {
        return inputPos;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public long getCreateTime() {
        return createTime;
    }

    @Override
    public String toString() {
        return "NPCMaterialCollectionTask{" +
                "npcId=" + npcId +
                ", targetPos=" + targetPos +
                ", inputPos=" + inputPos +
                ", itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
