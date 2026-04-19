package com.xiaoliang.simukraft.world;

import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * 统一处理官员邀请接受/拒绝逻辑，供数据包与命令共用。
 */
public final class OfficialInvitationService {

    private OfficialInvitationService() {
    }

    public static void handleResponse(ServerPlayer player, UUID invitationId, boolean accepted) {
        OfficialInvitationManager invitationManager = OfficialInvitationManager.getInstance();
        OfficialInvitationManager.OfficialInvitation invitation = invitationManager.getInvitation(invitationId);

        if (invitation == null) {
            player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.invitation_expired")
                            .withStyle(ChatFormatting.RED)),
                    false
            );
            return;
        }

        if (!invitation.getTargetPlayerId().equals(player.getUUID())) {
            player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.invitation_not_for_you")
                            .withStyle(ChatFormatting.RED)),
                    false
            );
            return;
        }

        if (accepted) {
            handleAccept(player, invitation, invitationManager);
        } else {
            handleReject(player, invitation, invitationManager);
        }
    }

    private static void handleAccept(ServerPlayer player,
                                     OfficialInvitationManager.OfficialInvitation invitation,
                                     OfficialInvitationManager invitationManager) {
        ServerLevel level = player.serverLevel();
        CityData cityData = CityData.get(level);
        CityData.CityInfo city = cityData.getCity(invitation.getCityId());

        if (city == null) {
            player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.city_not_found")
                            .withStyle(ChatFormatting.RED)),
                    false
            );
            invitationManager.removeInvitation(invitation.getInvitationId());
            return;
        }

        String playerName = player.getName().getString();
        if (city.isOfficial(playerName)) {
            player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.already_official")
                            .withStyle(ChatFormatting.YELLOW)),
                    false
            );
            invitationManager.removeInvitation(invitation.getInvitationId());
            return;
        }

        boolean success = cityData.addOfficialToCity(city.getCityId(), playerName, player.getUUID(), level);
        if (!success) {
            player.displayClientMessage(
                    Objects.requireNonNull(Component.translatable("message.city_official.add_failed")
                            .withStyle(ChatFormatting.RED)),
                    false
            );
            return;
        }

        invitationManager.removeInvitation(invitation.getInvitationId());

        player.displayClientMessage(
                Objects.requireNonNull(Component.translatable("message.city_official.invitation_accepted", city.getCityName())
                        .withStyle(ChatFormatting.GREEN)),
                false
        );

        NetworkManager.sendHUDDataToPlayer(
                SimukraftWorldData.get(level).getCurrentDay(),
                PopulationData.get(level).getPopulation(),
                city.getCityName(),
                city.getFunds(),
                city.getCitizenIds().size(),
                player
        );

        // 通知城市群组：玩家接受了官员邀请（城市事件）
        com.xiaoliang.simukraft.utils.CityMessageUtils.sendToCityGroup(
                level.getServer(), invitation.getCityId(),
                Objects.requireNonNull(Component.translatable("message.city_official.player_accepted_invitation",
                        player.getName().getString(), city.getCityName())),
                com.xiaoliang.simukraft.notification.MessageCategory.OFFICIAL
        );
    }

    private static void handleReject(ServerPlayer player,
                                     OfficialInvitationManager.OfficialInvitation invitation,
                                     OfficialInvitationManager invitationManager) {
        invitationManager.removeInvitation(invitation.getInvitationId());

        player.displayClientMessage(
                Objects.requireNonNull(Component.translatable("message.city_official.invitation_rejected")
                        .withStyle(ChatFormatting.YELLOW)),
                false
        );

        // 通知城市群组：玩家拒绝了官员邀请（城市事件）
        com.xiaoliang.simukraft.utils.CityMessageUtils.sendToCityGroup(
                player.serverLevel().getServer(), invitation.getCityId(),
                Objects.requireNonNull(Component.translatable("message.city_official.player_rejected_invitation",
                        player.getName().getString())),
                com.xiaoliang.simukraft.notification.MessageCategory.OFFICIAL
        );
    }
}
