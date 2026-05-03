package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.employment.bridge.EmploymentLegacyBridge;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import com.xiaoliang.simukraft.utils.NPCRestHandler;
import com.xiaoliang.simukraft.world.CityPermissionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
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
                // 用于检查重复的UUID集合
                Set<UUID> addedNpcUuids = new HashSet<>();
                // 统一收集所有已在岗NPC，避免重复出现在雇佣候选里
                Set<UUID> hiredNpcUuids = collectHiredNpcUuids(player.getServer());

                // 使用权限管理器获取玩家城市ID和检查权限（使用玩家名）
                CityPermissionManager permManager = CityPermissionManager.getInstance();
                ServerLevel playerLevel = player.serverLevel();
                String playerName = player.getName().getString();
                UUID playerCityId = permManager.getPlayerCityId(playerLevel, playerName);

                // 兼容旧档：名称映射缺失时，尝试通过UUID查找所属城市（至少覆盖市长）
                if (playerCityId == null) {
                    playerCityId = permManager.getPlayerCityId(playerLevel, player.getUUID());
                }

                // 市长/官员限定：不在任何城市或无权限都返回空列表
                if (playerCityId == null || !permManager.canViewNPCList(playerLevel, playerName, playerCityId)) {
                    NetworkManager.INSTANCE.sendTo(
                            new NPCListPacket(npcList),
                            player.connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                    );
                    return;
                }

                // 方法1：从当前加载的NPC实体中获取空闲NPC
                for (ServerLevel level : player.getServer().getAllLevels()) {
                    for (CustomEntity npc : level.getEntitiesOfClass(
                            CustomEntity.class,
                            new AABB(-10000, -10000, -10000, 10000, 10000, 10000))) {
                        // 检查NPC是否空闲且不在休息中（锁定状态）
                        if (npc.getWorkStatus() == WorkStatus.IDLE
                                && !NPCRestHandler.isLocked(npc.getUUID())
                                && !hiredNpcUuids.contains(npc.getUUID())) {
                            // 仅显示同城NPC（市长/官员限定）
                            String npcCityIdStr = npc.getCityIdString();
                            if (npcCityIdStr != null && !npcCityIdStr.isEmpty()) {
                                try {
                                    UUID npcCityId = UUID.fromString(npcCityIdStr);
                                    if (npcCityId.equals(playerCityId)) {
                                        String skinPath = npc.getSkinPath();
                                        // 获取NPC等级和经验值
                                        int npcLevel = NPCDataManager.getNPCLevel(player.getServer(), npc.getUUID());
                                        int npcXp = NPCDataManager.getNPCXp(player.getServer(), npc.getUUID());
                                        npcList.add(new NPCListPacket.NPCInfo(
                                            npc.getUUID(),
                                            npc.getFullName(),
                                            skinPath,
                                            npcLevel,
                                            npcXp
                                        ));
                                        addedNpcUuids.add(npc.getUUID());
                                    }
                                } catch (IllegalArgumentException e) {
                                    // 如果城市ID格式不正确，跳过此NPC
                                }
                            }
                        }
                    }
                }

                // 方法2：从V2雇佣存储获取空闲NPC（包括视距外的NPC）
                // 获取所有已雇佣的NPC UUID
                Set<UUID> allHiredNpcs = EmploymentLegacyBridge.loadAllHiredNPCs(player.getServer()).keySet();

                // 从NPC数据管理器获取所有NPC数据
                for (UUID npcUuid : NPCDataManager.getAllNPCUuids(player.getServer())) {
                    // 检查是否已经在列表中
                    if (addedNpcUuids.contains(npcUuid)) {
                        continue;
                    }

                    // 检查是否已被雇佣（从V2存储）
                    if (allHiredNpcs.contains(npcUuid) || hiredNpcUuids.contains(npcUuid)) {
                        continue;
                    }

                    // 检查NPC是否处于休息中（锁定状态）
                    if (NPCRestHandler.isLocked(npcUuid)) {
                        continue;
                    }

                    // 获取NPC名称和城市ID
                    String npcName = NPCDataManager.getNPCName(player.getServer(), npcUuid);
                    String npcCityIdStr = NPCDataManager.getNPCCityId(player.getServer(), npcUuid);
                    String skinPath = NPCDataManager.getNPCSkinPath(player.getServer(), npcUuid);

                    if (npcName == null || npcName.isEmpty()) {
                        continue;
                    }

                    // 检查NPC是否属于同一城市
                    boolean shouldShow = false;
                    if (npcCityIdStr != null && !npcCityIdStr.isEmpty()) {
                        try {
                            UUID npcCityId = UUID.fromString(npcCityIdStr);
                            if (npcCityId.equals(playerCityId)) {
                                shouldShow = true;
                            }
                        } catch (IllegalArgumentException e) {
                            // 城市ID格式不正确，继续尝试实体检查
                        }
                    }

                    // 如果文件中没有城市ID，尝试通过实体检查城市
                    if (!shouldShow) {
                        for (ServerLevel checkLevel : player.getServer().getAllLevels()) {
                            CustomEntity npc = checkLevel.getEntitiesOfClass(CustomEntity.class,
                                            new AABB(-10000, -10000, -10000, 10000, 10000, 10000))
                                    .stream()
                                    .filter(e -> e.getUUID().equals(npcUuid))
                                    .findFirst()
                                    .orElse(null);
                            if (npc != null) {
                                npcCityIdStr = npc.getCityIdString();
                                if (npcCityIdStr != null && !npcCityIdStr.isEmpty()) {
                                    try {
                                        UUID npcCityId = UUID.fromString(npcCityIdStr);
                                        if (npcCityId.equals(playerCityId)) {
                                            shouldShow = true;
                                            skinPath = npc.getSkinPath();
                                            break;
                                        }
                                    } catch (IllegalArgumentException e) {
                                                                // 城市ID格式不正确
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                    // 添加应该显示的NPC
                    if (shouldShow) {
                        // 获取NPC等级和经验值
                        int npcLevelValue = NPCDataManager.getNPCLevel(player.getServer(), npcUuid);
                        int xp = NPCDataManager.getNPCXp(player.getServer(), npcUuid);
                        npcList.add(new NPCListPacket.NPCInfo(npcUuid, npcName, skinPath, npcLevelValue, xp));
                        addedNpcUuids.add(npcUuid);
                    }
                }

                // 发送NPC列表给客户端
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
        // 使用V2存储获取所有已雇佣的NPC
        return EmploymentLegacyBridge.loadAllHiredNPCs(server).keySet();
    }
}
