package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveFarmlandDataPacket {
    
    public SaveFarmlandDataPacket() {
        // 空构造函数，这个数据包不需要参数
    }
    
    public SaveFarmlandDataPacket(FriendlyByteBuf buf) {
        // 空构造函数，这个数据包不需要参数
    }
    
    public void encode(FriendlyByteBuf buf) {
        // 空方法，这个数据包不需要参数
    }
    
    public static void handle(SaveFarmlandDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Simukraft.LOGGER.info("[SaveFarmlandDataPacket] Received save farmland data request, player: {}", player.getName().getString());
                
                // 在服务器端保存农田盒数据
                try {
                    com.xiaoliang.simukraft.world.FarmlandHiredData.saveAllFarmlandData(player.server);
                    Simukraft.LOGGER.info("[SaveFarmlandDataPacket] Farmland box data saved to server");
                } catch (Exception e) {
                    Simukraft.LOGGER.error("[SaveFarmlandDataPacket] Failed to save farmland data: {}", e.getMessage());
                }
            }
        });
        context.setPacketHandled(true);
    }
}