package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.BuildingDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public record BuildingNBTRequestPacket(String buildingName, String category) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(buildingName);
        buf.writeUtf(category);
    }

    public static BuildingNBTRequestPacket decode(FriendlyByteBuf buf) {
        return new BuildingNBTRequestPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(BuildingNBTRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            String buildingName = packet.buildingName();
            String category = packet.category();

            // 从BuildingDataManager获取NBT数据
            CompoundTag buildingData = BuildingDataManager.loadBuildingData(buildingName, category);
            if (context.getSender() == null || buildingData == null) {
                return;
            }

            // 发送响应包给客户端
            BuildingNBTResponsePacket response = new BuildingNBTResponsePacket(buildingName, category, buildingData);
            NetworkManager.INSTANCE.sendTo(response, context.getSender().connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
