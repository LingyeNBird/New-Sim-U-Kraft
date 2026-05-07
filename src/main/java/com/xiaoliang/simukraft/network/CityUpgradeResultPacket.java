package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.CityUpgradeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 城市升级结果回包。
 * 成功时用于关闭界面并提示，失败时用于在当前界面展示明确原因。
 */
@SuppressWarnings("null")
public class CityUpgradeResultPacket {
    private final boolean success;
    private final String resultCode;
    private final int targetLevel;

    public CityUpgradeResultPacket(boolean success, String resultCode, int targetLevel) {
        this.success = success;
        this.resultCode = Objects.requireNonNull(resultCode);
        this.targetLevel = targetLevel;
    }

    public static void encode(CityUpgradeResultPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeUtf(packet.resultCode);
        buf.writeInt(packet.targetLevel);
    }

    public static CityUpgradeResultPacket decode(FriendlyByteBuf buf) {
        return new CityUpgradeResultPacket(buf.readBoolean(), buf.readUtf(), buf.readInt());
    }

    public static void handle(CityUpgradeResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Component message = packet.toComponent();

            if (mc.player != null) {
                mc.player.sendSystemMessage(message);
            }

            if (mc.screen instanceof CityUpgradeScreen screen) {
                screen.handleUpgradeResult(packet.success, message);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private Component toComponent() {
        return switch (resultCode) {
            case "started" -> Component.translatable("message.simukraft.city_upgrade.started", targetLevel);
            case "success" -> Component.translatable("message.simukraft.city_upgrade.success", targetLevel);
            case "already_upgrading" -> Component.translatable("message.simukraft.city_upgrade.already_upgrading");
            case "no_permission" -> Component.translatable("message.simukraft.city_upgrade.no_permission");
            case "city_not_found" -> Component.translatable("message.simukraft.city_upgrade.city_not_found");
            case "invalid_target" -> Component.translatable("message.simukraft.city_upgrade.invalid_target");
            case "not_upgradeable" -> Component.translatable("message.simukraft.city_upgrade.not_upgradeable");
            case "insufficient_items" -> Component.translatable("message.simukraft.city_upgrade.insufficient_items");
            case "insufficient_funds" -> Component.translatable("message.simukraft.city_upgrade.insufficient_funds");
            case "insufficient_population" -> Component.translatable("message.simukraft.city_upgrade.insufficient_population");
            default -> Component.translatable("message.simukraft.city_upgrade.failed");
        };
    }
}
