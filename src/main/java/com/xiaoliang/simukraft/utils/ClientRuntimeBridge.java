package com.xiaoliang.simukraft.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

/**
 * 通过反射访问客户端运行时，避免公共类在服务端加载时解析客户端专属类型。
 */
public final class ClientRuntimeBridge {
    private static final String CLIENT_IMPL_CLASS = "com.xiaoliang.simukraft.client.ClientRuntimeBridgeImpl";

    private ClientRuntimeBridge() {
    }

    public static void openScreen(String screenClassName, Class<?>[] constructorTypes, Object... constructorArgs)
            throws ReflectiveOperationException {
        if (!isClient()) {
            return;
        }
        try {
            Class<?> impl = Class.forName(CLIENT_IMPL_CLASS);
            impl.getMethod("openScreen", String.class, Class[].class, Object[].class)
                    .invoke(null, screenClassName, constructorTypes, constructorArgs);
        } catch (InvocationTargetException e) {
            rethrowAsReflectiveOperationException(e);
        }
    }

    @Nullable
    public static MinecraftServer getSingleplayerServer() {
        if (!isClient()) {
            return null;
        }
        try {
            Class<?> impl = Class.forName(CLIENT_IMPL_CLASS);
            Object server = impl.getMethod("getSingleplayerServer").invoke(null);
            return server instanceof MinecraftServer minecraftServer ? minecraftServer : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    public static Player getLocalPlayer() {
        if (!isClient()) {
            return null;
        }
        try {
            Class<?> impl = Class.forName(CLIENT_IMPL_CLASS);
            Object player = impl.getMethod("getLocalPlayer").invoke(null);
            return player instanceof Player localPlayer ? localPlayer : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    private static void rethrowAsReflectiveOperationException(InvocationTargetException e) throws ReflectiveOperationException {
        Throwable cause = e.getCause();
        if (cause instanceof ReflectiveOperationException reflectiveOperationException) {
            throw reflectiveOperationException;
        }
        ReflectiveOperationException wrapped = new ReflectiveOperationException(cause != null ? cause.getMessage() : e.getMessage());
        if (cause != null) {
            wrapped.initCause(cause);
        }
        throw wrapped;
    }
}
