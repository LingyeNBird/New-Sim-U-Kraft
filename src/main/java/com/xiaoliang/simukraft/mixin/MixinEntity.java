package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity Mixin
 * 注入 Entity 类的 getEyePosition 方法，确保射线投射基于虚拟摄像机位置
 */
@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    public void getEyePosition(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (FreeCameraManager.isActive() && (Object)this instanceof LocalPlayer) {
            cir.setReturnValue(FreeCameraManager.getPosition());
        }
    }
}