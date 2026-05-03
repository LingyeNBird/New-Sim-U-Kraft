package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.client.ClientToastHUDOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public class ShowToastPacket {
    private final String toastType;
    private final int upgradeLevel;
    
    public ShowToastPacket(String toastType, int upgradeLevel) {
        this.toastType = toastType;
        this.upgradeLevel = upgradeLevel;
    }
    
    public static void encode(ShowToastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(Objects.requireNonNull(packet.toastType));
        buffer.writeInt(packet.upgradeLevel);
    }
    
    public static ShowToastPacket decode(FriendlyByteBuf buffer) {
        return new ShowToastPacket(Objects.requireNonNull(buffer.readUtf()), buffer.readInt());
    }
    
    public static void handle(ShowToastPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在客户端处理，显示toast
            Minecraft mc = Minecraft.getInstance();
            var player = mc.player;
            if (player != null) {
                ClientToastHUDOverlay.showToast(packet.toastType, packet.upgradeLevel, player.getUUID());
                Simukraft.LOGGER.debug("[ShowToastPacket] Received: type={}, upgradeLevel={}, player={}", packet.toastType, packet.upgradeLevel, player.getName().getString());
            } else {
                Simukraft.LOGGER.debug("[ShowToastPacket] Received but player is null: {}", packet.toastType);
            }
        });
        context.get().setPacketHandled(true);
    }
    
    // 发送数据包到特定玩家（带升级等级）
    public static void sendToPlayer(ServerPlayer player, String toastType, int upgradeLevel) {
        NetworkManager.INSTANCE.sendTo(
                new ShowToastPacket(toastType, upgradeLevel),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }
    
    // 发送数据包到特定玩家（兼容旧版本，无升级等级）
    public static void sendToPlayer(ServerPlayer player, String toastType) {
        // 默认使用等级0
        sendToPlayer(player, toastType, 0);
    }
}
