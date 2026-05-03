package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public class BuildBoxData {
    private static final Map<BlockPos, UUID> hiredBuilderUuids = new HashMap<>();
    private static final Map<BlockPos, CustomEntity> hiredBuilders = new HashMap<>();
    private static final Map<UUID, String> npcNames = new HashMap<>(); // 存储UUID到NPC名称的映射
    private static final Map<BlockPos, Integer> buildProgress = new HashMap<>();

    // 规划师雇佣数据
    private static final Map<BlockPos, UUID> hiredPlannerUuids = new HashMap<>();
    private static final Map<BlockPos, CustomEntity> hiredPlanners = new HashMap<>();

    // 建筑盒更新频率配置（单位：tick，默认20tick = 1秒）
    private static final Map<BlockPos, Integer> updateFrequencies = new HashMap<>();
    private static final int DEFAULT_UPDATE_FREQUENCY = 20; // 默认1秒更新一次

    public static void setHiredBuilder(BlockPos buildBoxPos, CustomEntity npc) {
        System.out.println("[BuildBoxData] 设置雇佣建筑师 - buildBoxPos: " + buildBoxPos + ", NPC: " + (npc != null ? npc.getFullName() + " (UUID: " + npc.getUUID() + ")" : "null"));
        hiredBuilders.put(buildBoxPos, npc);
        if (npc != null) {
            UUID npcUuid = npc.getUUID();
            hiredBuilderUuids.put(buildBoxPos, npcUuid);
            // 存储NPC名称到映射中，即使NPC不在渲染距离内，也能获取到名称
            npcNames.put(npcUuid, npc.getFullName());
            buildProgress.put(buildBoxPos, 0);
            saveHiredBuilders();
        }
        System.out.println("[BuildBoxData] 设置后 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
    }
    
    /**
     * 通过NPC实体ID设置雇佣建筑师
     * 在专用服务器环境下使用，此时客户端没有直接的NPC实体引用
     */
    public static void setHiredBuilder(BlockPos buildBoxPos, int entityId) {
        // 在客户端，我们通过实体ID查找NPC实体
        var minecraft = Minecraft.getInstance();
        CustomEntity npc = null;
        
        if (minecraft.level != null) {
            var entity = minecraft.level.getEntity(entityId);
            if (entity instanceof CustomEntity) {
                npc = (CustomEntity) entity;
            }
        }
        
        // 保存雇佣数据
        if (npc != null) {
            hiredBuilders.put(buildBoxPos, npc);
            hiredBuilderUuids.put(buildBoxPos, npc.getUUID());
        } else {
            // 如果找不到实体（可能是在专用服务器环境下），只标记为已雇佣
            hiredBuilders.put(buildBoxPos, null);
            hiredBuilderUuids.put(buildBoxPos, null);
        }
        
        buildProgress.put(buildBoxPos, 0);
        
        // 保存数据到文件
        saveHiredBuilders();
    }
    
    /**
     * 通过UUID设置雇佣建筑师
     * 直接保存UUID，不依赖实体引用
     */
    public static void setHiredBuilder(BlockPos buildBoxPos, UUID npcUuid) {
        // 直接保存UUID，不依赖实体引用
        hiredBuilderUuids.put(buildBoxPos, npcUuid);
        buildProgress.put(buildBoxPos, 0);
        
        // 尝试找到对应的NPC实体
        var minecraft = Minecraft.getInstance();
        CustomEntity npc = null;
        
        if (minecraft.level != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof CustomEntity && entity.getUUID().equals(npcUuid)) {
                    npc = (CustomEntity) entity;
                    // 存储NPC名称到映射中
                    npcNames.put(npcUuid, npc.getFullName());
                    System.out.println("[BuildBoxData] 通过UUID设置雇佣建筑师，找到NPC实体，名称: " + npc.getFullName());
                    break;
                }
            }
        }
        
        hiredBuilders.put(buildBoxPos, npc);
        
        // 保存数据到文件
        saveHiredBuilders();
        
        System.out.println("[BuildBoxData] 通过UUID设置雇佣建筑师后 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
    }

    public static void clearHiredBuilder(BlockPos buildBoxPos) {
        System.out.println("[BuildBoxData] 清除雇佣建筑师 - buildBoxPos: " + buildBoxPos);
        System.out.println("[BuildBoxData] 清除前 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
        
        // 获取NPC UUID，用于从npcNames映射中移除
        UUID npcUuid = hiredBuilderUuids.get(buildBoxPos);
        
        hiredBuilders.remove(buildBoxPos);
        hiredBuilderUuids.remove(buildBoxPos);
        buildProgress.remove(buildBoxPos);
        
        // 从npcNames映射中移除对应的NPC名称
        if (npcUuid != null) {
            npcNames.remove(npcUuid);
            System.out.println("[BuildBoxData] 从npcNames映射中移除NPC名称 - UUID: " + npcUuid);
        }
        
        saveHiredBuilders();
        System.out.println("[BuildBoxData] 清除后 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
    }

    public static boolean hasHiredBuilder(BlockPos buildBoxPos) {
        // 检查hiredBuilders和hiredBuilderUuids两个映射，确保雇佣状态正确
        // 如果hiredBuilderUuids中有记录，说明该建筑盒确实雇佣了NPC
        return hiredBuilders.containsKey(buildBoxPos) || hiredBuilderUuids.containsKey(buildBoxPos);
    }

    /**
     * 检查是否已雇佣规划师
     */
    public static boolean hasHiredPlanner(BlockPos buildBoxPos) {
        return hiredPlanners.containsKey(buildBoxPos) || hiredPlannerUuids.containsKey(buildBoxPos);
    }

    /**
     * 设置雇佣规划师
     */
    public static void setHiredPlanner(BlockPos buildBoxPos, CustomEntity npc) {
        System.out.println("[BuildBoxData] 设置雇佣规划师 - buildBoxPos: " + buildBoxPos + ", NPC: " + (npc != null ? npc.getFullName() + " (UUID: " + npc.getUUID() + ")" : "null"));
        hiredPlanners.put(buildBoxPos, npc);
        if (npc != null) {
            UUID npcUuid = npc.getUUID();
            hiredPlannerUuids.put(buildBoxPos, npcUuid);
            npcNames.put(npcUuid, npc.getFullName());
            saveHiredPlanners();
        }
        System.out.println("[BuildBoxData] 设置规划师后 - hiredPlanners: " + hiredPlanners + ", hiredPlannerUuids: " + hiredPlannerUuids);
    }

    /**
     * 通过UUID设置雇佣规划师
     */
    public static void setHiredPlanner(BlockPos buildBoxPos, UUID npcUuid) {
        hiredPlannerUuids.put(buildBoxPos, npcUuid);

        var minecraft = Minecraft.getInstance();
        CustomEntity npc = null;

        if (minecraft.level != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof CustomEntity && entity.getUUID().equals(npcUuid)) {
                    npc = (CustomEntity) entity;
                    npcNames.put(npcUuid, npc.getFullName());
                    System.out.println("[BuildBoxData] 通过UUID设置雇佣规划师，找到NPC实体，名称: " + npc.getFullName());
                    break;
                }
            }
        }

        hiredPlanners.put(buildBoxPos, npc);
        saveHiredPlanners();
    }

    /**
     * 清除雇佣规划师
     */
    public static void clearHiredPlanner(BlockPos buildBoxPos) {
        System.out.println("[BuildBoxData] 清除雇佣规划师 - buildBoxPos: " + buildBoxPos);
        UUID npcUuid = hiredPlannerUuids.get(buildBoxPos);

        hiredPlanners.remove(buildBoxPos);
        hiredPlannerUuids.remove(buildBoxPos);

        if (npcUuid != null) {
            npcNames.remove(npcUuid);
        }

        saveHiredPlanners();
    }

    /**
     * 获取雇佣的规划师
     */
    public static CustomEntity getHiredPlanner(BlockPos buildBoxPos) {
        CustomEntity npc = hiredPlanners.get(buildBoxPos);

        if (npc == null && hiredPlannerUuids.containsKey(buildBoxPos)) {
            UUID npcUuid = hiredPlannerUuids.get(buildBoxPos);
            if (npcUuid != null) {
                var minecraft = Minecraft.getInstance();
                if (minecraft.getSingleplayerServer() != null) {
                    npc = com.xiaoliang.simukraft.world.BuildBoxHiredData.findNPCByUuid(minecraft.getSingleplayerServer(), npcUuid);
                    if (npc != null) {
                        hiredPlanners.put(buildBoxPos, npc);
                        npcNames.put(npcUuid, npc.getFullName());
                    }
                }
            }
        }

        return npc;
    }

    private static void saveHiredPlanners() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            com.xiaoliang.simukraft.world.BuildBoxHiredData.saveHiredPlanners(
                    minecraft.getSingleplayerServer(),
                    hiredPlannerUuids
            );
        }
    }

    public static CustomEntity getHiredBuilder(BlockPos buildBoxPos) {
        // 首先尝试从hiredBuilders获取NPC实体
        CustomEntity npc = hiredBuilders.get(buildBoxPos);
        
        // 如果找不到实体，但hiredBuilderUuids中有记录，尝试重新查找
        if (npc == null && hiredBuilderUuids.containsKey(buildBoxPos)) {
            UUID npcUuid = hiredBuilderUuids.get(buildBoxPos);
            if (npcUuid != null) {
                var minecraft = Minecraft.getInstance();
                if (minecraft.getSingleplayerServer() != null) {
                    npc = com.xiaoliang.simukraft.world.BuildBoxHiredData.findNPCByUuid(minecraft.getSingleplayerServer(), npcUuid);
                    if (npc != null) {
                        hiredBuilders.put(buildBoxPos, npc);
                        // 保存NPC名称
                        npcNames.put(npcUuid, npc.getFullName());
                        System.out.println("[BuildBoxData] 重新找到NPC实体 - Pos: " + buildBoxPos + ", NPC: " + npc.getFullName());
                    }
                }
            }
        }
        
        return npc;
    }

    public static Map<BlockPos, CustomEntity> getAllHiredBuilders() {
        return new HashMap<>(hiredBuilders);
    }

    public static int getBuildProgress(BlockPos buildBoxPos) {
        return buildProgress.getOrDefault(buildBoxPos, 0);
    }

    public static void setBuildProgress(BlockPos buildBoxPos, int progress) {
        buildProgress.put(buildBoxPos, progress);
    }

    public static void incrementBuildProgress(BlockPos buildBoxPos) {
        int current = buildProgress.getOrDefault(buildBoxPos, 0);
        buildProgress.put(buildBoxPos, current + 1);
    }

    public static void loadHiredBuilders(MinecraftServer server) {
        hiredBuilders.clear();
        hiredBuilderUuids.clear();
        hiredPlanners.clear();
        hiredPlannerUuids.clear();
        npcNames.clear();

        // 加载建筑师数据
        var builderData = com.xiaoliang.simukraft.world.BuildBoxHiredData.loadHiredBuilders(server);

        for (Map.Entry<BlockPos, UUID> entry : builderData.entrySet()) {
            BlockPos pos = entry.getKey();
            UUID npcUuid = entry.getValue();

            if (npcUuid != null) {
                // 无论是否找到对应的NPC实体，都将UUID添加到hiredBuilderUuids映射中
                hiredBuilderUuids.put(pos, npcUuid);

                // 尝试找到对应的NPC实体
                CustomEntity npc = com.xiaoliang.simukraft.world.BuildBoxHiredData.findNPCByUuid(server, npcUuid);
                if (npc != null) {
                    hiredBuilders.put(pos, npc);
                    // 保存NPC名称，即使NPC不在渲染距离内也能显示名称
                    npcNames.put(npcUuid, npc.getFullName());
                } else {
                    // 如果找不到NPC实体，但UUID存在，说明NPC可能在其他维度或未加载
                    // 这种情况下，我们仍然应该保持雇佣状态，但需要从服务器获取NPC名称
                    System.out.println("[BuildBoxData] 警告：无法找到NPC实体，但UUID存在 - Pos: " + pos + ", UUID: " + npcUuid);
                }
            } else {
                System.out.println("[BuildBoxData] 警告：加载到空的NPC UUID - Pos: " + pos);
            }
        }

        // 加载规划师数据
        var plannerData = com.xiaoliang.simukraft.world.BuildBoxHiredData.loadHiredPlanners(server);

        for (Map.Entry<BlockPos, UUID> entry : plannerData.entrySet()) {
            BlockPos pos = entry.getKey();
            UUID npcUuid = entry.getValue();

            if (npcUuid != null) {
                hiredPlannerUuids.put(pos, npcUuid);

                // 尝试找到对应的NPC实体
                CustomEntity npc = com.xiaoliang.simukraft.world.BuildBoxHiredData.findNPCByUuid(server, npcUuid);
                if (npc != null) {
                    hiredPlanners.put(pos, npc);
                    npcNames.put(npcUuid, npc.getFullName());
                    System.out.println("[BuildBoxData] 加载规划师 - Pos: " + pos + ", NPC: " + npc.getFullName());
                } else {
                    System.out.println("[BuildBoxData] 警告：无法找到规划师NPC实体，但UUID存在 - Pos: " + pos + ", UUID: " + npcUuid);
                }
            } else {
                System.out.println("[BuildBoxData] 警告：加载到空的规划师NPC UUID - Pos: " + pos);
            }
        }

        System.out.println("[BuildBoxData] 加载完成 - 建筑师: " + hiredBuilderUuids.size() + ", 规划师: " + hiredPlannerUuids.size());
    }

    private static void saveHiredBuilders() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            com.xiaoliang.simukraft.world.BuildBoxHiredData.saveHiredBuilders(
                    minecraft.getSingleplayerServer(), 
                    hiredBuilderUuids
            );
        }
        // 在专用服务器环境下，这个方法不会被调用，服务器会通过其他方式保存数据
    }

    public static void syncLoadedData() {
        // 问题：这个方法会导致已解雇的NPC在重新加入存档后又被重新雇佣
        // 原因：当玩家重新加入存档时，loadHiredBuilders方法会从buildbox_hired_builders.json文件中加载雇佣数据
        // 解决方案：移除这个方法，不再自动将UUID同步到实体，而是让服务器端通过网络包来管理雇佣状态
    }

    public static void fireNPC(CustomEntity npc) {
        // 获取NPC的UUID
        UUID npcUuid = npc.getUUID();
        
        // 创建一个集合来存储需要清除的建筑盒位置
        Set<BlockPos> buildBoxPositionsToClear = new HashSet<>();
        
        // 找出所有关联的建筑盒位置
        for (var entry : getAllHiredBuilders().entrySet()) {
            if (entry.getValue().equals(npc)) {
                buildBoxPositionsToClear.add(entry.getKey());
            }
        }
        
        // 同时检查hiredBuilderUuids映射
        for (var entry : new HashMap<>(hiredBuilderUuids).entrySet()) {
            if (entry.getValue().equals(npcUuid)) {
                buildBoxPositionsToClear.add(entry.getKey());
            }
        }
        
        // 清除所有关联的建筑盒数据
        for (BlockPos buildBoxPos : buildBoxPositionsToClear) {
            clearHiredBuilder(buildBoxPos);
            buildProgress.remove(buildBoxPos);
        }
    
        // 完全重置NPC状态，包括建造相关字段
        npc.setWorkStatus(WorkStatus.IDLE);
        npc.setJob("unemployed");
        
        // 清除建造任务并移除持久化存储
        if (npc.getConstructionTask() != null) {
            npc.getConstructionTask().markCompleted();  // 如果有任务，标记为完成
        }
        npc.setConstructionTask(null);  // 清除建造任务
        npc.setConstructionProgress(0);
        
        // 移除持久化存储中的建造任务记录
        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.xiaoliang.simukraft.world.ConstructionTaskData.removeTask(
                serverLevel.getServer(), npc.getUUID()
            );
        }
        
        // 保存数据到文件
        saveHiredBuilders();
    }
    
    // 新增标记建筑盒被破坏的方法
    public static void markBuildBoxDestroyed(BlockPos buildBoxPos) {
        CustomEntity npc = getHiredBuilder(buildBoxPos);
        if (npc != null) {
            fireNPC(npc);
        }
        clearHiredBuilder(buildBoxPos);
        buildProgress.remove(buildBoxPos);
    }
    
    // 新增获取所有建筑盒位置的方法
    public static Set<BlockPos> getAllHiredBuilderPositions() {
        return new HashSet<>(hiredBuilders.keySet());
    }

    // 获取所有雇佣的建筑师UUID
    public static Map<BlockPos, UUID> getAllHiredBuilderUuids() {
        return new HashMap<>(hiredBuilderUuids);
    }

    // 获取所有雇佣的规划师UUID
    public static Map<BlockPos, UUID> getAllHiredPlannerUuids() {
        return new HashMap<>(hiredPlannerUuids);
    }
    
    // 新增移除建造进度的方法
    public static void removeBuildBoxProgress(BlockPos buildBoxPos) {
        buildProgress.remove(buildBoxPos);
    }
    
    /**
     * 通过UUID清除所有关联的雇佣记录
     */
    public static void clearHiredBuilderByUuid(UUID npcUuid) {
        System.out.println("[BuildBoxData] 通过UUID清除雇佣记录 - UUID: " + npcUuid);
        System.out.println("[BuildBoxData] 清除前 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
        
        boolean modified = false;
        
        // 遍历hiredBuilderUuids映射，清除所有匹配的UUID记录
        for (Iterator<Map.Entry<BlockPos, UUID>> it = hiredBuilderUuids.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BlockPos, UUID> entry = it.next();
            if (entry.getValue().equals(npcUuid)) {
                BlockPos buildBoxPos = entry.getKey();
                System.out.println("[BuildBoxData] 找到匹配的雇佣记录 - buildBoxPos: " + buildBoxPos + ", UUID: " + entry.getValue());
                it.remove();
                hiredBuilders.remove(buildBoxPos);
                buildProgress.remove(buildBoxPos);
                modified = true;
            }
        }
        
        // 从npcNames映射中移除对应的NPC名称
        npcNames.remove(npcUuid);
        System.out.println("[BuildBoxData] 从npcNames映射中移除NPC名称 - UUID: " + npcUuid);
        
        // 只有在数据被修改时才保存
        if (modified) {
            System.out.println("[BuildBoxData] 数据已修改，保存到文件");
            saveHiredBuilders();
        } else {
            System.out.println("[BuildBoxData] 未找到匹配的雇佣记录，无需修改");
        }

        System.out.println("[BuildBoxData] 清除后 - hiredBuilders: " + hiredBuilders + ", hiredBuilderUuids: " + hiredBuilderUuids + ", npcNames: " + npcNames);
    }

    // ==================== 更新频率配置方法 ====================

    /**
     * 获取建筑盒的更新频率（单位：tick）
     * @param buildBoxPos 建筑盒位置
     * @return 更新频率，默认为20tick（1秒）
     */
    public static int getUpdateFrequency(BlockPos buildBoxPos) {
        return updateFrequencies.getOrDefault(buildBoxPos, DEFAULT_UPDATE_FREQUENCY);
    }

    /**
     * 设置建筑盒的更新频率
     * @param buildBoxPos 建筑盒位置
     * @param frequency 更新频率（单位：tick），最小值为1
     */
    public static void setUpdateFrequency(BlockPos buildBoxPos, int frequency) {
        if (frequency < 1) {
            frequency = 1; // 最小1tick
        }
        updateFrequencies.put(buildBoxPos, frequency);
        System.out.println("[BuildBoxData] 设置建筑盒更新频率 - Pos: " + buildBoxPos + ", Frequency: " + frequency + " ticks");
    }

    /**
     * 重置建筑盒的更新频率为默认值
     * @param buildBoxPos 建筑盒位置
     */
    public static void resetUpdateFrequency(BlockPos buildBoxPos) {
        updateFrequencies.remove(buildBoxPos);
        System.out.println("[BuildBoxData] 重置建筑盒更新频率为默认值 - Pos: " + buildBoxPos);
    }

    /**
     * 获取默认更新频率
     * @return 默认更新频率（20tick = 1秒）
     */
    public static int getDefaultUpdateFrequency() {
        return DEFAULT_UPDATE_FREQUENCY;
    }

    /**
     * 清除建筑盒的所有数据（包括更新频率）
     * @param buildBoxPos 建筑盒位置
     */
    public static void clearAllBuildBoxData(BlockPos buildBoxPos) {
        clearHiredBuilder(buildBoxPos);
        clearHiredPlanner(buildBoxPos);
        updateFrequencies.remove(buildBoxPos);
        buildProgress.remove(buildBoxPos);
        System.out.println("[BuildBoxData] 清除建筑盒所有数据 - Pos: " + buildBoxPos);
    }
}