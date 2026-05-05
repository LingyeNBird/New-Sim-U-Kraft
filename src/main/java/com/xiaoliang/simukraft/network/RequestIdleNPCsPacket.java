package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.employment.bridge.EmploymentLegacyBridge;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import com.xiaoliang.simukraft.utils.NPCRestHandler;
import com.xiaoliang.simukraft.utils.NPCTaskScheduler;
import com.xiaoliang.simukraft.world.CityPermissionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class RequestIdleNPCsPacket {
    public RequestIdleNPCsPacket() {}

    public RequestIdleNPCsPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {
        // 发送一个简单的标记，确保数据包不为空
        buf.writeBoolean(true);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                List<NPCListPacket.NPCInfo> npcList = new ArrayList<>();
                Set<UUID> addedNpcUuids = new HashSet<>();
                Set<UUID> hiredNpcUuids = collectHiredNpcUuids(player.getServer());
                Map<UUID, CustomEntity> loadedNpcsByUuid = collectLoadedNpcs(player);
                Map<UUID, NPCDataManager.NPCBasicData> npcBasicData = NPCDataManager.getAllNPCBasicData(player.getServer());
                Map<UUID, NPCDataManager.SkillDataCache> npcSkillData = NPCDataManager.getAllNPCSkillData(player.getServer());

                CityPermissionManager permManager = CityPermissionManager.getInstance();
                ServerLevel playerLevel = player.serverLevel();
                String playerName = player.getName().getString();
                UUID playerCityId = permManager.getPlayerCityId(playerLevel, playerName);

                if (playerCityId == null) {
                    playerCityId = permManager.getPlayerCityId(playerLevel, player.getUUID());
                }

                if (playerCityId == null || !permManager.canViewNPCList(playerLevel, playerName, playerCityId)) {
                    NetworkManager.INSTANCE.sendTo(
                            new NPCListPacket(npcList),
                            player.connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                    );
                    return;
                }

                for (CustomEntity npc : loadedNpcsByUuid.values()) {
                    UUID npcUuid = npc.getUUID();
                    if (npc.getWorkStatus() != WorkStatus.IDLE
                            || NPCRestHandler.isLocked(npcUuid)
                            || NPCDataManager.isNPCPregnantOrInLabor(player.getServer(), npcUuid)
                            || hiredNpcUuids.contains(npcUuid)) {
                        continue;
                    }
                    if (!isSameCity(playerCityId, npc.getCityIdString())) {
                        continue;
                    }
                    npcList.add(toNpcInfo(npcUuid, npc.getFullName(), npc.getSkinPath(), npcSkillData));
                    addedNpcUuids.add(npcUuid);
                }

                for (Map.Entry<UUID, NPCDataManager.NPCBasicData> entry : npcBasicData.entrySet()) {
                    UUID npcUuid = entry.getKey();
                    if (addedNpcUuids.contains(npcUuid)) {
                        continue;
                    }
                    if (hiredNpcUuids.contains(npcUuid)
                            || NPCRestHandler.isLocked(npcUuid)
                            || NPCDataManager.isNPCPregnantOrInLabor(player.getServer(), npcUuid)) {
                        continue;
                    }

                    NPCDataManager.NPCBasicData basicData = entry.getValue();
                    if (basicData == null || basicData.name() == null || basicData.name().isEmpty()) {
                        continue;
                    }
                    if (!isSameCity(playerCityId, basicData.cityId())) {
                        continue;
                    }

                    npcList.add(toNpcInfo(npcUuid, basicData.name(), basicData.skinPath(), npcSkillData));
                    addedNpcUuids.add(npcUuid);
                }

                NetworkManager.INSTANCE.sendTo(
                        new NPCListPacket(npcList),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static Set<UUID> collectHiredNpcUuids(net.minecraft.server.MinecraftServer server) {
        return new HashSet<>(EmploymentLegacyBridge.loadAllHiredNPCs(server).keySet());
    }

    private static Map<UUID, CustomEntity> collectLoadedNpcs(ServerPlayer player) {
        Map<UUID, CustomEntity> loadedNpcs = new HashMap<>();
        if (player == null || player.getServer() == null) {
            return loadedNpcs;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (CustomEntity npc : NPCTaskScheduler.getNPCsInLevel(level)) {
                loadedNpcs.put(npc.getUUID(), npc);
            }
        }
        return loadedNpcs;
    }

    private static boolean isSameCity(UUID playerCityId, String npcCityIdStr) {
        if (playerCityId == null || npcCityIdStr == null || npcCityIdStr.isEmpty()) {
            return false;
        }
        try {
            return playerCityId.equals(UUID.fromString(npcCityIdStr));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static NPCListPacket.NPCInfo toNpcInfo(UUID npcUuid,
                                                   String name,
                                                   String skinPath,
                                                   Map<UUID, NPCDataManager.SkillDataCache> npcSkillData) {
        NPCDataManager.SkillDataCache skillData = npcSkillData.get(npcUuid);
        int level = skillData != null ? skillData.level : 1;
        int xp = skillData != null ? skillData.xp : 0;
        return new NPCListPacket.NPCInfo(npcUuid, name, skinPath == null ? "" : skinPath, level, xp);
    }
}
