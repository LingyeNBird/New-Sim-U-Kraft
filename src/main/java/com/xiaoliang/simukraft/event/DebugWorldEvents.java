package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.debug.DebugWorldGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Simukraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DebugWorldEvents {
    private DebugWorldEvents() {
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            DebugWorldGenerator.ensureGenerated(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DebugWorldGenerator.ensureGenerated(player.serverLevel(), player);
        DebugWorldGenerator.ensurePlayerSpawnSafety(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DebugWorldGenerator.ensureGenerated(player.serverLevel(), player);
        DebugWorldGenerator.ensurePlayerSpawnSafety(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DebugWorldGenerator.ensureGenerated(player.serverLevel(), player);
        DebugWorldGenerator.ensurePlayerSpawnSafety(player);
    }
}
