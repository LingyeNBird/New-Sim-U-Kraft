package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 建筑盒被拆除的通知数据包
 * 服务器 -> 客户端
 * 当建筑盒被破坏时，通知所有客户端清除对应的雇佣数据
 */
@SuppressWarnings("null")
public class BuildBoxDestroyedPacket {
    private final BlockPos buildBoxPos;

    public BuildBoxDestroyedPacket(BlockPos pos) {
        this.buildBoxPos = pos;
    }

    public BuildBoxDestroyedPacket(FriendlyByteBuf buf) {
        this.buildBoxPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(buildBoxPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端清除建筑盒的雇佣数据
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level != null) {
                // 清除建筑师和规划师的雇佣数据
                com.xiaoliang.simukraft.client.gui.BuildBoxData.clearHiredBuilder(buildBoxPos);
                com.xiaoliang.simukraft.client.gui.BuildBoxData.clearHiredPlanner(buildBoxPos);

                Simukraft.LOGGER.debug("[BuildBoxDestroyedPacket] Client cleared hire data for build box {} ", buildBoxPos);

                // 如果当前打开的是BuildBoxScreen，刷新界面
                if (minecraft.screen instanceof com.xiaoliang.simukraft.client.gui.BuildBoxScreen screen) {
                    // 检查是否是同一个建筑盒
                    if (screen.getBuildBoxPos().equals(buildBoxPos)) {
                        screen.refreshButtonStates();
                        Simukraft.LOGGER.debug("[BuildBoxDestroyedPacket] Refreshing build box screen state");
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
