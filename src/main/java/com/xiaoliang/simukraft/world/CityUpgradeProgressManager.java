package com.xiaoliang.simukraft.world;

import com.xiaoliang.simukraft.network.CityUpgradeResultPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.ShowToastPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public final class CityUpgradeProgressManager {
    private static final int BASE_UPGRADE_SECONDS = 5;
    private static final int TICKS_PER_SECOND = 20;
    private static final Map<UUID, PendingUpgrade> PENDING_UPGRADES = new ConcurrentHashMap<>();

    private CityUpgradeProgressManager() {
    }

    public static boolean hasPendingUpgrade(UUID cityId) {
        return PENDING_UPGRADES.containsKey(cityId);
    }

    public static int getUpgradeDurationTicks(int targetLevel) {
        int seconds = Math.max(BASE_UPGRADE_SECONDS, BASE_UPGRADE_SECONDS + Math.max(0, targetLevel - 1));
        return seconds * TICKS_PER_SECOND;
    }

    public static int getUpgradeDurationSeconds(int targetLevel) {
        return getUpgradeDurationTicks(targetLevel) / TICKS_PER_SECOND;
    }

    public static void startUpgrade(ServerPlayer player,
                                    ServerLevel level,
                                    CityData cityData,
                                    BlockPos cityCorePos,
                                    UUID cityId,
                                    int targetLevel,
                                    CityUpgradeManager.Requirements requirements) {
        removeItems(player, requirements);
        CityData.CityInfo cityInfo = cityData.getCity(cityId);
        if (cityInfo != null) {
            cityInfo.setFunds(cityInfo.getFunds() - requirements.funds());
            cityData.setDirty();
        }

        PENDING_UPGRADES.put(cityId, new PendingUpgrade(
                cityId,
                cityCorePos.immutable(),
                targetLevel,
                player.getUUID(),
                level.dimension(),
                level.getGameTime() + getUpgradeDurationTicks(targetLevel)
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_UPGRADES.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, PendingUpgrade>> iterator = PENDING_UPGRADES.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingUpgrade pendingUpgrade = iterator.next().getValue();
            ServerLevel level = server.getLevel(pendingUpgrade.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < pendingUpgrade.finishTick()) {
                continue;
            }
            completeUpgrade(server, level, pendingUpgrade);
            iterator.remove();
        }
    }

    private static void completeUpgrade(MinecraftServer server, ServerLevel level, PendingUpgrade pendingUpgrade) {
        CityData cityData = CityData.get(level);
        CityData.CityInfo cityInfo = cityData.getCityByCorePos(pendingUpgrade.cityCorePos());
        ServerPlayer player = server.getPlayerList().getPlayer(pendingUpgrade.playerId());
        if (cityInfo == null || cityInfo.getCityId() == null || !cityInfo.getCityId().equals(pendingUpgrade.cityId())) {
            if (player != null) {
                NetworkManager.sendToPlayer(new CityUpgradeResultPacket(false, "city_not_found", pendingUpgrade.targetLevel()), player);
            }
            return;
        }

        cityInfo.setCityLevel(pendingUpgrade.targetLevel());
        cityData.setDirty();
        NetworkManager.syncCityHUDData(cityInfo.getCityId(), level);

        if (player != null) {
            String toastType = pendingUpgrade.targetLevel() <= 4 ? "w1" : pendingUpgrade.targetLevel() <= 8 ? "w2" : "g1";
            ShowToastPacket.sendToPlayer(player, toastType, pendingUpgrade.targetLevel());
            NetworkManager.sendToPlayer(new CityUpgradeResultPacket(true, "success", pendingUpgrade.targetLevel()), player);
        }
    }

    private static void removeItems(ServerPlayer player, CityUpgradeManager.Requirements requirements) {
        if (requirements.wood() > 0) {
            removeItems(player, net.minecraft.world.item.Items.OAK_LOG, requirements.wood());
        }
        if (requirements.cobblestone() > 0) {
            removeItems(player, net.minecraft.world.item.Items.COBBLESTONE, requirements.cobblestone());
        }
        if (requirements.ironIngot() > 0) {
            removeItems(player, net.minecraft.world.item.Items.IRON_INGOT, requirements.ironIngot());
        }
        if (requirements.goldIngot() > 0) {
            removeItems(player, net.minecraft.world.item.Items.GOLD_INGOT, requirements.goldIngot());
        }
        if (requirements.diamond() > 0) {
            removeItems(player, net.minecraft.world.item.Items.DIAMOND, requirements.diamond());
        }
        if (requirements.lapisLazuli() > 0) {
            removeItems(player, net.minecraft.world.item.Items.LAPIS_LAZULI, requirements.lapisLazuli());
        }
    }

    private static void removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int toRemove = amount;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item && toRemove > 0) {
                int removed = Math.min(toRemove, stack.getCount());
                stack.shrink(removed);
                toRemove -= removed;
                if (toRemove == 0) {
                    break;
                }
            }
        }
    }

    private record PendingUpgrade(UUID cityId,
                                  BlockPos cityCorePos,
                                  int targetLevel,
                                  UUID playerId,
                                  net.minecraft.resources.ResourceKey<Level> dimension,
                                  long finishTick) {
    }
}
