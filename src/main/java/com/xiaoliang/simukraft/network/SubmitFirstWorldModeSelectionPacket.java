package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.world.SimukraftWorldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class SubmitFirstWorldModeSelectionPacket {
    private final boolean enableExpertMode;

    public SubmitFirstWorldModeSelectionPacket(boolean enableExpertMode) {
        this.enableExpertMode = enableExpertMode;
    }

    public SubmitFirstWorldModeSelectionPacket(FriendlyByteBuf buf) {
        this.enableExpertMode = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enableExpertMode);
    }

    public static SubmitFirstWorldModeSelectionPacket decode(FriendlyByteBuf buf) {
        return new SubmitFirstWorldModeSelectionPacket(buf);
    }

    public static void handle(SubmitFirstWorldModeSelectionPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.simukraft.first_world_mode.no_permission"));
                return;
            }

            SimukraftWorldData worldData = SimukraftWorldData.get(player.serverLevel());
            if (worldData.isFirstWorldModeSelectionCompleted()) {
                return;
            }

            ServerConfig.ENABLE_EXPERT_MODE.set(message.enableExpertMode);
            ServerConfig.clearCache();
            ServerConfig.SPEC.save();

            worldData.setFirstWorldModeSelectionCompleted(true);

            player.sendSystemMessage(Component.translatable(
                    message.enableExpertMode
                            ? "message.simukraft.first_world_mode.selected_expert"
                            : "message.simukraft.first_world_mode.selected_normal"
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}
