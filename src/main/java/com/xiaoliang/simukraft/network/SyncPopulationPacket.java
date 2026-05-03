package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientSimukraftData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPopulationPacket {
    private final int population;

    public SyncPopulationPacket(int population) {
        this.population = population;
    }

    public SyncPopulationPacket(FriendlyByteBuf buf) {
        this.population = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(population);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientSimukraftData.setCurrentPopulation(population);
        });
        ctx.get().setPacketHandled(true);
    }
}