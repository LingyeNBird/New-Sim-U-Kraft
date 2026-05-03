package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.NPCResidenceCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.xiaoliang.simukraft.client.NPCResidenceCache;

/**
 * NPC居住信息响应网络包（服务器 -> 客户端）
 */
@SuppressWarnings({"null", "unused"})
public class NPCResidenceResponsePacket {
    private final String npcName;
    private final boolean hasResidence;
    private final String position;

    public NPCResidenceResponsePacket(String npcName, boolean hasResidence, String position) {
        this.npcName = npcName;
        this.hasResidence = hasResidence;
        this.position = position;
    }

    public NPCResidenceResponsePacket(FriendlyByteBuf buf) {
        this.npcName = buf.readUtf();
        this.hasResidence = buf.readBoolean();
        this.position = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcName);
        buf.writeBoolean(hasResidence);
        buf.writeUtf(position != null ? position : "");
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端缓存NPC居住信息
            NPCResidenceCache.setResidenceInfo(npcName, hasResidence, position);
        });
        ctx.get().setPacketHandled(true);
    }

    public String getNpcName() {
        return npcName;
    }

    public boolean hasResidence() {
        return hasResidence;
    }

    public String getPosition() {
        return position;
    }
}
