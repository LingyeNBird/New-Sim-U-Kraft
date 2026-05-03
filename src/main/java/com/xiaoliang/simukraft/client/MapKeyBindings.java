package com.xiaoliang.simukraft.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * 地图相关快捷键绑定。
 * 注册在 MOD 事件总线上，由 Forge 自动调用 {@link RegisterKeyMappingsEvent}。
 */
@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class MapKeyBindings {

    private static final String CATEGORY = "key.categories.simukraft";

    @Nonnull
    private static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.simukraft.open_map",
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
    }

    /**
     * 获取打开地图的快捷键映射。
     */
    public static KeyMapping getOpenMapKey() {
        return OPEN_MAP;
    }
}
