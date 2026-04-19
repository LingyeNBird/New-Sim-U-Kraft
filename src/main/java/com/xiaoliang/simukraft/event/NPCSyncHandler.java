package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public class NPCSyncHandler {
    private static boolean firstLoad = true;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof CustomEntity npc)) return;
        
        // 当NPC实体加载时，同步其工作状态
        MinecraftServer server = event.getLevel().getServer();

        if (server != null && npc.getNpcId() != -1) {
            String npcIdStr = "npc" + npc.getNpcId();
            String[] jobData = NPCDataManager.getJobData(server, npcIdStr);
            
            if (jobData != null && jobData.length >= 2) {
                String status = jobData[0];
                String job = jobData[1];

                // 重要：只有当NPC已经有名字初始化后，才进行状态恢复
                // 新生成的NPC还没有初始化名字，不应该恢复工作状态
                if (!npc.isNameInitialized()) {
                    Simukraft.LOGGER.debug("[NPCSyncHandler] Skip restoring brand-new NPC state, npcId={}", npc.getNpcId());
                    return;
                }

                npc.setJob(job);

                // 先通用恢复工作状态，再补充职业特有的手持物/附加行为。
                if ("working".equals(status)) {
                    npc.setWorkStatus(com.xiaoliang.simukraft.entity.WorkStatus.WORKING);

                    // 设置手持物品 - 从JSON配置读取
                    ItemStack heldItem = resolveHeldItemFromConfig(job);
                    if (!heldItem.isEmpty()) {
                        npc.setItemInHand(npc.getUsedItemHand(), heldItem);
                    }
                    // 后备：使用默认手持物品
                    else if ("builder".equals(job)) {
                        npc.setItemInHand(npc.getUsedItemHand(), new ItemStack(Items.COBBLESTONE));
                    } else if ("shepherd".equals(job)) {
                        npc.setItemInHand(npc.getUsedItemHand(), new ItemStack(Items.SHEARS));
                    } else if ("butcher".equals(job)) {
                        npc.setItemInHand(npc.getUsedItemHand(), new ItemStack(Items.GOLDEN_AXE));
                    } else if ("farmer".equals(job)) {
                        npc.setItemInHand(npc.getUsedItemHand(), new ItemStack(Items.STONE_HOE));
                    }
                } else {
                    npc.setWorkStatus(com.xiaoliang.simukraft.entity.WorkStatus.IDLE);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;
            
            // 在服务器启动后首次tick同步数据
            if (firstLoad && event.getServer() != null) {
                // 创建业务文件夹结构
                com.xiaoliang.simukraft.utils.FileUtils.createBusinessFolders(event.getServer());
                // 服务器端不需要加载客户端GUI数据
                firstLoad = false;
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[NPCSyncHandler] Failed during server tick bootstrap", e);
        } catch (Error e) {
            Simukraft.LOGGER.error("[NPCSyncHandler] Fatal error during server tick bootstrap", e);
        }
    }

    /**
     * 从JSON配置解析手持物品
     */
    private static ItemStack resolveHeldItemFromConfig(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return ItemStack.EMPTY;
        }
        
        // 从商业建筑配置查找
        var commercialConfigs = com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfigsByJobType(jobType);
        if (!commercialConfigs.isEmpty()) {
            String heldItemId = commercialConfigs.get(0).getHeldItem();
            if (heldItemId != null && !heldItemId.isBlank()) {
                try {
                    ResourceLocation itemId = parseHeldItemId(heldItemId);
                    var item = itemId == null ? null
                            : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                } catch (Exception e) {
                    Simukraft.LOGGER.warn("[NPCSyncHandler] 无法解析手持物品: {}", heldItemId);
                }
            }
        }

        // 从工业建筑配置查找
        var industrialConfigs = com.xiaoliang.simukraft.building.IndustrialBuildingManager.getConfigsByJobType(jobType);
        if (!industrialConfigs.isEmpty()) {
            String heldItemId = industrialConfigs.get(0).getHeldItem();
            if (heldItemId != null && !heldItemId.isBlank()) {
                try {
                    ResourceLocation itemId = parseHeldItemId(heldItemId);
                    var item = itemId == null ? null
                            : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                } catch (Exception e) {
                    Simukraft.LOGGER.warn("[NPCSyncHandler] 无法解析手持物品: {}", heldItemId);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private static ResourceLocation parseHeldItemId(String heldItemId) {
        return heldItemId.contains(":")
                ? ResourceLocation.tryParse(heldItemId)
                : ResourceLocation.tryParse("minecraft:" + heldItemId);
    }
}
