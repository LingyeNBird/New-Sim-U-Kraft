package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.world.BaseBuildingHiredData;
import com.xiaoliang.simukraft.world.LogisticsHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 仓库管理员每日工作处理器
 * 确保第二天仓库管理员能正常工作，解决需要重新雇佣的问题
 */
public class WarehouseManagerDailyWorkHandler {

    /**
     * 启动仓库管理员每日工作
     * 在早上6:00触发，确保所有被雇佣的仓库管理员恢复工作状态
     */
    public static void startDailyWork(ServerLevel level) {
        if (level == null) return;

        MinecraftServer server = level.getServer();

        // 获取所有仓库的雇佣记录
        Map<BlockPos, UUID> hiredManagers = LogisticsHiredData.getServerBoxHiredNpcs(server);

        if (hiredManagers.isEmpty()) {
            return;
        }

        System.out.println("[WarehouseManagerDailyWorkHandler] 恢复 " + hiredManagers.size() + " 个仓库管理员的工作状态");

        // 遍历所有雇佣记录，确保仓库管理员处于工作状态
        for (Map.Entry<BlockPos, UUID> entry : hiredManagers.entrySet()) {
            BlockPos warehousePos = entry.getKey();
            UUID npcUuid = entry.getValue();

            // 根据UUID查找NPC实体
            CustomEntity npc = BaseBuildingHiredData.findNPCByUuid(server, npcUuid);

            if (npc != null) {
                // 确保仓库管理员处于工作状态
                if (npc.getWorkStatus() != WorkStatus.WORKING) {
                    npc.setWorkStatus(WorkStatus.WORKING);
                    npc.setWorking(true);
                    System.out.println("[WarehouseManagerDailyWorkHandler] 恢复仓库管理员工作状态: " + npc.getFullName() + " at " + warehousePos);
                }

                // 确保职业正确设置
                if (!"warehouse_manager".equals(npc.getJob())) {
                    npc.setJob("warehouse_manager");
                    System.out.println("[WarehouseManagerDailyWorkHandler] 设置仓库管理员职业: " + npc.getFullName());
                }

                // 确保手持物品正确
                ItemStack heldItem = new ItemStack(Objects.requireNonNull(Items.BOOK));
                npc.setItemInHand(Objects.requireNonNull(npc.getUsedItemHand()), heldItem);
            } else {
                System.out.println("[WarehouseManagerDailyWorkHandler] 警告: 找不到仓库管理员NPC, UUID=" + npcUuid);
            }
        }
    }
}
