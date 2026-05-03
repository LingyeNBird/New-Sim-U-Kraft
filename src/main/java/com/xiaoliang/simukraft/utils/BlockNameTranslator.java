package com.xiaoliang.simukraft.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;


public class BlockNameTranslator {


    private static final String KEY_UNKNOWN_BLOCK = "simukraft.name.unknown_block";

    private static final String KEY_UNKNOWN_ITEM  = "simukraft.name.unknown_item";

    private static final String KEY_EMPTY         = "simukraft.name.empty";


    public static String getBlockName(BlockState state) {
        if (state == null) {
            return Component.translatable(KEY_UNKNOWN_BLOCK).getString();
        }
        return getBlockName(state.getBlock());
    }


    public static String getBlockName(Block block) {
        if (block == null) {
            return Component.translatable(KEY_UNKNOWN_BLOCK).getString();
        }

        return block.getName().getString();
    }


    public static String getBlockName(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return Component.translatable(KEY_UNKNOWN_BLOCK).getString();
        }

        ResourceLocation location = ResourceLocation.tryParse(blockId);
        if (location == null) {
            return blockId;
        }


        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            String name = block.getName().getString();

            return name;
        }


        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item != null) {
            return new ItemStack(item).getHoverName().getString();
        }


        return blockId;
    }


    public static String getItemName(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Component.translatable(KEY_EMPTY).getString();
        }

        return itemStack.getHoverName().getString();
    }


    public static String getItemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Component.translatable(KEY_UNKNOWN_ITEM).getString();
        }

        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            return itemId;
        }


        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item != null) {
            return new ItemStack(item).getHoverName().getString();
        }


        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            return block.getName().getString();
        }

        return itemId;
    }

    /**
     * 获取物品/方块的可翻译 Component（客户端翻译用）
     */
    public static Component getItemComponent(String materialId) {
        if (materialId == null || materialId.isEmpty()) {
            return Component.translatable(KEY_UNKNOWN_ITEM);
        }

        ResourceLocation location = ResourceLocation.tryParse(materialId);
        if (location == null) {
            return Component.literal(materialId);
        }

        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item != null) {
            return new ItemStack(item).getHoverName();
        }

        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            return block.getName();
        }

        return Component.literal(materialId);
    }

    /**
     * 获取方块的可翻译 Component（客户端翻译用）
     */
    public static Component getBlockComponent(Block block) {
        if (block == null) {
            return Component.translatable(KEY_UNKNOWN_BLOCK);
        }
        return block.getName();
    }
}
