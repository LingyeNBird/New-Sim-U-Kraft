package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.inventory.WarehouseGridMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 仓库网格插入物品数据包
 * 客户端请求将手上的物品插入仓库
 */
public class WarehouseGridInsertPacket {
    private final BlockPos warehousePos;

    public WarehouseGridInsertPacket(BlockPos warehousePos) {
        this.warehousePos = warehousePos;
    }

    public WarehouseGridInsertPacket(FriendlyByteBuf buf) {
        this.warehousePos = Objects.requireNonNull(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(warehousePos));
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

                // 获取玩家手上的物品
                ItemStack carried = player.containerMenu.getCarried();
                if (carried.isEmpty()) {
                    return;
                }

                // 执行插入操作
                ItemStack remaining = menu.insertToWarehouse(Objects.requireNonNull(carried.copy()));
                int inserted = carried.getCount() - remaining.getCount();

                if (inserted > 0) {
                    // 更新手上的物品
                    carried.shrink(inserted);
                    if (carried.isEmpty()) {
                        player.containerMenu.setCarried(Objects.requireNonNull(ItemStack.EMPTY));
                    }
                    // 刷新显示
                    menu.refreshWarehouseDisplay();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getWarehousePos() {
        return warehousePos;
    }
}
