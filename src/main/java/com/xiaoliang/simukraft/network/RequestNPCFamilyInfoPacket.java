package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.utils.NPCDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class RequestNPCFamilyInfoPacket {
    private final UUID npcUuid;

    public RequestNPCFamilyInfoPacket(UUID npcUuid) {
        this.npcUuid = npcUuid;
    }

    public RequestNPCFamilyInfoPacket(FriendlyByteBuf buf) {
        this.npcUuid = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(npcUuid);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || npcUuid == null) {
                return;
            }

            String spouseName = NPCDataManager.getNPCSpouseName(player.getServer(), npcUuid);
            NPCDataManager.NPCPregnancyData pregnancyData = NPCDataManager.getNPCPregnancyData(player.getServer(), npcUuid);
            String pregnancyStage = pregnancyData != null ? pregnancyData.stage() : "";

            NetworkManager.INSTANCE.sendTo(
                    new NPCFamilyInfoResponsePacket(npcUuid, spouseName, pregnancyStage),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
