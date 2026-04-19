package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.building.ControlBoxDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 配方选择数据包
 * 客户端选择配方后发送到服务器
 */
@SuppressWarnings({"null", "unused"})
public class SelectRecipePacket {
    private final BlockPos pos;
    private final String recipeId;
    private final String buildingFileName;

    public SelectRecipePacket(BlockPos pos, String recipeId, String buildingFileName) {
        this.pos = pos;
        this.recipeId = recipeId;
        this.buildingFileName = buildingFileName;
    }

    public SelectRecipePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.recipeId = buf.readUtf(32767);
        this.buildingFileName = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(recipeId);
        buf.writeUtf(buildingFileName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            // 保存配方选择到控制盒数据文件
            ControlBoxDataManager.updateSelectedRecipe(server, pos, recipeId);

            Simukraft.LOGGER.info("[SelectRecipePacket] 玩家 {} 为建筑 {} 选择了配方: {}", 
                player.getName().getString(), buildingFileName, recipeId);
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getBuildingFileName() {
        return buildingFileName;
    }
}
