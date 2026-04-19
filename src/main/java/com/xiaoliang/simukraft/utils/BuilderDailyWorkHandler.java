package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.building.ConstructionTask;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.world.BuildBoxHiredData;
import com.xiaoliang.simukraft.world.ConstructionTaskData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 建筑师每日工作处理器
 * 确保第二天建筑师能正常工作，解决需要重新雇佣的问题
 * 修复局域网开放模式下NPC休息后建造任务丢失的问题
 */
public class BuilderDailyWorkHandler {

    /**
     * 启动建筑师每日工作
     * 在早上6:00触发，确保所有被雇佣的建筑师恢复工作状态
     */
    public static void startDailyWork(ServerLevel level) {
        if (level == null) return;

        // 检查当前时间是否为工作时间（早上6:00到晚上18:00）
        long dayTime = level.getDayTime() % 24000L;
        boolean isWorkTime = dayTime >= 0 && dayTime < 12000;
        if (!isWorkTime) {
            // 非工作时间，不执行传送等操作
            return;
        }

        MinecraftServer server = level.getServer();

        // 获取所有建筑盒的雇佣记录
        Map<BlockPos, UUID> hiredBuilders = BuildBoxHiredData.loadHiredBuilders(server);

        if (hiredBuilders.isEmpty()) {
            return;
        }

        // 获取v2系统的雇佣服务
        var employmentService = com.xiaoliang.simukraft.employment.service.EmploymentServices.get(server);
        String dimensionId = level.dimension().location().toString();

        // 遍历所有雇佣记录，确保建筑师处于工作状态
        for (Map.Entry<BlockPos, UUID> entry : hiredBuilders.entrySet()) {
            BlockPos buildBoxPos = entry.getKey();
            UUID npcUuid = entry.getValue();

            // 根据UUID查找NPC实体
            CustomEntity npc = BuildBoxHiredData.findNPCByUuid(server, npcUuid);

            if (npc != null) {
                // 确保v2系统中有对应的雇佣记录
                var existingAssignment = employmentService.findByNpc(npcUuid);
                if (existingAssignment.isEmpty()) {
                    // 检查该NPC是否已经被雇佣为其他职业（如规划师）
                    var allAssignments = employmentService.listByCity(null);
                    boolean hasOtherJob = allAssignments.stream()
                            .filter(a -> a.npcUuid().equals(npcUuid))
                            .filter(a -> a.jobType() != com.xiaoliang.simukraft.employment.domain.JobType.BUILDER)
                            .anyMatch(a -> a.status() == com.xiaoliang.simukraft.employment.domain.EmploymentStatus.ASSIGNED);
                    if (hasOtherJob) {
                        Simukraft.LOGGER.info("[BuilderDailyWorkHandler] NPC {} 已被雇佣为其他职业，跳过创建建筑师记录",
                            npcUuid.toString().substring(0, 8));
                    } else {
                        // v2系统中没有记录，创建雇佣记录
                        Simukraft.LOGGER.info("[BuilderDailyWorkHandler] v2系统中无雇佣记录，创建记录 - NPC: {}, 建筑盒: {}",
                            npcUuid.toString().substring(0, 8), buildBoxPos);
                        var hireResult = employmentService.hire(new com.xiaoliang.simukraft.employment.service.EmploymentCommands.HireCommand(
                            npcUuid,
                            dimensionId,
                            buildBoxPos,
                            com.xiaoliang.simukraft.employment.domain.WorkBlockType.BUILD_BOX,
                            com.xiaoliang.simukraft.employment.domain.JobType.BUILDER
                        ));
                        if (!hireResult.success()) {
                            Simukraft.LOGGER.warn("[BuilderDailyWorkHandler] 创建v2雇佣记录失败 - NPC: {}, 原因: {}",
                                npcUuid.toString().substring(0, 8), hireResult.message());
                        }
                    }
                }

                // 检查是否有持久化的建造任务需要恢复
                restoreConstructionTaskIfNeeded(server, npc, buildBoxPos);

                if (!"builder".equals(npc.getJob())) {
                    npc.setJob("builder");
                }

                boolean hasActiveTask = npc.getConstructionTask() != null
                        && !npc.getConstructionTask().isCompleted()
                        && npc.getConstructionTask().hasNextBlock();

                // 建筑师雇佣后保持工作中状态，等待玩家手动解雇
                // 有建造任务时传送到建筑盒，无任务时保持工作中状态但不强制传送
                if (npc.getWorkStatus() != WorkStatus.WORKING) {
                    npc.setWorkStatus(WorkStatus.WORKING);
                    npc.setWorking(true);
                }
                if (hasActiveTask) {
                    double distance = npc.position().distanceTo(new net.minecraft.world.phys.Vec3(
                            buildBoxPos.getX() + 0.5,
                            buildBoxPos.getY(),
                            buildBoxPos.getZ() + 0.5
                    ));
                    if (distance > 3.0) {
                        npc.scheduleHireArrivalTeleport(buildBoxPos);
                    }
                }
            }
        }
    }

    /**
     * 如果需要，从持久化存储中恢复建造任务
     * 解决局域网开放模式下NPC休息后建造任务丢失的问题
     */
    private static void restoreConstructionTaskIfNeeded(MinecraftServer server, CustomEntity npc, BlockPos buildBoxPos) {
        // 如果NPC已经有建造任务，不需要恢复
        if (npc.getConstructionTask() != null) {
            return;
        }

        // 从持久化存储中加载建造任务
        ConstructionTaskData.TaskInfo taskInfo = ConstructionTaskData.loadTask(server, npc.getUUID());
        if (taskInfo == null) {
            return;
        }

        try {
            // 验证NPC是否仍然被雇佣为建筑师
            // 检查BuildBoxHiredData中是否仍有该NPC的雇佣记录
            Map<BlockPos, UUID> hiredBuilders = BuildBoxHiredData.loadHiredBuilders(server);
            boolean isStillHired = hiredBuilders.values().stream()
                    .anyMatch(uuid -> uuid.equals(npc.getUUID()));
            
            if (!isStillHired) {
                Simukraft.LOGGER.warn("[BuilderDailyWorkHandler] NPC {} 已被解雇，移除建造任务记录",
                    npc.getUUID().toString().substring(0, 8));
                ConstructionTaskData.removeTask(server, npc.getUUID());
                return;
            }

            // 验证建筑盒是否还存在
            ServerLevel level = server.overworld();
            BlockState buildBoxState = level.getBlockState(Objects.requireNonNull(taskInfo.buildBoxPos));
            if (buildBoxState.isAir()) {
                Simukraft.LOGGER.warn("[BuilderDailyWorkHandler] 建筑盒已不存在，移除建造任务 - NPC: {}",
                    npc.getUUID().toString().substring(0, 8));
                ConstructionTaskData.removeTask(server, npc.getUUID());
                return;
            }

            // 检查建造是否已经完成（进度到达或超过总方块数）
            // 注意：这里需要重新加载建筑数据来获取实际的总方块数
            ConstructionTask tempTask = new ConstructionTask(
                Objects.requireNonNull(taskInfo.buildingName),
                Objects.requireNonNull(taskInfo.category),
                Objects.requireNonNull(taskInfo.startPos),
                Objects.requireNonNull(taskInfo.buildBoxPos),
                Objects.requireNonNull(taskInfo.facing),
                Objects.requireNonNull(taskInfo.displayName),
                taskInfo.cost,
                level
            );
            int totalBlocks = tempTask.getTotalBlocks();

            // 如果进度已经到达或超过总方块数，说明建造已经完成，不需要恢复
            if (taskInfo.currentBlockIndex >= totalBlocks) {
                Simukraft.LOGGER.info("[BuilderDailyWorkHandler] 建造任务已完成，跳过恢复 - NPC: {}, 建筑: {}",
                    npc.getUUID().toString().substring(0, 8),
                    taskInfo.displayName);
                // 移除已完成的建造任务记录
                ConstructionTaskData.removeTask(server, npc.getUUID());
                return;
            }

            // 重新创建建造任务（使用恢复专用构造函数）
            ConstructionTask task = tempTask;

            // 恢复建造进度
            task.setCurrentBlockIndex(taskInfo.currentBlockIndex);

            // 设置NPC的建造任务
            npc.setConstructionTask(task);

            Simukraft.LOGGER.info("[BuilderDailyWorkHandler] 成功恢复建造任务 - NPC: {}, 建筑: {}, 进度: {}/{}",
                npc.getUUID().toString().substring(0, 8),
                taskInfo.displayName,
                taskInfo.currentBlockIndex,
                task.getTotalBlocks());

        } catch (Exception e) {
            Simukraft.LOGGER.error("[BuilderDailyWorkHandler] 恢复建造任务失败 - NPC: {}",
                npc.getUUID().toString().substring(0, 8), e);
        }
    }

    /**
     * 保存建造任务到持久化存储
     * 在NPC开始建造或进度更新时调用
     */
    public static void saveConstructionTask(MinecraftServer server, CustomEntity npc) {
        if (server == null || npc == null) return;

        ConstructionTask task = npc.getConstructionTask();
        if (task == null) {
            // 如果没有任务，移除之前的记录
            ConstructionTaskData.removeTask(server, npc.getUUID());
            return;
        }

        try {
            ConstructionTaskData.TaskInfo taskInfo = new ConstructionTaskData.TaskInfo(
                Objects.requireNonNull(task.getInternalBuildingName()),
                Objects.requireNonNull(task.getCategory()),
                Objects.requireNonNull(task.getStartPos()),
                Objects.requireNonNull(task.getBuildBoxPos()),
                Objects.requireNonNull(task.getFacing().getName()),
                Objects.requireNonNull(task.getDisplayName()),
                task.getCost(),
                task.getCurrentBlockIndex()
            );

            ConstructionTaskData.saveTask(server, npc.getUUID(), taskInfo);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[BuilderDailyWorkHandler] 保存建造任务失败 - NPC: {}",
                npc.getUUID().toString().substring(0, 8), e);
        }
    }

    /**
     * 移除建造任务
     * 在建筑完成或取消时调用
     */
    public static void removeConstructionTask(MinecraftServer server, UUID npcUuid) {
        if (server == null || npcUuid == null) return;
        ConstructionTaskData.removeTask(server, npcUuid);
    }
}
