package com.xiaoliang.simukraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class OpenFirstWorldModeSelectionPacket {
    public OpenFirstWorldModeSelectionPacket() {
    }

    public OpenFirstWorldModeSelectionPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenFirstWorldModeSelectionPacket decode(FriendlyByteBuf buf) {
        return new OpenFirstWorldModeSelectionPacket(buf);
    }

    public static void handle(OpenFirstWorldModeSelectionPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClientSide();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientSide() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        try {
            Class<?> guiClass = Class.forName("com.xiaoliang.simukraft.client.gui.FirstWorldModeSelectionScreen");
            Object screen = guiClass.getConstructor().newInstance();
            minecraft.setScreen((net.minecraft.client.gui.screens.Screen) screen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
