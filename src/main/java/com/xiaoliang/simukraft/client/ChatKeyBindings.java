package com.xiaoliang.simukraft.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ChatKeyBindings {
    private static final String CATEGORY = "key.categories.simukraft";
    
    private static final KeyMapping OPEN_CHAT = new KeyMapping(
            "key.simukraft.open_chat",
            GLFW.GLFW_KEY_N,
            CATEGORY
    );
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(Objects.requireNonNull(OPEN_CHAT));
    }
    
    public static KeyMapping getOpenChatKey() {
        return OPEN_CHAT;
    }
}
