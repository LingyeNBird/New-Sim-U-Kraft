package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.building.CommercialBuildingConfig;
import com.xiaoliang.simukraft.building.CommercialBuildingManager;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.init.ModBlocks;
import com.xiaoliang.simukraft.utils.CommercialWorkHandler;
import com.xiaoliang.simukraft.world.CommercialHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 商业建筑传送处理器
 * 统一处理所有商业建筑NPC的传送完成事件
 * 完全配置化，无硬编码
 */
@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommercialTeleportHandler {

    @SubscribeEvent
    public static void onNPCTeleportComplete(NPCTeleportCompleteEvent event) {
        if (event.getLevel().isClientSide()) return;

        CustomEntity npc = event.getNPC();
        BlockPos targetPos = event.getTargetPos();

        // 检查是否是商业控制箱的传送
        if (targetPos == null) return;
        if (event.getLevel().getBlockState(targetPos).getBlock() != ModBlocks.COMMERCIAL_CONTROL_BOX.get()) return;

        // 获取NPC职业
        String npcJob = npc.getJob();
        if (npcJob == null || npcJob.isEmpty()) return;

        // 通过 CommercialHiredData 读取该位置的 jobType
        String jobType = CommercialHiredData.getJobType(event.getLevel().getServer(), targetPos);
        if (jobType == null || jobType.isEmpty()) return;

        // 确认NPC职业与建筑职业匹配
        if (!npcJob.equals(jobType)) return;

        // 获取建筑文件名
        String buildingFileName = CommercialWorkHandler.getBuildingFileName((ServerLevel) event.getLevel(), targetPos);
        if (buildingFileName == null) return;

        // 获取配置
        CommercialBuildingConfig config = CommercialBuildingManager.getConfig(buildingFileName);
        if (config == null) return;

        // 再次确认配置中的职业类型匹配
        if (!npcJob.equals(config.getJobType())) return;

        // 使用统一的商业工作处理器处理传送后的逻辑
        System.out.println("[CommercialTeleportHandler] 处理NPC传送：" + npc.getFullName() + "，职业：" + npcJob + "，建筑：" + buildingFileName);
        CommercialWorkHandler.onCommercialNpcTeleported(npc, (ServerLevel) event.getLevel(), targetPos, buildingFileName);

        // 设置手持物品（重进游戏后恢复）
        System.out.println("[CommercialTeleportHandler] 设置手持物品：" + config.getHeldItem());
        CommercialWorkHandler.setNpcHeldItem(npc, config);
    }
}
