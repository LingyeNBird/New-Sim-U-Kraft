package com.xiaoliang.simukraft.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 服务器返回仓库物品数据
 */
public class WarehouseGridResponsePacket {

    private final List<ItemStack> items;
    private final List<BlockPos> containerPositions;
    private final List<Integer> actualCounts;

    public WarehouseGridResponsePacket(List<ItemStack> items, List<BlockPos> containerPositions, List<Integer> actualCounts) {
        this.items = items != null ? items : new ArrayList<>();
        this.containerPositions = containerPositions != null ? containerPositions : new ArrayList<>();
        this.actualCounts = actualCounts != null ? actualCounts : new ArrayList<>();
    }

    public WarehouseGridResponsePacket(FriendlyByteBuf buf) {
        // 读取物品
        int size = buf.readInt();
        this.items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(Objects.requireNonNull(buf.readItem()));
        }

        // 读取容器位置
        int posCount = buf.readInt();
        this.containerPositions = new ArrayList<>();
        for (int i = 0; i < posCount; i++) {
            containerPositions.add(Objects.requireNonNull(buf.readBlockPos()));
        }

        // 读取实际数量
        int countSize = buf.readInt();
        this.actualCounts = new ArrayList<>();
        for (int i = 0; i < countSize; i++) {
            actualCounts.add(buf.readInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        // 写入物品
        buf.writeInt(items.size());
        for (ItemStack item : items) {
            buf.writeItem(Objects.requireNonNull(item));
        }

        // 写入容器位置
        buf.writeInt(containerPositions.size());
        for (BlockPos pos : containerPositions) {
            buf.writeBlockPos(Objects.requireNonNull(pos));
        }

        // 写入实际数量
        buf.writeInt(actualCounts.size());
        for (int count : actualCounts) {
            buf.writeInt(count);
        }
    }

    public void handle(Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var screen = Minecraft.getInstance().screen;

            if (screen instanceof com.xiaoliang.simukraft.client.gui.WarehouseGridContainerScreen containerScreen) {
                containerScreen.receiveItems(items, containerPositions, actualCounts);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public List<BlockPos> getContainerPositions() {
        return containerPositions;
    }

    public List<Integer> getActualCounts() {
        return actualCounts;
    }
}
