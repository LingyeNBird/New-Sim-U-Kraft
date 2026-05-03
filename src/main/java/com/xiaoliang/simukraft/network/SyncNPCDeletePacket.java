package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncNPCDeletePacket {
    private final UUID npcUuid;
    
    public SyncNPCDeletePacket(UUID npcUuid) {
        this.npcUuid = npcUuid;
    }
    
    public SyncNPCDeletePacket(FriendlyByteBuf buf) {
        this.npcUuid = Objects.requireNonNull(buf.readUUID());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(Objects.requireNonNull(npcUuid));
    }
    
    public static void handle(SyncNPCDeletePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端处理删除同步
            Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Received NPC delete sync: NPC={}", packet.npcUuid);
            
            // 从客户端世界中删除NPC实体
            deleteNPCEntityFromClient(packet.npcUuid);
            
            // 更新客户端缓存
            updateClientCache(packet.npcUuid);
            
            // 刷新打开的屏幕
            refreshOpenScreens(packet.npcUuid);
            
            Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Client NPC delete sync complete: {}", packet.npcUuid);
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void deleteNPCEntityFromClient(UUID npcUuid) {
        // 在客户端世界中查找并删除NPC实体
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.level != null) {
            // 使用 entitiesForRendering() 方法获取实体列表
            for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof com.xiaoliang.simukraft.entity.CustomEntity && 
                    entity.getUUID().equals(npcUuid)) {
                    entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Removed client entity NPC: {}", npcUuid);
                    return;
                }
            }
            Simukraft.LOGGER.debug("[SyncNPCDeletePacket] NPC entity not found on client: {}", npcUuid);
        }
    }
    
    private static void updateClientCache(UUID npcUuid) {
        try {
            // 更新NPC名称缓存
            com.xiaoliang.simukraft.utils.NPCDataManager.removeNPCFromCache(npcUuid);
            Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Updated client NPC cache: {}", npcUuid);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[SyncNPCDeletePacket] Failed to update client cache: {}", e.getMessage());
        }
    }
    
    private static void refreshOpenScreens(UUID npcUuid) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.screen != null) {
            // 如果当前屏幕是市民管理界面，刷新市民列表
            if (client.screen instanceof com.xiaoliang.simukraft.client.gui.CityCitizenScreen) {
                com.xiaoliang.simukraft.client.gui.CityCitizenScreen screen = 
                    (com.xiaoliang.simukraft.client.gui.CityCitizenScreen) client.screen;
                screen.refreshCitizenList();
                Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Refreshed CityCitizenScreen");
            }
            
            // 如果当前屏幕是雇佣界面，刷新NPC列表
            if (client.screen instanceof com.xiaoliang.simukraft.client.gui.AbstractHireScreen) {
                com.xiaoliang.simukraft.client.gui.AbstractHireScreen screen = 
                    (com.xiaoliang.simukraft.client.gui.AbstractHireScreen) client.screen;
                screen.refreshNPCList();
                Simukraft.LOGGER.debug("[SyncNPCDeletePacket] Refreshed hire screen");
            }
        }
    }
    
    public UUID getNpcUuid() {
        return npcUuid;
    }
}
