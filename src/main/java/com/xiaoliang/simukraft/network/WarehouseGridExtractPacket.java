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
 * 仓库网格提取物品数据包
 * 客户端请求从仓库提取物品到手上
 * 使用物品ID和NBT来精确识别物品
 */
public class WarehouseGridExtractPacket {
    private final BlockPos warehousePos;
    private final String itemId;  // 物品ID（如 "minecraft:oak_log"）
    private final CompoundTag nbtTag;  // 物品的NBT数据（可为null）
    private final int count;

    public WarehouseGridExtractPacket(BlockPos warehousePos, String itemId, CompoundTag nbtTag, int count) {
        this.warehousePos = warehousePos;
        this.itemId = itemId;
        this.nbtTag = nbtTag;
        this.count = count;
    }

    public WarehouseGridExtractPacket(FriendlyByteBuf buf) {
        this.warehousePos = Objects.requireNonNull(buf.readBlockPos());
        this.itemId = Objects.requireNonNull(buf.readUtf(32767));
        this.nbtTag = buf.readNbt();
        this.count = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(warehousePos));
        buf.writeUtf(Objects.requireNonNull(itemId));
        buf.writeNbt(nbtTag);
        buf.writeInt(count);
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

                // 执行提取操作（根据物品ID和NBT）
                ItemStack extracted = menu.extractFromWarehouseByItemId(itemId, nbtTag, count);

                if (!extracted.isEmpty()) {
                    // 设置玩家手上的物品
                    player.containerMenu.setCarried(extracted);
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

    public String getItemId() {
        return itemId;
    }

    public CompoundTag getNbtTag() {
        return nbtTag;
    }

    public int getCount() {
        return count;
    }
}
