package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class AddOfficialPacket {
    private final BlockPos cityCorePos;
    private final String targetPlayerName;

    public AddOfficialPacket(BlockPos cityCorePos, String targetPlayerName) {
        this.cityCorePos = cityCorePos;
        this.targetPlayerName = targetPlayerName;
    }

    public AddOfficialPacket(FriendlyByteBuf buf) {
        this.cityCorePos = buf.readBlockPos();
        this.targetPlayerName = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(cityCorePos);
        buf.writeUtf(targetPlayerName);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                CityData cityData = CityData.get(level);
                CityData.CityInfo city = cityData.getCityByCorePos(cityCorePos);

                if (city != null) {
                    // 检查发送者是否是市长
                    if (!city.isMayor(player.getName().getString())) {
                        player.displayClientMessage(
                            Component.translatable("message.city_official.not_mayor").withStyle(ChatFormatting.RED),
                            false
                        );
                        return;
                    }

                    // 检查目标玩家是否已经是官员
                    if (city.isOfficial(targetPlayerName)) {
                        player.displayClientMessage(
                            Component.translatable("message.city_official.already_official").withStyle(ChatFormatting.YELLOW),
                            false
                        );
                        return;
                    }

                    // 检查目标玩家是否是市长
                    if (city.isMayor(targetPlayerName)) {
                        player.displayClientMessage(
                            Component.translatable("message.city_official.cannot_add_mayor").withStyle(ChatFormatting.RED),
                            false
                        );
                        return;
                    }

                    // 获取目标玩家的UUID（如果在线）
                    ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayerByName(targetPlayerName);
                    UUID targetPlayerId = targetPlayer != null ? targetPlayer.getUUID() : null;
                    
                    // 添加官员（使用玩家名和UUID）
                    boolean success = cityData.addOfficialToCity(city.getCityId(), targetPlayerName, targetPlayerId, level);
                    if (success) {
                        player.displayClientMessage(
                            Component.translatable("message.city_official.added_success", targetPlayerName).withStyle(ChatFormatting.GREEN),
                            false
                        );

                        // 通知城市群组：新官员任命（城市事件）
                        com.xiaoliang.simukraft.utils.CityMessageUtils.sendToCityGroup(
                            level.getServer(), city.getCityId(),
                            Component.translatable("message.city_official.become_official", city.getCityName()),
                            com.xiaoliang.simukraft.notification.MessageCategory.OFFICIAL
                        );

                        // 刷新官员列表
                        OfficialListRequestPacket refreshPacket = new OfficialListRequestPacket(cityCorePos);
                        refreshPacket.handle(context);
                    } else {
                        player.displayClientMessage(
                            Component.translatable("message.city_official.add_failed").withStyle(ChatFormatting.RED),
                            false
                        );
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }

}
