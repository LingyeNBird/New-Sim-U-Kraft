package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.CityCitizenScreen;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 城市市民列表响应数据包
 */
@SuppressWarnings({"null", "unused"})
public record CitizenListResponsePacket(List<CitizenInfo> citizenInfos) {

    public CitizenListResponsePacket(FriendlyByteBuf buf) {
        this(createCitizenInfosFromBuffer(buf));
    }

    private static List<CitizenInfo> createCitizenInfosFromBuffer(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<CitizenInfo> infos = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            int npcId = buf.readInt();
            boolean hasResidence = buf.readBoolean();
            String job = buf.readUtf();
            String skinPath = buf.readUtf();
            int level = buf.readInt();
            int xp = buf.readInt();
            boolean hasPosition = buf.readBoolean();
            BlockPos position = hasPosition ? buf.readBlockPos() : null;
            infos.add(new CitizenInfo(uuid, name, npcId, hasResidence, job, skinPath, level, xp, hasPosition, position));
        }
        return infos;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(citizenInfos.size());
        for (CitizenInfo info : citizenInfos) {
            buf.writeUUID(info.uuid());
            buf.writeUtf(info.name());
            buf.writeInt(info.npcId());
            buf.writeBoolean(info.hasResidence());
            buf.writeUtf(info.job());
            buf.writeUtf(info.skinPath());
            buf.writeInt(info.level());
            buf.writeInt(info.xp());
            buf.writeBoolean(info.hasPosition());
            if (info.hasPosition() && info.position() != null) {
                buf.writeBlockPos(info.position());
            }
        }
    }

    public static void handle(CitizenListResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 在客户端更新市民列表
            CityCitizenScreen.setCitizenList(packet.citizenInfos());
        });
        context.setPacketHandled(true);
    }

    /**
     * 市民信息记录类
     */
    public record CitizenInfo(UUID uuid, String name, int npcId, boolean hasResidence, String job, String skinPath, int level, int xp,
                              boolean hasPosition, BlockPos position) {
    }
}
