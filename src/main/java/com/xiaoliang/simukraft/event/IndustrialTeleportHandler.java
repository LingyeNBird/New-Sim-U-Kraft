package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.building.ControlBoxDataManager;
import com.xiaoliang.simukraft.building.IndustrialBuildingConfig;
import com.xiaoliang.simukraft.building.IndustrialBuildingManager;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.init.ModBlocks;
import com.xiaoliang.simukraft.job.jobs.industrialgeneric.IndustrialWorkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 工业建筑传送处理器
 * 统一处理所有工业建筑NPC的传送完成事件
 * 完全配置化，无硬编码
 */
@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IndustrialTeleportHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onNPCTeleportComplete(NPCTeleportCompleteEvent event) {
        if (event.getLevel().isClientSide()) return;

        CustomEntity npc = event.getNPC();
        BlockPos targetPos = event.getTargetPos();

        // 检查是否是工业控制箱的传送
        if (targetPos == null) return;
        if (event.getLevel().getBlockState(targetPos).getBlock() != ModBlocks.INDUSTRIAL_CONTROL_BOX.get()) return;

        // 获取NPC职业
        String npcJob = npc.getJob();
        if (npcJob == null || npcJob.isEmpty()) return;

        // 获取建筑文件名
        String buildingFileName = IndustrialWorkHandler.getBuildingFileName((ServerLevel) event.getLevel(), targetPos);
        if (buildingFileName == null) return;

        // 获取配置
        IndustrialBuildingConfig config = IndustrialBuildingManager.getConfig(buildingFileName);
        if (config == null) return;

        // 再次确认配置中的职业类型匹配
        if (!npcJob.equals(config.getJobType())) return;

        // 使用统一的工业工作处理器处理传送后的逻辑（生成生物、设置手持物品等）
        LOGGER.debug("[IndustrialTeleportHandler] 处理NPC传送: {}, 职业: {}, 建筑: {}",
                npc.getFullName(), npcJob, buildingFileName);
        IndustrialWorkHandler.onIndustrialNpcTeleported(npc, (ServerLevel) event.getLevel(), targetPos, buildingFileName);

        // 获取当前选择的配方ID
        String selectedRecipeId = ControlBoxDataManager.getSelectedRecipe(event.getLevel().getServer(), targetPos);

        // 设置手持物品（重进游戏后恢复，使用配方配置）
        String effectiveHeldItem = config.getEffectiveHeldItem(selectedRecipeId);
        LOGGER.debug("[IndustrialTeleportHandler] 设置手持物品: {}", effectiveHeldItem);
        IndustrialWorkHandler.setNpcHeldItem(npc, config, selectedRecipeId);
    }
}
