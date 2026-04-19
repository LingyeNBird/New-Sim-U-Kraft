package com.xiaoliang.simukraft.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

public class MessageUtils {
    public static void sendModeSelectedMessages(ServerPlayer player) {
        sendModeMessages(player, player.getServer());
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendModeSelectedMessagesClient() {
        Player player = ClientRuntimeBridge.getLocalPlayer();
        if (player != null) {
            sendModeMessages(player, ClientRuntimeBridge.getSingleplayerServer());
        }
    }

    private static void sendModeMessages(Player player, MinecraftServer server) {
        if (server == null) return;

        // 只发送欢迎消息，移除模式相关消息
        Component welcomeMsg = styledMessage("message.simukraft.welcome", 0xFFFFFF);
        player.sendSystemMessage(Objects.requireNonNull(welcomeMsg));
        player.sendSystemMessage(Objects.requireNonNull(styledMessage("message.simukraft.welcome.divider", 0x7F7F7F)));
        player.sendSystemMessage(Objects.requireNonNull(styledMessage("message.simukraft.welcome.refactor_edition", 0xA8D8FF)));
    }

    private static Component styledMessage(String key, int color) {
        Style style = Objects.requireNonNull(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        return Objects.requireNonNull(Component.translatable(Objects.requireNonNull(key)).setStyle(style));
    }
}
