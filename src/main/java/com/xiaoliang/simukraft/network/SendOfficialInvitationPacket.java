package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.notification.MessageCategory;
import com.xiaoliang.simukraft.notification.MessageNotification;
import com.xiaoliang.simukraft.notification.NotificationServiceManager;
import com.xiaoliang.simukraft.utils.CityMessageUtils;
import com.xiaoliang.simukraft.world.CityData;
import com.xiaoliang.simukraft.world.OfficialInvitationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 发送官员邀请数据包
 * 市长通过开拓助手向玩家发送官员邀请
 */
public class SendOfficialInvitationPacket {

    private final BlockPos cityCorePos;
    private final String targetPlayerName;

    public SendOfficialInvitationPacket(BlockPos cityCorePos, String targetPlayerName) {
        this.cityCorePos = cityCorePos;
        this.targetPlayerName = targetPlayerName;
    }

    public SendOfficialInvitationPacket(FriendlyByteBuf buf) {
        this.cityCorePos = Objects.requireNonNull(buf.readBlockPos());
        this.targetPlayerName = Objects.requireNonNull(buf.readUtf(32767));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(cityCorePos));
        buf.writeUtf(Objects.requireNonNull(targetPlayerName));
    }

    public static SendOfficialInvitationPacket decode(FriendlyByteBuf buf) {
        return new SendOfficialInvitationPacket(buf);
    }

    public static void handle(SendOfficialInvitationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            CityData cityData = CityData.get(level);
            CityData.CityInfo city = cityData.getCityByCorePos(packet.cityCorePos);
            String safeTargetPlayerName = Objects.requireNonNull(packet.targetPlayerName);

            if (city == null) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.city_not_found")
                        .withStyle(ChatFormatting.RED)),
                    false
                );
                return;
            }

            // 检查发送者是否是市长
            if (!city.isMayor(player.getName().getString())) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.not_mayor")
                        .withStyle(ChatFormatting.RED)),
                    false
                );
                return;
            }

            // 检查目标玩家是否在线
            ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayerByName(safeTargetPlayerName);
            if (targetPlayer == null) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.player_not_found")
                        .withStyle(ChatFormatting.RED)),
                    false
                );
                return;
            }

            // 检查目标玩家是否已经是官员
            if (city.isOfficial(safeTargetPlayerName)) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.already_official")
                        .withStyle(ChatFormatting.YELLOW)),
                    false
                );
                return;
            }

            // 检查目标玩家是否是市长
            if (city.isMayor(safeTargetPlayerName)) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.cannot_add_mayor")
                        .withStyle(ChatFormatting.RED)),
                    false
                );
                return;
            }

            // 检查目标玩家是否已有城市
            if (cityData.hasCity(safeTargetPlayerName)) {
                player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.target_has_city", safeTargetPlayerName)
                        .withStyle(ChatFormatting.RED)),
                    false
                );
                return;
            }

            // 创建邀请
            OfficialInvitationManager invitationManager = OfficialInvitationManager.getInstance();
            UUID invitationId = invitationManager.createInvitation(
                city.getCityId(),
                city.getCityName(),
                player.getUUID(),
                player.getName().getString(),
                targetPlayer.getUUID(),
                Objects.requireNonNull(targetPlayer.getName().getString())
            );

            // 通过开拓助手发送带按钮的邀请消息
            sendInvitationMessage(level, invitationId, city, player, targetPlayer);

            // 通知市长
            player.displayClientMessage(
                Objects.requireNonNull(Component.translatable("message.city_official.invitation_sent", targetPlayer.getDisplayName())
                    .withStyle(ChatFormatting.GREEN)),
                false
            );

            Simukraft.LOGGER.info("[SendOfficialInvitationPacket] Official invitation sent: {} from {} to {}",
                              invitationId, player.getName().getString(), targetPlayer.getName().getString());
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 通过通知服务发送带 action 按钮的邀请消息
     */
    private static void sendInvitationMessage(ServerLevel level, UUID invitationId,
                                               CityData.CityInfo city, ServerPlayer mayor,
                                               ServerPlayer targetPlayer) {
        net.minecraft.network.chat.Component contentComp = net.minecraft.network.chat.Component.translatable(
                "message.simukraft.official.invitation_content",
                mayor.getDisplayName(), city.getCityName());
        net.minecraft.network.chat.Component titleComp = net.minecraft.network.chat.Component.translatable("notify.title.official_invitation");
        net.minecraft.network.chat.Component senderComp = net.minecraft.network.chat.Component.translatable("notify.title.pioneer_helper");

        // 构建带 action 按钮的通知（String 字段用于 CityChat 兼容，Component 字段用于客户端翻译）
        MessageNotification notification = new MessageNotification(
                senderComp.getString(), "OFFICIAL_INVITATION",
                titleComp.getString(), contentComp.getString(),
                targetPlayer.getUUID(), MessageCategory.OFFICIAL
        );
        notification.setContentComponent(contentComp);
        notification.setTitleComponent(titleComp);
        notification.setCategoryDisplayComponent(
                net.minecraft.network.chat.Component.translatable(
                        Objects.requireNonNull(MessageCategory.OFFICIAL.getTranslationKey())
                )
        );
        notification.setRelatedEntityId(city.getCityId());
        notification.setRelatedEntityType("CITY");
        notification.setCityName(city.getCityName());
        notification.setCategoryDisplayName(MessageCategory.OFFICIAL.getDisplayName());

        // 添加可点击的 action 按钮
        String acceptLabel = Objects.requireNonNull(
                net.minecraft.network.chat.Component.translatable("message.simukraft.official.invitation_accept").getString()
        );
        String denyLabel = Objects.requireNonNull(
                net.minecraft.network.chat.Component.translatable("message.simukraft.official.invitation_deny").getString()
        );
        notification.addAction(acceptLabel, "skofficial accept " + invitationId, 0x55FF55);
        notification.addAction(denyLabel, "skofficial deny " + invitationId, 0xFF5555);

        NotificationServiceManager.getService().sendNotification(notification);

        // 同时通知城市群组（不带 action，仅记录）
        net.minecraft.network.chat.Component groupMsg = net.minecraft.network.chat.Component.translatable(
                "message.simukraft.official.invitation_sent",
                mayor.getDisplayName(), targetPlayer.getDisplayName());
        CityMessageUtils.sendToCityGroup(
                level.getServer(), city.getCityId(),
                groupMsg,
                MessageCategory.OFFICIAL);

        Simukraft.LOGGER.info("[SendOfficialInvitationPacket] Invitation sent to {} via notification service with action buttons",
                targetPlayer.getName().getString());
    }
}
