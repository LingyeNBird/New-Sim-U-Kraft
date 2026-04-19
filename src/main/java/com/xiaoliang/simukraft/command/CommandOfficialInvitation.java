package com.xiaoliang.simukraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xiaoliang.simukraft.world.OfficialInvitationService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 官员邀请响应命令。
 * 供聊天栏可点击按钮调用，类似 tpa 的接收/拒绝交互。
 */
@SuppressWarnings("null")
public final class CommandOfficialInvitation {

    private CommandOfficialInvitation() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("skofficial")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.literal("accept")
                                .then(Commands.argument("invitationId", StringArgumentType.word())
                                        .executes(context -> execute(context.getSource(), true,
                                                StringArgumentType.getString(context, "invitationId")))))
                        .then(Commands.literal("deny")
                                .then(Commands.argument("invitationId", StringArgumentType.word())
                                        .executes(context -> execute(context.getSource(), false,
                                                StringArgumentType.getString(context, "invitationId")))))
        );
    }

    private static int execute(CommandSourceStack source, boolean accepted, String invitationIdString) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            return 0;
        }

        UUID invitationId;
        try {
            invitationId = UUID.fromString(invitationIdString);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("message.simukraft.official.invitation_id_invalid"));
            return 0;
        }

        OfficialInvitationService.handleResponse(player, invitationId, accepted);
        return 1;
    }
}
