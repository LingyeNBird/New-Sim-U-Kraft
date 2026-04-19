package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camera Mixin
 * 注入 Camera 类的 setup 方法，实现自由相机渲染
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    public void setupFreeCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (FreeCameraManager.isActive() && entity instanceof LocalPlayer) {
            ci.cancel(); // 取消原版渲染逻辑

            this.setPosition(FreeCameraManager.getPosition());
            this.setRotation(FreeCameraManager.getYaw(), FreeCameraManager.getPitch());
        }
    }
}