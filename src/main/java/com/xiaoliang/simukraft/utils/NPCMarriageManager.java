package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.NPCFamilyInfoResponsePacket;
import com.xiaoliang.simukraft.entity.Gender;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class NPCMarriageManager {
    private static final double RANDOM_MARRIAGE_CHANCE = 0.2D;
    private static final double RANDOM_MARRIAGE_RADIUS = 10.0D;
    private static final int MIN_MARRIAGE_AGE = 18;

    private NPCMarriageManager() {
    }

    public record MarriagePair(CustomEntity maleNpc, CustomEntity femaleNpc) {
    }

    @Nullable
    public static MarriagePair tryRandomMarriage(MinecraftServer server) {
        if (server == null) {
            return null;
        }

        List<CustomEntity> loadedNpcs = new ArrayList<>(NPCTaskScheduler.getAllNPCs(server));
        if (loadedNpcs.size() < 2) {
            return null;
        }

        Map<UUID, UUID> spouseMap = NPCDataManager.getAllNPCSpouseUuids(server);
        List<MarriagePair> candidatePairs = new ArrayList<>();

        for (CustomEntity maleNpc : loadedNpcs) {
            if (!isMarriageCandidate(server, maleNpc, spouseMap)) {
                continue;
            }
            if (maleNpc.getGender() != Gender.MALE) {
                continue;
            }

            for (CustomEntity femaleNpc : loadedNpcs) {
                if (maleNpc == femaleNpc || !isMarriageCandidate(server, femaleNpc, spouseMap)) {
                    continue;
                }
                if (femaleNpc.getGender() != Gender.FEMALE) {
                    continue;
                }
                if (!isSameCity(maleNpc, femaleNpc)) {
                    continue;
                }
                if (maleNpc.distanceToSqr(femaleNpc) > RANDOM_MARRIAGE_RADIUS * RANDOM_MARRIAGE_RADIUS) {
                    continue;
                }
                candidatePairs.add(new MarriagePair(maleNpc, femaleNpc));
            }
        }

        if (candidatePairs.isEmpty()) {
            return null;
        }

        Collections.shuffle(candidatePairs);
        MarriagePair selectedPair = candidatePairs.get(0);
        if (selectedPair.maleNpc().getRandom().nextDouble() >= RANDOM_MARRIAGE_CHANCE) {
            return null;
        }
        return marry(server, selectedPair.maleNpc(), selectedPair.femaleNpc()) ? selectedPair : null;
    }

    @Nullable
    public static MarriagePair forceMarriageNearPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return null;
        }

        MinecraftServer server = player.getServer();
        AABB searchBox = Objects.requireNonNull(player.getBoundingBox().inflate(RANDOM_MARRIAGE_RADIUS));
        List<CustomEntity> nearbyNpcs = new ArrayList<>(
                player.serverLevel().getEntitiesOfClass(CustomEntity.class, searchBox)
        );
        nearbyNpcs.sort(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)));

        Map<UUID, UUID> spouseMap = NPCDataManager.getAllNPCSpouseUuids(server);
        for (CustomEntity maleNpc : nearbyNpcs) {
            if (!isMarriageCandidate(server, maleNpc, spouseMap) || maleNpc.getGender() != Gender.MALE) {
                continue;
            }
            for (CustomEntity femaleNpc : nearbyNpcs) {
                if (maleNpc == femaleNpc || !isMarriageCandidate(server, femaleNpc, spouseMap)) {
                    continue;
                }
                if (femaleNpc.getGender() != Gender.FEMALE) {
                    continue;
                }
                return marry(server, maleNpc, femaleNpc) ? new MarriagePair(maleNpc, femaleNpc) : null;
            }
        }
        return null;
    }

    private static boolean marry(MinecraftServer server, CustomEntity maleNpc, CustomEntity femaleNpc) {
        if (server == null || maleNpc == null || femaleNpc == null) {
            return false;
        }
        if (!NPCDataManager.setNPCSpouses(server, maleNpc.getUUID(), femaleNpc.getUUID())) {
            return false;
        }

        try {
            String maleSpouseName = femaleNpc.getFullName();
            String femaleSpouseName = maleNpc.getFullName();
            server.getPlayerList().getPlayers().forEach(p -> {
                NetworkManager.sendToPlayer(
                        new NPCFamilyInfoResponsePacket(maleNpc.getUUID(), maleSpouseName, ""),
                        p
                );
                NetworkManager.sendToPlayer(
                        new NPCFamilyInfoResponsePacket(femaleNpc.getUUID(), femaleSpouseName, ""),
                        p
                );
            });
        } catch (Exception ignored) {
        }

        broadcastMarriage(server, maleNpc, femaleNpc);
        Simukraft.LOGGER.info("[NPCMarriageManager] NPC {} 与 {} 结婚成功", maleNpc.getFullName(), femaleNpc.getFullName());
        return true;
    }

    private static void broadcastMarriage(MinecraftServer server, CustomEntity maleNpc, CustomEntity femaleNpc) {
        Component message = Objects.requireNonNull(Component.literal(
                maleNpc.getFullName() + "已经和" + femaleNpc.getFullName() + "结为夫妻,祝他们百年好合!"
        ));
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    private static boolean isMarriageCandidate(MinecraftServer server, CustomEntity npc, Map<UUID, UUID> spouseMap) {
        if (server == null || npc == null || !npc.isAlive() || npc.isRemoved()) {
            return false;
        }
        if (spouseMap.containsKey(npc.getUUID())) {
            return false;
        }
        return NPCDataManager.getNPCAge(server, npc.getUUID()) >= MIN_MARRIAGE_AGE;
    }

    private static boolean isSameCity(CustomEntity firstNpc, CustomEntity secondNpc) {
        UUID firstCityId = firstNpc.getCityId();
        UUID secondCityId = secondNpc.getCityId();
        return firstCityId != null && Objects.equals(firstCityId, secondCityId);
    }
}
