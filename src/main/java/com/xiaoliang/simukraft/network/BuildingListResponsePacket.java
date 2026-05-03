package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.BuildingListScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("null")
public record BuildingListResponsePacket(List<BuildingInfo> buildingInfos) {

    public BuildingListResponsePacket(FriendlyByteBuf buf) {
        this(createBuildingInfosFromBuffer(buf));  // 现在这是第一行语句
    }

    private static List<BuildingInfo> createBuildingInfosFromBuffer(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<BuildingInfo> infos = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            String sizeStr = buf.readUtf();
            String amount = buf.readUtf();
            String author = buf.readUtf();
            String description = buf.readUtf();
            String category = buf.readUtf();
            String fileName = buf.readUtf();
            String nbtFileName = buf.readUtf();
            // 读取标签列表
            int tagCount = buf.readInt();
            List<String> tags = new ArrayList<>(tagCount);
            for (int j = 0; j < tagCount; j++) {
                tags.add(buf.readUtf());
            }
            infos.add(new BuildingInfo(name, sizeStr, amount, author, description, category, fileName, nbtFileName, tags));
        }
        return infos;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(buildingInfos.size());
        for (BuildingInfo info : buildingInfos) {
            buf.writeUtf(info.name);
            buf.writeUtf(info.size);
            buf.writeUtf(info.amount);
            buf.writeUtf(info.author);
            buf.writeUtf(info.description);
            buf.writeUtf(info.category);
            buf.writeUtf(info.fileName);
            buf.writeUtf(info.nbtFileName);
            // 写入标签列表
            buf.writeInt(info.tags.size());
            for (String tag : info.tags) {
                buf.writeUtf(tag);
            }
        }
    }

    public static void handle(BuildingListResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 在客户端更新建筑列表
            BuildingListScreen.setBuildingList(packet.buildingInfos());
        });
        context.setPacketHandled(true);
    }

    public record BuildingInfo(String name, String size, String amount, String author, String description,
                               String category, String fileName, String nbtFileName, List<String> tags) {
    }
}
