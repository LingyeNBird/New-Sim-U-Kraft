package com.xiaoliang.simukraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xiaoliang.simukraft.network.ShowToastPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class CommandToast {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 注册/w1命令
        dispatcher.register(
                Commands.literal("w1")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                // 发送数据包到客户端，显示toast
                                ShowToastPacket.sendToPlayer(player, "w1");
                                return 1;
                            }
                            return 0;
                        })
        );
        
        // 注册/w2命令
        dispatcher.register(
                Commands.literal("w2")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                // 发送数据包到客户端，显示toast
                                ShowToastPacket.sendToPlayer(player, "w2");
                                return 1;
                            }
                            return 0;
                        })
        );
        
        // 注册/g1命令
        dispatcher.register(
                Commands.literal("g1")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                // 发送数据包到客户端，显示toast
                                ShowToastPacket.sendToPlayer(player, "g1");
                                return 1;
                            }
                            return 0;
                        })
        );
    }
}