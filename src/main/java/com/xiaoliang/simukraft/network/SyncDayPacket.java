package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientSimukraftData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDayPacket {
    private final int currentDay;

    public SyncDayPacket(int currentDay) {
        this.currentDay = currentDay;
    }

    public SyncDayPacket(FriendlyByteBuf buf) {
        this.currentDay = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(currentDay);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientSimukraftData.setCurrentDay(currentDay);
        });
        ctx.get().setPacketHandled(true);
    }
}