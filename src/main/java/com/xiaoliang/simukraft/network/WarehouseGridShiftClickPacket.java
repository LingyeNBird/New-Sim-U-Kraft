package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.inventory.WarehouseGridMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 仓库网格Shift+点击数据包
 * 客户端请求将物品从仓库直接移动到玩家背包
 * 使用物品ID和NBT来精确识别物品
 */
public class WarehouseGridShiftClickPacket {
    private final BlockPos warehousePos;
    private final String itemId;  // 物品ID（如 "minecraft:oak_log"）
    private final CompoundTag nbtTag;  // 物品的NBT数据（可为null）

    public WarehouseGridShiftClickPacket(BlockPos warehousePos, String itemId, CompoundTag nbtTag) {
        this.warehousePos = warehousePos;
        this.itemId = itemId;
        this.nbtTag = nbtTag;
    }

    public WarehouseGridShiftClickPacket(FriendlyByteBuf buf) {
        this.warehousePos = Objects.requireNonNull(buf.readBlockPos());
        this.itemId = Objects.requireNonNull(buf.readUtf(32767));
        this.nbtTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(warehousePos));
        buf.writeUtf(Objects.requireNonNull(itemId));
        buf.writeNbt(nbtTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 检查玩家是否打开了仓库菜单
            if (player.containerMenu instanceof WarehouseGridMenu menu) {
                // 验证仓库位置
                if (!menu.getWarehousePos().equals(warehousePos)) {
                    return;
                }

                // 先提取物品到手上（包含NBT匹配）
                ItemStack extracted = menu.extractFromWarehouseByItemId(itemId, nbtTag, 64); // 尝试提取一组

                if (extracted.isEmpty()) {
                    return;
                }

                // 尝试将提取的物品放入玩家背包
                ItemStack remaining = extracted.copy();

                // 获取玩家背包
                net.minecraft.world.entity.player.Inventory inventory = player.getInventory();

                // 尝试将物品放入背包
                for (int i = 0; i < inventory.getContainerSize() && !remaining.isEmpty(); i++) {
                    ItemStack slotStack = inventory.getItem(i);
                    if (slotStack.isEmpty()) {
                        // 空槽位，直接放入
                        inventory.setItem(i, Objects.requireNonNull(remaining.copy()));
                        remaining = ItemStack.EMPTY;
                    } else if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(slotStack, remaining)) {
                        // 可以堆叠
                        int space = slotStack.getMaxStackSize() - slotStack.getCount();
                        int toAdd = Math.min(space, remaining.getCount());
                        if (toAdd > 0) {
                            slotStack.grow(toAdd);
                            remaining.shrink(toAdd);
                        }
                    }
                }

                // 如果有剩余，放回仓库
                if (!remaining.isEmpty()) {
                    ItemStack returnStack = menu.insertToWarehouse(remaining);
                    // 如果仓库也满了，掉在地上
                    if (!returnStack.isEmpty()) {
                        player.drop(returnStack, false);
                    }
                }

                // 刷新显示
                menu.refreshWarehouseDisplay();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getWarehousePos() {
        return warehousePos;
    }

    public String getItemId() {
        return itemId;
    }
}
