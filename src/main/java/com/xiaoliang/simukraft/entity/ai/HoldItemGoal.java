package com.xiaoliang.simukraft.entity.ai;

import com.xiaoliang.simukraft.entity.CustomEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("null")
public class HoldItemGoal extends Goal {
    private final CustomEntity npc;

    public HoldItemGoal(CustomEntity npc) {
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        if (!npc.canStartAutonomousGoal()) return false;
        String job = npc.getJob();
        if (job == null || job.isBlank()) return false;
        
        ItemStack heldItem = resolveHeldItemFromConfig(job);
        return !heldItem.isEmpty();
    }

    @Override
    public void start() {
        // 根据职业设置不同的手持物品
        String job = npc.getJob();
        ItemStack itemToHold = resolveHeldItemFromConfig(job);
        
        // 如果配置中没有，使用默认手持物品
        if (itemToHold.isEmpty()) {
            itemToHold = switch (job) {
                case "builder" -> new ItemStack(Items.COBBLESTONE);
                case "farmer" -> new ItemStack(Items.STONE_HOE);
                default -> ItemStack.EMPTY;
            };
        }
        
        // 设置主手物品
        npc.setItemInHand(npc.getUsedItemHand(), itemToHold);
    }
    
    /**
     * 从JSON配置解析手持物品
     */
    private ItemStack resolveHeldItemFromConfig(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return ItemStack.EMPTY;
        }
        
        // 从商业建筑配置查找
        var commercialConfigs = com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfigsByJobType(jobType);
        if (!commercialConfigs.isEmpty()) {
            String heldItemId = commercialConfigs.get(0).getHeldItem();
            if (heldItemId != null && !heldItemId.isBlank()) {
                try {
                    ResourceLocation itemId = parseItemId(heldItemId);
                    var item = itemId == null ? null
                            : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }

        // 从工业建筑配置查找
        var industrialConfigs = com.xiaoliang.simukraft.building.IndustrialBuildingManager.getConfigsByJobType(jobType);
        if (!industrialConfigs.isEmpty()) {
            String heldItemId = industrialConfigs.get(0).getHeldItem();
            if (heldItemId != null && !heldItemId.isBlank()) {
                try {
                    ResourceLocation itemId = parseItemId(heldItemId);
                    var item = itemId == null ? null
                            : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private static ResourceLocation parseItemId(String heldItemId) {
        return heldItemId.contains(":")
                ? ResourceLocation.tryParse(heldItemId)
                : ResourceLocation.tryParse("minecraft:" + heldItemId);
    }

    @Override
    public void stop() {
        // 停止时清空手中的物品
        npc.setItemInHand(npc.getUsedItemHand(), ItemStack.EMPTY);
    }
}
