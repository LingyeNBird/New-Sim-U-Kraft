package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncNPCNamePacket {
    private final UUID npcUuid;
    private final String newName;

    public SyncNPCNamePacket(UUID npcUuid, String newName) {
        this.npcUuid = npcUuid;
        this.newName = newName;
    }

    public static void encode(SyncNPCNamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(Objects.requireNonNull(packet.npcUuid));
        buffer.writeUtf(Objects.requireNonNull(packet.newName), 32);
    }

    public static SyncNPCNamePacket decode(FriendlyByteBuf buffer) {
        UUID npcUuid = Objects.requireNonNull(buffer.readUUID());
        String newName = Objects.requireNonNull(buffer.readUtf(32));
        return new SyncNPCNamePacket(npcUuid, newName);
    }

    public static void handle(SyncNPCNamePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端处理NPC名称同步
            Simukraft.LOGGER.debug("[SyncNPCNamePacket] Received NPC name sync: NPC={}, newName={}", packet.npcUuid, packet.newName);
            
            // 更新NPC名称缓存
            com.xiaoliang.simukraft.utils.NPCDataManager.updateNPCNameCache(packet.npcUuid, packet.newName);
            
            // 更新在线实体名称（如果存在）
            updateClientNPCEntityName(packet.npcUuid, packet.newName);
            
            // 强制刷新打开的屏幕
            refreshOpenScreens(packet.npcUuid, packet.newName);
            
            Simukraft.LOGGER.debug("[SyncNPCNamePacket] NPC name sync complete: {} -> {}", packet.npcUuid, packet.newName);
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void updateClientNPCEntityName(UUID npcUuid, String newName) {

        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.level != null) {
            for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof com.xiaoliang.simukraft.entity.CustomEntity npc &&
                        entity.getUUID().equals(npcUuid)) {
                    npc.syncNameFromPacket(newName);
                    Simukraft.LOGGER.debug("[SyncNPCNamePacket] Updated client entity NPC name: {} -> {}", npcUuid, newName);
                    return;
                }
            }
        }
        Simukraft.LOGGER.debug("[SyncNPCNamePacket] NPC entity not found on client: {}", npcUuid);
    }
    
    private static void refreshOpenScreens(UUID npcUuid, String newName) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.screen != null) {
            // 检查是否是NPC交互屏幕
            if (client.screen instanceof com.xiaoliang.simukraft.client.gui.NPCInteractionScreen) {
                // NPC交互屏幕会自动刷新，因为我们已经修改了渲染逻辑
                Simukraft.LOGGER.debug("[SyncNPCNamePacket] NPCInteractionScreen is open, will update on next render");
            }
            
            // 检查是否是雇佣屏幕
            if (client.screen instanceof com.xiaoliang.simukraft.client.gui.AbstractHireScreen) {
                com.xiaoliang.simukraft.client.gui.AbstractHireScreen hireScreen = 
                    (com.xiaoliang.simukraft.client.gui.AbstractHireScreen) client.screen;
                // 强制刷新NPC列表
                hireScreen.refreshNPCList();
                Simukraft.LOGGER.debug("[SyncNPCNamePacket] Hire screen refreshed");
            }
            
            // 检查是否是市民管理屏幕
            if (client.screen instanceof com.xiaoliang.simukraft.client.gui.CityCitizenScreen) {
                // CityCitizenScreen已经处理了本地更新，不需要额外刷新
                Simukraft.LOGGER.debug("[SyncNPCNamePacket] CityCitizenScreen is open, local update already handled");
            }
        }
    }

    public UUID getNpcUuid() {
        return npcUuid;
    }

    public String getNewName() {
        return newName;
    }
}
