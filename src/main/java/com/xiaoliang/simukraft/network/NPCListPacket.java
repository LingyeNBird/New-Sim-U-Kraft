package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class NPCListPacket {
    private final List<NPCInfo> npcList;

    public NPCListPacket(List<NPCInfo> npcList) {
        this.npcList = npcList;
    }

    public NPCListPacket(Map<UUID, String> npcMap) {
        // 兼容旧版本，只包含UUID和名字
        this.npcList = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : npcMap.entrySet()) {
            npcList.add(new NPCInfo(entry.getKey(), entry.getValue(), "", 1, 0));
        }
    }

    public NPCListPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.npcList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            String skinPath = buf.readUtf();
            int level = buf.readInt();
            int xp = buf.readInt();
            npcList.add(new NPCInfo(uuid, name, skinPath, level, xp));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcList.size());
        for (NPCInfo info : npcList) {
            buf.writeUUID(info.uuid());
            buf.writeUtf(info.name());
            buf.writeUtf(info.skinPath());
            buf.writeInt(info.level());
            buf.writeInt(info.xp());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();

            // 使用新的方法传递完整信息
            if (mc.screen instanceof AbstractHireScreen) {
                ((AbstractHireScreen) mc.screen).updateNPCList(npcList);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * NPC信息记录类
     */
    public record NPCInfo(UUID uuid, String name, String skinPath, int level, int xp) {
    }
}
