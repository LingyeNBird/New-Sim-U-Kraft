package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.NPCFamilyInfoCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class NPCFamilyInfoResponsePacket {
    private final UUID npcUuid;
    private final String spouseName;
    private final String pregnancyStage;

    public NPCFamilyInfoResponsePacket(UUID npcUuid, String spouseName, String pregnancyStage) {
        this.npcUuid = npcUuid;
        this.spouseName = spouseName;
        this.pregnancyStage = pregnancyStage;
    }

    public NPCFamilyInfoResponsePacket(FriendlyByteBuf buf) {
        this.npcUuid = buf.readUUID();
        this.spouseName = buf.readUtf();
        this.pregnancyStage = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(npcUuid);
        buf.writeUtf(spouseName != null ? spouseName : "");
        buf.writeUtf(pregnancyStage != null ? pregnancyStage : "");
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> Minecraft.getInstance().execute(() ->
                NPCFamilyInfoCache.put(npcUuid, spouseName, pregnancyStage)
        ));
        ctx.get().setPacketHandled(true);
    }
}
