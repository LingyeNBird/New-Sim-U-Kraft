package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.IndustrialControlBoxLDLibScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 配方同步数据包
 * 服务器同步当前配方到客户端
 */
public class SyncRecipePacket {
    private final BlockPos pos;
    private final String recipeId;
    private final boolean multiRecipe;

    public SyncRecipePacket(BlockPos pos, String recipeId, boolean multiRecipe) {
        this.pos = pos;
        this.recipeId = recipeId;
        this.multiRecipe = multiRecipe;
    }

    public SyncRecipePacket(FriendlyByteBuf buf) {
        this.pos = Objects.requireNonNull(buf.readBlockPos());
        this.recipeId = Objects.requireNonNull(buf.readUtf(32767));
        this.multiRecipe = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(pos));
        buf.writeUtf(Objects.requireNonNull(recipeId));
        buf.writeBoolean(multiRecipe);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端处理 - 使用新的LDLib界面
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof IndustrialControlBoxLDLibScreen screen) {
                screen.onRecipeSyncReceived(recipeId, multiRecipe);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public boolean isMultiRecipe() {
        return multiRecipe;
    }
}
