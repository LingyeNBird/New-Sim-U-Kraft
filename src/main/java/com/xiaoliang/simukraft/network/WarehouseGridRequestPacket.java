package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.ContainerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 请求仓库物品数据
 */
@SuppressWarnings("null")
public class WarehouseGridRequestPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    private final BlockPos warehousePos;

    public WarehouseGridRequestPacket(BlockPos warehousePos) {
        this.warehousePos = warehousePos;
    }

    public WarehouseGridRequestPacket(FriendlyByteBuf buf) {
        this.warehousePos = Objects.requireNonNull(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(warehousePos));
    }

    public void handle(Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            var data = com.xiaoliang.simukraft.world.LogisticsData.get(level);
            var warehouse = data.getWarehouseByBlockPos(warehousePos);

            List<ItemStack> items = new ArrayList<>();
            List<BlockPos> positions = new ArrayList<>();
            List<Integer> actualCounts = new ArrayList<>();

            if (warehouse != null) {
                List<BlockPos> containerPositions = warehouse.getContainerPositions();

                // 收集所有容器中的所有物品
                // 使用列表来存储物品原型和数量，使用 ItemHandlerHelper.canItemStacksStack 来判断是否可以堆叠
                List<ItemStack> mergedItemPrototypes = new ArrayList<>();
                List<Integer> mergedItemCounts = new ArrayList<>();

                for (BlockPos pos : containerPositions) {
                    // 跳过精妙储存模组的大箱子副箱子（避免重复计算）
                    if (isSophisticatedStorageSubChest(level, pos)) {
                        LOGGER.debug("[WarehouseGridRequest] 跳过精妙储存模组副箱子: {}", pos);
                        continue;
                    }
                    
                    // 使用 ContainerUtils 获取容器中的所有物品
                    List<ItemStack> containerItems = ContainerUtils.getAllItemsOnMainThread(level, pos);
                    LOGGER.debug("[WarehouseGridRequest] 容器 {} 返回 {} 个物品", pos, containerItems.size());

                    for (ItemStack item : containerItems) {
                        if (item.isEmpty()) continue;

                        String itemName = item.getItem().toString();
                        LOGGER.debug("[WarehouseGridRequest] 处理物品: {} x {}", itemName, item.getCount());

                        // 查找是否可以与现有物品堆叠
                        boolean found = false;
                        for (int i = 0; i < mergedItemPrototypes.size(); i++) {
                            ItemStack prototype = mergedItemPrototypes.get(i);
                            if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(prototype, item)) {
                                // 可以堆叠，增加数量
                                mergedItemCounts.set(i, mergedItemCounts.get(i) + item.getCount());
                                found = true;
                                LOGGER.debug("[WarehouseGridRequest] 物品与第 {} 个原型堆叠，新数量: {}", i, mergedItemCounts.get(i));
                                break;
                            }
                        }

                        if (!found) {
                            // 不能堆叠，添加为新物品
                            mergedItemPrototypes.add(item.copy());
                            mergedItemCounts.add(item.getCount());
                            LOGGER.debug("[WarehouseGridRequest] 添加新物品原型，当前共 {} 种", mergedItemPrototypes.size());
                        }
                    }
                }
                
                LOGGER.debug("[WarehouseGridRequest] 合并后共有 {} 种物品", mergedItemPrototypes.size());

                // 创建物品数据列表用于排序
                List<ItemData> itemDataList = new ArrayList<>();
                for (int i = 0; i < mergedItemPrototypes.size(); i++) {
                    itemDataList.add(new ItemData(mergedItemPrototypes.get(i), mergedItemCounts.get(i)));
                }
                
                // 按物品名称排序（保持稳定的排序，方便快速拿取）
                itemDataList.sort((a, b) -> {
                    String nameA = a.prototype.getItem().toString();
                    String nameB = b.prototype.getItem().toString();
                    return nameA.compareToIgnoreCase(nameB);
                });
                
                LOGGER.debug("[WarehouseGridRequest] 已按名称排序 {} 种物品", itemDataList.size());

                // 添加所有合并后的物品
                for (ItemData itemData : itemDataList) {
                    ItemStack prototype = itemData.prototype;
                    int totalCount = itemData.count;

                    // 创建显示用的物品（数量限制为64用于显示）
                    ItemStack displayItem = prototype.copy();
                    displayItem.setCount(Math.min(totalCount, 64));

                    items.add(displayItem);
                    actualCounts.add(totalCount);
                    positions.add(warehousePos); // 使用仓库位置作为标记
                }

                // 不再填充空槽位到54个，让客户端根据实际物品数量显示
            }

            // 发送物品数据、容器位置和实际数量到客户端
            NetworkManager.sendTo(new WarehouseGridResponsePacket(items, positions, actualCounts), player);
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getWarehousePos() {
        return warehousePos;
    }
    
    /**
     * 物品数据类，用于排序
     */
    private static class ItemData {
        final ItemStack prototype;
        final int count;
        
        ItemData(ItemStack prototype, int count) {
            this.prototype = prototype;
            this.count = count;
        }
    }
    
    /**
     * 检测指定位置是否是精妙储存模组的大箱子副箱子
     * 精妙储存模组的大箱子由两个方块组成，副箱子会将所有 capability 请求转发到主箱子
     * 如果不跳过副箱子，会导致物品被重复计算
     */
    private static boolean isSophisticatedStorageSubChest(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return false;
        
        // 检查是否是精妙储存模组的箱子实体
        String className = blockEntity.getClass().getName();
        if (!className.contains("sophisticatedstorage")) {
            return false;
        }
        
        try {
            // 使用反射检查 doubleMainPos 字段
            // 如果该字段不为 null，说明这是副箱子
            java.lang.reflect.Field field = blockEntity.getClass().getDeclaredField("doubleMainPos");
            field.setAccessible(true);
            Object value = field.get(blockEntity);
            return value != null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果没有这个字段，说明不是大箱子或者不是副箱子
            return false;
        }
    }
}
