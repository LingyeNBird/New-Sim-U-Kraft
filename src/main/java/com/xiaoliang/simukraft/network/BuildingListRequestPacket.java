package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.BuildingDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("null")
public record BuildingListRequestPacket(String category) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(category);
    }

    public static BuildingListRequestPacket decode(FriendlyByteBuf buf) {
        return new BuildingListRequestPacket(buf.readUtf());
    }

    public static void handle(BuildingListRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            String category = packet.category();

            // 从BuildingDataManager获取建筑列表
            List<BuildingDataManager.BuildingInfo> buildings = BuildingDataManager.getBuildingsByCategory(category);

            // 转换为网络可用的BuildingInfo格式
            List<BuildingListResponsePacket.BuildingInfo> networkBuildingInfos = new ArrayList<>();
            for (BuildingDataManager.BuildingInfo info : buildings) {
                String baseName = info.getFileName().replace(".sk", "");
                String nbtFileName = baseName + ".nbt";

                networkBuildingInfos.add(new BuildingListResponsePacket.BuildingInfo(
                        info.getName(),
                        info.getSize(),
                        info.getAmount(),
                        info.getAuthor(),
                        info.getDescription(),
                        info.getCategory(),
                        info.getFileName(),
                        nbtFileName,
                        info.getTags()
                ));
            }

            // 发送响应包给客户端
            BuildingListResponsePacket response = new BuildingListResponsePacket(networkBuildingInfos);
            NetworkManager.INSTANCE.sendTo(response, context.getSender().connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
