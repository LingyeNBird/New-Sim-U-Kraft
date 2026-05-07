package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientToastHUDOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class ShowCityNamePacket {
    private final String cityName;

    public ShowCityNamePacket(String cityName) {
        this.cityName = Objects.requireNonNull(cityName);
    }

    public static void encode(ShowCityNamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.cityName);
    }

    public static ShowCityNamePacket decode(FriendlyByteBuf buffer) {
        return new ShowCityNamePacket(buffer.readUtf());
    }

    public static void handle(ShowCityNamePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ClientToastHUDOverlay.showCityName(packet.cityName, mc.player.getUUID());
            }
        });
        context.get().setPacketHandled(true);
    }
}
