package com.xiaoliang.simukraft.mixin.xaero;

import com.mojang.logging.LogUtils;
import com.xiaoliang.simukraft.integration.xaero.XaeroWorldMapIntegration;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
@Pseudo
@Mixin(value = WorldMapSession.class, remap = false)
public class MixinWorldMapSession {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private MapProcessor mapProcessor;

    @Inject(
        method = "init",
        at = @At("RETURN"),
        remap = false,
        require = 0
    )
    private void simukraft$onSessionInit(ClientPacketListener connection, long biomeZoomSeed, CallbackInfo ci) {
        try {
            if (this.mapProcessor == null) return;
            xaero.map.highlight.HighlighterRegistry registry = this.mapProcessor.getHighlighterRegistry();
            if (registry == null) return;
            java.lang.reflect.Field field = xaero.map.highlight.HighlighterRegistry.class.getDeclaredField("highlighters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<xaero.map.highlight.AbstractHighlighter> current =
                    (java.util.List<xaero.map.highlight.AbstractHighlighter>) field.get(registry);
            java.util.List<xaero.map.highlight.AbstractHighlighter> mutable = new java.util.ArrayList<>(current);
            XaeroWorldMapIntegration.onSessionInit(mutable);
            field.set(registry, java.util.Collections.unmodifiableList(mutable));
        } catch (Throwable t) {
            LOGGER.error("Simukraft: MixinWorldMapSession injection failed", t);
        }
    }
}
