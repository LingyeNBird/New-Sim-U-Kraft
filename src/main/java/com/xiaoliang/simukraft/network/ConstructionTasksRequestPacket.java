package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.building.ConstructionTask;
import com.xiaoliang.simukraft.client.gui.ConstructionTasksScreen;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class ConstructionTasksRequestPacket {
    private final BlockPos cityCorePos;

    public ConstructionTasksRequestPacket(BlockPos cityCorePos) {
        this.cityCorePos = cityCorePos;
    }

    public ConstructionTasksRequestPacket(FriendlyByteBuf buf) {
        this.cityCorePos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.cityCorePos);
    }

    public static ConstructionTasksRequestPacket decode(FriendlyByteBuf buf) {
        return new ConstructionTasksRequestPacket(buf);
    }

    public static void handle(ConstructionTasksRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                CityData cityData = CityData.get(level);

                // 获取城市信息
                CityData.CityInfo city = cityData.getCityByCorePos(packet.cityCorePos);
                if (city != null) {
                    List<ConstructionTasksResponsePacket.TaskInfo> taskInfos = new ArrayList<>();

                    // 遍历城市的所有市民
                    for (UUID citizenId : city.getCitizenIds()) {
                        Entity entity = level.getEntity(citizenId);
                        if (entity instanceof CustomEntity npc) {
                            ConstructionTask task = npc.getConstructionTask();
                            if (task != null) {
                                // 获取NPC名称
                                String npcName = npc.getFullName();
                                if (npcName == null || npcName.isEmpty()) {
                                    npcName = "Unknown NPC";
                                }

                                // 创建任务信息
                                ConstructionTasksResponsePacket.TaskInfo taskInfo =
                                    new ConstructionTasksResponsePacket.TaskInfo(
                                        task.getBuildingName(),
                                        npcName,
                                        task.getProgress(),
                                        task.isCompleted()
                                    );
                                taskInfos.add(taskInfo);
                            }
                        }
                    }

                    // 发送响应包
                    ConstructionTasksResponsePacket responsePacket =
                        new ConstructionTasksResponsePacket(taskInfos);
                    NetworkManager.sendToPlayer(responsePacket, player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
