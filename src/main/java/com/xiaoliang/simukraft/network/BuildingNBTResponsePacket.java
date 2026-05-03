package com.xiaoliang.simukraft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class BuildingNBTResponsePacket {
    private final String buildingName;
    private final String category;
    private final CompoundTag buildingData;

    public BuildingNBTResponsePacket(String buildingName, String category, CompoundTag buildingData) {
        this.buildingName = buildingName;
        this.category = category;
        this.buildingData = buildingData;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(buildingName);
        buf.writeUtf(category);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(buildingData, baos);
            byte[] data = baos.toByteArray();
            buf.writeInt(data.length);
            buf.writeBytes(data);
        } catch (IOException e) {
            buf.writeInt(0);
            e.printStackTrace();
        }
    }

    public BuildingNBTResponsePacket(FriendlyByteBuf buf) {
        this.buildingName = buf.readUtf();
        this.category = buf.readUtf();
        int dataLength = buf.readInt();
        CompoundTag tempBuildingData = null;
        if (dataLength > 0) {
            byte[] data = buf.readBytes(dataLength).array();
            try {
                tempBuildingData = NbtIo.readCompressed(new ByteArrayInputStream(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.buildingData = tempBuildingData;
    }

    public static void handle(BuildingNBTResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 处理逻辑将在客户端实现
        });
        context.setPacketHandled(true);
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getCategory() {
        return category;
    }

    public CompoundTag getBuildingData() {
        return buildingData;
    }
}
