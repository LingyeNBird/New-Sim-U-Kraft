package com.xiaoliang.simukraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步建筑盒雇佣状态的网络包
 * 服务器 -> 客户端
 * 用于服务器重启后同步雇佣数据给客户端
 */
public class SyncBuildBoxHireStatusPacket {
    private final BlockPos buildBoxPos;
    private final UUID builderUuid;  // null表示没有雇佣建筑师
    private final UUID plannerUuid;  // null表示没有雇佣规划师
    private final String builderName;
    private final String plannerName;

    public SyncBuildBoxHireStatusPacket(BlockPos pos, UUID builderUuid, UUID plannerUuid, 
                                        String builderName, String plannerName) {
        this.buildBoxPos = pos;
        this.builderUuid = builderUuid;
        this.plannerUuid = plannerUuid;
        this.builderName = builderName;
        this.plannerName = plannerName;
    }

    public SyncBuildBoxHireStatusPacket(FriendlyByteBuf buf) {
        this.buildBoxPos = Objects.requireNonNull(buf.readBlockPos());
        this.builderUuid = buf.readBoolean() ? Objects.requireNonNull(buf.readUUID()) : null;
        this.plannerUuid = buf.readBoolean() ? Objects.requireNonNull(buf.readUUID()) : null;
        this.builderName = Objects.requireNonNull(buf.readUtf());
        this.plannerName = Objects.requireNonNull(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(buildBoxPos));
        buf.writeBoolean(builderUuid != null);
        if (builderUuid != null) {
            buf.writeUUID(builderUuid);
        }
        buf.writeBoolean(plannerUuid != null);
        if (plannerUuid != null) {
            buf.writeUUID(plannerUuid);
        }
        buf.writeUtf(builderName != null ? builderName : "");
        buf.writeUtf(plannerName != null ? plannerName : "");
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端更新BuildBoxData
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level != null) {
                // 更新建筑师数据
                if (builderUuid != null) {
                    com.xiaoliang.simukraft.client.gui.BuildBoxData.setHiredBuilder(buildBoxPos, builderUuid);
                } else {
                    com.xiaoliang.simukraft.client.gui.BuildBoxData.clearHiredBuilder(buildBoxPos);
                }
                // 更新规划师数据
                if (plannerUuid != null) {
                    com.xiaoliang.simukraft.client.gui.BuildBoxData.setHiredPlanner(buildBoxPos, plannerUuid);
                } else {
                    com.xiaoliang.simukraft.client.gui.BuildBoxData.clearHiredPlanner(buildBoxPos);
                }

                // 如果当前打开的是BuildBoxScreen，刷新界面
                if (minecraft.screen instanceof com.xiaoliang.simukraft.client.gui.BuildBoxScreen screen) {
                    // 检查是否是同一个建筑盒
                    if (screen.getBuildBoxPos().equals(buildBoxPos)) {
                        screen.refreshButtonStates();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getBuildBoxPos() {
        return buildBoxPos;
    }
}
