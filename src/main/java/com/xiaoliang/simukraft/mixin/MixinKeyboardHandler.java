package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import com.xiaoliang.simukraft.client.gui.AreaSelectionScreen;
import com.xiaoliang.simukraft.client.gui.BuildingPreviewScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * KeyboardHandler Mixin
 * 注入 KeyboardHandler 类的 keyPress 方法，处理 WASD 和 Space/Shift 按键
 */
@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"))
    public void keyPressFreeCamera(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (!FreeCameraManager.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        // 在没有屏幕、BuildingPreviewScreen 或 AreaSelectionScreen 时处理按键
        boolean shouldHandle = mc.screen == null
            || mc.screen instanceof BuildingPreviewScreen
            || mc.screen instanceof AreaSelectionScreen
            || mc.screen instanceof com.xiaoliang.simukraft.client.gui.LogisticsAreaSelectionScreen;

        if (!shouldHandle) return;

        // 处理按键按下和释放事件
        boolean isPressed = (action == GLFW.GLFW_PRESS);
        boolean isReleased = (action == GLFW.GLFW_RELEASE);

        if (!isPressed && !isReleased) return;

        boolean state = isPressed;

        switch (key) {
            case GLFW.GLFW_KEY_W:
                FreeCameraManager.setMovingForward(state);
                break;
            case GLFW.GLFW_KEY_S:
                FreeCameraManager.setMovingBackward(state);
                break;
            case GLFW.GLFW_KEY_A:
                FreeCameraManager.setMovingLeft(state);
                break;
            case GLFW.GLFW_KEY_D:
                FreeCameraManager.setMovingRight(state);
                break;
            case GLFW.GLFW_KEY_SPACE:
                FreeCameraManager.setMovingUp(state);
                break;
            case GLFW.GLFW_KEY_LEFT_SHIFT:
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                FreeCameraManager.setMovingDown(state);
                break;
        }
    }
}
