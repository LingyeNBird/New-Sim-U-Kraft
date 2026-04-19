package com.xiaoliang.simukraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端实现类：这里可以直接调用Minecraft客户端API，避免在生产环境通过反射访问被混淆的方法名导致失效。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRuntimeBridgeImpl {
    private ClientRuntimeBridgeImpl() {
    }

    public static void openScreen(String screenClassName, Class<?>[] constructorTypes, Object[] constructorArgs)
            throws ReflectiveOperationException {
        Class<?> screenClass = Class.forName(screenClassName);
        Object screen = screenClass.getConstructor(constructorTypes).newInstance(constructorArgs);
        Minecraft.getInstance().setScreen((Screen) screen);
    }

    public static MinecraftServer getSingleplayerServer() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    public static Player getLocalPlayer() {
        return Minecraft.getInstance().player;
    }
}

