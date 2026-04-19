package com.xiaoliang.simukraft.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class SimukraftMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        System.out.println("[Simukraft] Minecraft 客户端初始化完成 - Mixin 成功载入！");
        System.out.println("[Simukraft] Simukraft Mixin 系统已激活，准备就绪！");
    }
}
