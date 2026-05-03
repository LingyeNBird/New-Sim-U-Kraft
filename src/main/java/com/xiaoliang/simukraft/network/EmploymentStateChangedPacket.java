package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.employment.client.WorkBlockHireClientCache;
import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EmploymentStateChangedPacket {
    private final EmploymentAssignment assignment;

    public EmploymentStateChangedPacket(EmploymentAssignment assignment) {
        this.assignment = assignment;
    }

    public EmploymentStateChangedPacket(FriendlyByteBuf buf) {
        this.assignment = EmploymentPacketCodec.readAssignment(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        EmploymentPacketCodec.writeAssignment(buf, assignment);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::applyClient));
        ctx.get().setPacketHandled(true);
    }

    private void applyClient() {
        if (assignment.isAssigned()) {
            WorkBlockHireClientCache.upsert(assignment);
        } else {
            WorkBlockHireClientCache.remove(assignment);
        }
    }
}

