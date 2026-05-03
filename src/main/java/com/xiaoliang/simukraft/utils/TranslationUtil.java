package com.xiaoliang.simukraft.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;


public final class TranslationUtil {

    private TranslationUtil() {}


    public static MutableComponent component(String key, Object... inserts) {
        return Component.translatable(Objects.requireNonNull(key), Objects.requireNonNull(inserts));
    }


    public static String translate(String key, Object... inserts) {
        return component(key, inserts).getString();
    }



    public static MutableComponent gui(String subKey, Object... inserts) {
        return component("gui.simukraft." + subKey, inserts);
    }


    public static MutableComponent message(String subKey, Object... inserts) {
        return component("message.simukraft." + subKey, inserts);
    }


    public static MutableComponent hud(String subKey, Object... inserts) {
        return component("hud.simukraft." + subKey, inserts);
    }


    public static MutableComponent chat(String subKey, Object... inserts) {
        return component("chat.simukraft." + subKey, inserts);
    }
}
