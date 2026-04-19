package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.planning.PlanningTask;
import com.xiaoliang.simukraft.planning.PlanningTaskManager;
import com.xiaoliang.simukraft.world.BuildBoxHiredData;
import com.xiaoliang.simukraft.world.PlanningTaskData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 规划师每日工作处理器
 * 确保第二天规划师能正常工作，解决需要重新雇佣的问题
 * 修复局域网开放模式下NPC休息后规划任务丢失的问题
 */
public class PlannerDailyWorkHandler {

    /**
     * 启动规划师每日工作
     * 在早上6:00触发，确保所有被雇佣的规划师恢复工作状态
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

        // 获取所有建筑盒的规划师雇佣记录
        Map<BlockPos, UUID> hiredPlanners = BuildBoxHiredData.loadHiredPlanners(server);

        if (hiredPlanners.isEmpty()) {
            return;
        }

        // 获取v2系统的雇佣服务
        var employmentService = com.xiaoliang.simukraft.employment.service.EmploymentServices.get(server);
        String dimensionId = level.dimension().location().toString();

        // 遍历所有雇佣记录，确保规划师处于工作状态
        for (Map.Entry<BlockPos, UUID> entry : hiredPlanners.entrySet()) {
            BlockPos buildBoxPos = entry.getKey();
            UUID npcUuid = entry.getValue();

            // 根据UUID查找NPC实体
            CustomEntity npc = BuildBoxHiredData.findNPCByUuid(server, npcUuid);

            if (npc != null) {
                // 确保v2系统中有对应的雇佣记录
                var existingAssignment = employmentService.findByNpc(npcUuid);
                if (existingAssignment.isEmpty()) {
                    // 检查该NPC是否已经被雇佣为其他职业（如建筑师）
                    var allAssignments = employmentService.listByCity(null);
                    boolean hasOtherJob = allAssignments.stream()
                            .filter(a -> a.npcUuid().equals(npcUuid))
                            .filter(a -> a.jobType() != com.xiaoliang.simukraft.employment.domain.JobType.PLANNER)
                            .anyMatch(a -> a.status() == com.xiaoliang.simukraft.employment.domain.EmploymentStatus.ASSIGNED);
                    if (hasOtherJob) {
                        Simukraft.LOGGER.info("[PlannerDailyWorkHandler] NPC {} 已被雇佣为其他职业，跳过创建规划师记录",
                            npcUuid.toString().substring(0, 8));
                    } else {
                        // v2系统中没有记录，创建雇佣记录
                        Simukraft.LOGGER.info("[PlannerDailyWorkHandler] v2系统中无雇佣记录，创建记录 - NPC: {}, 建筑盒: {}",
                            npcUuid.toString().substring(0, 8), buildBoxPos);
                        var hireResult = employmentService.hire(new com.xiaoliang.simukraft.employment.service.EmploymentCommands.HireCommand(
                            npcUuid,
                            dimensionId,
                            buildBoxPos,
                            com.xiaoliang.simukraft.employment.domain.WorkBlockType.BUILD_BOX,
                            com.xiaoliang.simukraft.employment.domain.JobType.PLANNER
                        ));
                        if (!hireResult.success()) {
                            Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 创建v2雇佣记录失败 - NPC: {}, 原因: {}",
                                npcUuid.toString().substring(0, 8), hireResult.message());
                        }
                    }
                }

                // 设置职业为规划师
                if (!"planner".equals(npc.getJob())) {
                    npc.setJob("planner");
                }

                // 规划师雇佣后保持工作中状态，等待玩家手动解雇
                if (npc.getWorkStatus() != WorkStatus.WORKING) {
                    npc.setWorkStatus(WorkStatus.WORKING);
                    npc.setWorking(true);
                }

                // 检查是否有持久化的规划任务需要恢复
                restorePlanningTaskIfNeeded(server, level, npc, buildBoxPos);
            }
        }
    }

    /**
     * 如果需要，从持久化存储中恢复规划任务
     * 解决局域网开放模式下NPC休息后规划任务丢失的问题
     */
    private static void restorePlanningTaskIfNeeded(MinecraftServer server, ServerLevel level, CustomEntity npc, BlockPos buildBoxPos) {
        // 检查PlanningTaskManager中是否已有该NPC的任务
        PlanningTaskManager taskManager = PlanningTaskManager.get(level);
        PlanningTask existingTask = taskManager.getActiveTaskByNpc(npc.getUUID());

        if (existingTask != null) {
            // 已有任务，检查是否需要更新进度
            Simukraft.LOGGER.debug("[PlannerDailyWorkHandler] NPC {} 已有活跃规划任务，状态: {}",
                npc.getUUID().toString().substring(0, 8),
                existingTask.getStatus());
            return;
        }

        // 从JSON持久化存储中加载规划任务
        PlanningTaskData.TaskInfo taskInfo = PlanningTaskData.loadTaskByNpc(server, npc.getUUID());
        if (taskInfo == null) {
            return;
        }

        try {
            // 验证NPC是否仍然被雇佣为规划师
            Map<BlockPos, UUID> hiredPlanners = BuildBoxHiredData.loadHiredPlanners(server);
            boolean isStillHired = hiredPlanners.values().stream()
                    .anyMatch(uuid -> uuid.equals(npc.getUUID()));

            if (!isStillHired) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] NPC {} 已被解雇，移除规划任务记录",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 验证建筑盒是否还存在
            BlockPos taskBuildBoxPos = taskInfo.buildBoxPos;
            if (taskBuildBoxPos == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 建筑盒位置为空，移除规划任务 - NPC: {}",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }
            BlockState buildBoxState = level.getBlockState(taskBuildBoxPos);
            if (buildBoxState.isAir()) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 建筑盒已不存在，移除规划任务 - NPC: {}",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 检查任务是否已经完成
            if ("COMPLETED".equals(taskInfo.status) || "CANCELLED".equals(taskInfo.status)) {
                Simukraft.LOGGER.info("[PlannerDailyWorkHandler] 规划任务已{}，跳过恢复 - NPC: {}",
                    taskInfo.status.equals("COMPLETED") ? "完成" : "取消",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 检查目标方块列表是否为空
            if (taskInfo.targetBlocks == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 目标方块列表为空，移除规划任务 - NPC: {}",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 检查进度是否已经完成
            if (taskInfo.currentBlockIndex >= taskInfo.targetBlocks.size()) {
                Simukraft.LOGGER.info("[PlannerDailyWorkHandler] 规划任务已完成，跳过恢复 - NPC: {}, 进度: {}/{}",
                    npc.getUUID().toString().substring(0, 8),
                    taskInfo.currentBlockIndex,
                    taskInfo.targetBlocks.size());
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 检查任务类型是否为空
            if (taskInfo.taskType == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 任务类型为空，移除规划任务 - NPC: {}",
                    npc.getUUID().toString().substring(0, 8));
                PlanningTaskData.removeTaskByNpc(server, npc.getUUID());
                return;
            }

            // 重新创建规划任务
            PlanningTask.TaskType taskType = PlanningTask.TaskType.valueOf(taskInfo.taskType);
            PlanningTask task = new PlanningTask(npc.getUUID(), taskBuildBoxPos, taskType, taskInfo.targetBlocks);

            // 恢复任务ID（保持原有ID）
            task.setTaskId(taskInfo.taskId);

            // 恢复任务状态
            if ("IN_PROGRESS".equals(taskInfo.status)) {
                task.setStatus(PlanningTask.TaskStatus.IN_PROGRESS);
            } else {
                task.setStatus(PlanningTask.TaskStatus.PENDING);
            }

            // 恢复进度
            task.setCurrentBlockIndex(taskInfo.currentBlockIndex);

            // 恢复目标方块ID（用于替换/填充任务）
            if (taskInfo.targetBlockId != null) {
                task.setTargetBlockId(taskInfo.targetBlockId);
            }

            // 恢复替换映射
            if (taskInfo.replacementMap != null && !taskInfo.replacementMap.isEmpty()) {
                task.setReplacementMap(taskInfo.replacementMap);
            }

            // 将任务添加到PlanningTaskManager
            taskManager.addTask(task);

            Simukraft.LOGGER.info("[PlannerDailyWorkHandler] 成功恢复规划任务 - NPC: {}, 类型: {}, 进度: {}/{}",
                npc.getUUID().toString().substring(0, 8),
                taskInfo.taskType,
                taskInfo.currentBlockIndex,
                taskInfo.targetBlocks.size());

            // 有规划任务时传送到建筑盒（类似建筑师）
            double distance = npc.position().distanceTo(new net.minecraft.world.phys.Vec3(
                    taskBuildBoxPos.getX() + 0.5,
                    taskBuildBoxPos.getY(),
                    taskBuildBoxPos.getZ() + 0.5
            ));
            if (distance > 3.0) {
                npc.scheduleHireArrivalTeleport(taskBuildBoxPos);
                Simukraft.LOGGER.info("[PlannerDailyWorkHandler] 规划师距离建筑盒较远({}格)，安排传送 - NPC: {}, 建筑盒: {}",
                    String.format("%.1f", distance),
                    npc.getUUID().toString().substring(0, 8),
                    taskBuildBoxPos);
            }

        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlannerDailyWorkHandler] 恢复规划任务失败 - NPC: {}",
                npc.getUUID().toString().substring(0, 8), e);
        }
    }

    /**
     * 保存规划任务到持久化存储
     * 在规划任务创建或进度更新时调用
     */
    public static void savePlanningTask(MinecraftServer server, PlanningTask task) {
        if (server == null || task == null) return;

        try {
            // 检查必要字段是否为空
            UUID taskId = task.getTaskId();
            UUID npcId = task.getNpcId();
            BlockPos buildBoxPos = task.getBuildBoxPos();
            PlanningTask.TaskType type = task.getType();
            PlanningTask.TaskStatus status = task.getStatus();
            List<BlockPos> targetBlocks = task.getTargetBlocks();

            if (taskId == null || npcId == null || buildBoxPos == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 任务必要字段为空，跳过保存 - Task: {}",
                    taskId != null ? taskId.toString().substring(0, 8) : "null");
                return;
            }
            if (type == null || status == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 任务类型或状态为空，跳过保存 - Task: {}",
                    taskId.toString().substring(0, 8));
                return;
            }
            if (targetBlocks == null) {
                Simukraft.LOGGER.warn("[PlannerDailyWorkHandler] 目标方块列表为空，跳过保存 - Task: {}",
                    taskId.toString().substring(0, 8));
                return;
            }

            PlanningTaskData.TaskInfo taskInfo = new PlanningTaskData.TaskInfo(
                taskId,
                npcId,
                buildBoxPos,
                type.name(),
                status.name(),
                targetBlocks,
                task.getCompletedBlocks(),
                task.getTargetBlockId(),
                task.getReplacementMap(),
                task.getCreateTime()
            );

            PlanningTaskData.saveTask(server, taskInfo);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[PlannerDailyWorkHandler] 保存规划任务失败 - Task: {}",
                task.getTaskId() != null ? task.getTaskId().toString().substring(0, 8) : "null", e);
        }
    }

    /**
     * 移除规划任务从持久化存储
     * 在规划任务完成或取消时调用
     */
    public static void removePlanningTask(MinecraftServer server, UUID taskId) {
        if (server == null || taskId == null) return;
        PlanningTaskData.removeTask(server, taskId);
    }

    /**
     * 根据NPC UUID移除规划任务从持久化存储
     * 在规划师被解雇时调用
     */
    public static void removePlanningTaskByNpc(MinecraftServer server, UUID npcUuid) {
        if (server == null || npcUuid == null) return;
        PlanningTaskData.removeTaskByNpc(server, npcUuid);
    }
}
