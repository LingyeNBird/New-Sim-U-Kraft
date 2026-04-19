package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class DeleteCityPacket {
    private final BlockPos cityCorePos;
    
    public DeleteCityPacket(BlockPos cityCorePos) {
        this.cityCorePos = cityCorePos;
    }
    
    public DeleteCityPacket(FriendlyByteBuf buf) {
        this.cityCorePos = buf.readBlockPos();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(cityCorePos);
    }
    
    public static void handle(DeleteCityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            MinecraftServer server = player.getServer();
            if (server != null) {
                try {
                    ServerLevel level = server.overworld();

                    // 获取城市数据
                    com.xiaoliang.simukraft.world.CityData cityData = com.xiaoliang.simukraft.world.CityData.get(level);

                    // 查找对应的城市
                    com.xiaoliang.simukraft.world.CityData.CityInfo cityToDelete = null;
                    for (com.xiaoliang.simukraft.world.CityData.CityInfo city : cityData.getAllCities()) {
                        if (city.getCityCorePos().equals(packet.cityCorePos)) {
                            cityToDelete = city;
                            break;
                        }
                    }

                    if (cityToDelete != null) {
                        // 检查权限：只有市长可以删除城市，官员不能删除城市
                        if (!cityToDelete.isMayor(player.getUUID())) {
                            player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.simukraft.city.delete.no_permission")
                                    .withStyle(net.minecraft.ChatFormatting.RED),
                                false
                            );
                            return;
                        }
                        
                        UUID cityId = cityToDelete.getCityId();
                        String cityName = cityToDelete.getCityName();
                        List<UUID> citizenIds = new ArrayList<>(cityToDelete.getCitizenIds());

                        // 删除城市前获取区块数据用于集成
                        com.xiaoliang.simukraft.world.CityChunkData cityChunkData =
                                com.xiaoliang.simukraft.world.CityChunkData.get(level);
                        java.util.Set<Long> chunksToRelease =
                                new java.util.HashSet<>(cityChunkData.getCityChunks(cityId));

                        // 1. 删除城市中的所有NPC
                        for (UUID npcUuid : citizenIds) {
                            deleteNPC(server, level, npcUuid);
                        }

                        // 2. 从城市数据中删除城市
                        cityData.deleteCity(cityId, level);

                        // 调用模组集成：释放区块并广播更新
                        com.xiaoliang.simukraft.integration.IntegrationBridge.onCityChunksUnclaimed(
                                server, cityId, chunksToRelease);

                        // 3. 更新世界人口（减去城市人口）
                        com.xiaoliang.simukraft.world.PopulationData populationData = com.xiaoliang.simukraft.world.PopulationData.get(level);
                        int currentPopulation = populationData.getPopulation();
                        int newPopulation = Math.max(0, currentPopulation - citizenIds.size());
                        populationData.setPopulation(newPopulation, level);

                        // 4. 发送同步包给所有玩家，恢复HUD为无城市状态
                        syncCityDeletionToClients(server, level, citizenIds);
                    }
                } catch (Exception e) {
                    Simukraft.LOGGER.error("[DeleteCityPacket] Failed to delete city: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void deleteNPC(MinecraftServer server, ServerLevel level, UUID npcUuid) {
        try {
            // 删除NPC数据文件
            deleteNPCFromDataFile(server, npcUuid);

            // 删除职业数据
            deleteNPCFromJobFile(server, npcUuid);

            // 删除名称管理器中的记录
            deleteNPCFromNameManager(server, npcUuid);

            // 删除住宅信息
            deleteNPCFromResidenceFiles(server, npcUuid);

            // 删除内存缓存
            com.xiaoliang.simukraft.utils.NPCDataManager.removeNPCFromCache(npcUuid);

            // 删除实体NPC
            deleteNPCEntity(server, npcUuid);

        } catch (Exception e) {
            Simukraft.LOGGER.error("[DeleteCityPacket] Failed to delete NPC: {} - {}", npcUuid, e.getMessage());
        }
    }
    
    private static void deleteNPCFromDataFile(MinecraftServer server, UUID npcUuid) {
        try {
            java.nio.file.Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            java.nio.file.Path npcDir = worldDir.resolve(com.xiaoliang.simukraft.utils.FileUtils.MODE_DIR).resolve("npc");
            java.nio.file.Path dataFile = npcDir.resolve("npcdata.sk");
            
            if (!java.nio.file.Files.exists(dataFile)) {
                return;
            }
            
            String content = new String(java.nio.file.Files.readAllBytes(dataFile), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonArray npcArray = com.google.gson.JsonParser.parseString(content).getAsJsonArray();
            com.google.gson.JsonArray newArray = new com.google.gson.JsonArray();
            
            for (com.google.gson.JsonElement element : npcArray) {
                com.google.gson.JsonObject npcObj = element.getAsJsonObject();
                if (!UUID.fromString(npcObj.get("uuid").getAsString()).equals(npcUuid)) {
                    newArray.add(npcObj);
                }
            }
            
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.write(dataFile, gson.toJson(newArray).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DeleteCityPacket] Failed to delete NPC from npcdata.sk: {}", e.getMessage());
        }
    }

    private static void deleteNPCFromJobFile(MinecraftServer server, UUID npcUuid) {
        try {
            java.nio.file.Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            java.nio.file.Path npcDir = worldDir.resolve(com.xiaoliang.simukraft.utils.FileUtils.MODE_DIR).resolve("npc");
            java.nio.file.Path jobFile = npcDir.resolve("jobdata.sk");

            if (!java.nio.file.Files.exists(jobFile)) {
                return;
            }

            String content = new String(java.nio.file.Files.readAllBytes(jobFile), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonArray jobArray = com.google.gson.JsonParser.parseString(content).getAsJsonArray();
            com.google.gson.JsonArray newArray = new com.google.gson.JsonArray();

            for (com.google.gson.JsonElement element : jobArray) {
                com.google.gson.JsonObject jobObj = element.getAsJsonObject();
                if (!jobObj.get("npc_uuid").getAsString().equals(npcUuid.toString())) {
                    newArray.add(jobObj);
                }
            }

            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.write(jobFile, gson.toJson(newArray).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DeleteCityPacket] Failed to delete NPC from jobdata.sk: {}", e.getMessage());
        }
    }
    
    private static void deleteNPCFromNameManager(MinecraftServer server, UUID npcUuid) {
        try {
            java.nio.file.Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            java.nio.file.Path nameFile = worldDir.resolve(com.xiaoliang.simukraft.utils.FileUtils.MODE_DIR).resolve("simukraft_names.dat");

            if (!java.nio.file.Files.exists(nameFile)) {
                return;
            }

            String content = new String(java.nio.file.Files.readAllBytes(nameFile), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject nameData = com.google.gson.JsonParser.parseString(content).getAsJsonObject();

            if (nameData.has("npc_names")) {
                com.google.gson.JsonObject npcNames = nameData.getAsJsonObject("npc_names");
                npcNames.remove(npcUuid.toString());

                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                java.nio.file.Files.write(nameFile, gson.toJson(nameData).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DeleteCityPacket] Failed to delete NPC from simukraft_names.dat: {}", e.getMessage());
        }
    }
    
    private static void deleteNPCFromResidenceFiles(MinecraftServer server, UUID npcUuid) {
        try {
            java.nio.file.Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            java.nio.file.Path residenceDir = worldDir.resolve("simukraft").resolve("residence");
            
            if (!java.nio.file.Files.exists(residenceDir)) {
                return;
            }
            
            java.nio.file.Files.list(residenceDir)
                .filter(path -> path.toString().endsWith(".sk"))
                .forEach(file -> {
                    try {
                        List<String> lines = java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
                        boolean modified = false;
                        List<String> newLines = new ArrayList<>();
                        
                        for (String line : lines) {
                            if (line.trim().startsWith("resident_uuid:") && line.contains(npcUuid.toString())) {
                                modified = true;
                                continue;
                            }
                            if (line.trim().startsWith("resident:")) {
                                int currentIndex = lines.indexOf(line);
                                if (currentIndex + 1 < lines.size() && 
                                    lines.get(currentIndex + 1).trim().startsWith("resident_uuid:") && 
                                    lines.get(currentIndex + 1).contains(npcUuid.toString())) {
                                    continue;
                                }
                            }
                            newLines.add(line);
                        }
                        
                        if (modified) {
                            try (java.io.Writer writer = java.nio.file.Files.newBufferedWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                                for (String line : newLines) {
                                    writer.write(line);
                                    writer.write("\n");
                                }
                            }
                        }
                    } catch (Exception e) {
                        Simukraft.LOGGER.error("[DeleteCityPacket] Failed to process residence file: {} - {}", file, e.getMessage());
                    }
                });
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DeleteCityPacket] Failed to traverse residence directory: {}", e.getMessage());
        }
    }
    
    private static void deleteNPCEntity(MinecraftServer server, UUID npcUuid) {
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(npcUuid);
            if (entity != null) {
                entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                Simukraft.LOGGER.info("[DeleteCityPacket] Removed NPC entity: {}", npcUuid);
                return;
            }
        }
    }
    
    private static void syncCityDeletionToClients(MinecraftServer server, ServerLevel level, List<UUID> deletedNPCs) {
        // 发送NPC删除同步包
        for (UUID npcUuid : deletedNPCs) {
            SyncNPCDeletePacket syncPacket = new SyncNPCDeletePacket(npcUuid);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(syncPacket, player);
            }
        }
        
        // 发送HUD重置包给所有玩家（恢复为无城市状态）
        com.xiaoliang.simukraft.world.SimukraftWorldData worldData = com.xiaoliang.simukraft.world.SimukraftWorldData.get(level);
        int currentDay = worldData.getCurrentDay();
        com.xiaoliang.simukraft.world.PopulationData populationData = com.xiaoliang.simukraft.world.PopulationData.get(level);
        int worldPopulation = populationData.getPopulation();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 发送无城市状态的HUD数据（城市名称为空）
            NetworkManager.sendHUDDataToPlayer(
                currentDay,
                worldPopulation,
                "",  // 空城市名称表示无城市
                0.0, // 无城市资金
                0,   // 无城市人口
                player
            );
        }
        
        Simukraft.LOGGER.info("[DeleteCityPacket] Sent HUD reset packets to all players");
    }
    
    public BlockPos getCityCorePos() {
        return cityCorePos;
    }
}