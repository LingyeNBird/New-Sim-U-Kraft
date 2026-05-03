package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import com.xiaoliang.simukraft.client.gui.AreaSelectionScreen;
import com.xiaoliang.simukraft.client.gui.BuildingPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MouseHandler Mixin
 * 完全接管鼠标输入，实现自定义鼠标锁定和视角控制。
 *
 * <p>兼容 Ixeris 模组的线程分离架构：
 * Ixeris 将 GLFW 事件轮询（glfwPollEvents）移到主线程执行，渲染留在渲染线程。
 * 本 mixin 不再调用任何 GLFW 函数，改用增量偏移量（lastX/lastY）计算鼠标移动量，
 * 从而避免因跨线程调用 glfwSetCursorPos 造成的输入丢失。
 */
@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    private boolean mouseGrabbed;

    /** 上一次收到 onMove 回调时的鼠标绝对位置，用于计算帧间增量。 */
    @Unique
    private double simukraft_lastX = Double.NaN;

    @Unique
    private double simukraft_lastY = Double.NaN;

    /**
     * 注入 onMove 方法，处理鼠标移动。
     * 使用帧间增量而非窗口中心偏移，避免对 glfwSetCursorPos 的跨线程调用。
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    public void onMoveFreeCamera(long window, double xpos, double ypos, CallbackInfo ci) {
        if (!FreeCameraManager.isActive()) {
            // 自由相机关闭时重置记录位置，防止切换时出现跳变
            simukraft_lastX = Double.NaN;
            simukraft_lastY = Double.NaN;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        Screen currentScreen = mc.screen;

        // 在没有屏幕、BuildingPreviewScreen 或 AreaSelectionScreen 时处理鼠标控制
        boolean shouldHandle = currentScreen == null
            || currentScreen instanceof BuildingPreviewScreen
            || currentScreen instanceof AreaSelectionScreen
            || currentScreen instanceof com.xiaoliang.simukraft.client.gui.LogisticsAreaSelectionScreen;

        if (shouldHandle) {
            // 初始化上一帧位置
            if (Double.isNaN(simukraft_lastX)) {
                simukraft_lastX = xpos;
                simukraft_lastY = ypos;
                // 重置累积值
                this.accumulatedDX = 0;
                this.accumulatedDY = 0;
                return;
            }

            // 用增量计算旋转，不调用 glfwSetCursorPos（兼容 Ixeris 线程模型）
            double deltaX = xpos - simukraft_lastX;
            double deltaY = ypos - simukraft_lastY;

            simukraft_lastX = xpos;
            simukraft_lastY = ypos;

            if (Math.abs(deltaX) > 0.5 || Math.abs(deltaY) > 0.5) {
                // 应用鼠标灵敏度
                double sensitivity = mc.options.sensitivity().get() * 0.6D + 0.2D;
                double sensitivityMultiplier = sensitivity * sensitivity * sensitivity * 0.6D;

                float yawChange = (float) (deltaX * sensitivityMultiplier);
                float pitchChange = (float) (deltaY * sensitivityMultiplier);

                FreeCameraManager.handleRotation(yawChange, pitchChange);
            }

            // 重置累积值，阻止原版处理
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
        }
    }

    /**
     * 注入 turnPlayer 方法，阻止原版实体转动
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    public void turnPlayerFreeCamera(CallbackInfo ci) {
        if (FreeCameraManager.isActive()) {
            Minecraft mc = Minecraft.getInstance();
            Screen currentScreen = mc.screen;

            // 在没有屏幕、BuildingPreviewScreen 或 AreaSelectionScreen 时阻止原版实体转动
            boolean shouldHandle = currentScreen == null
                || currentScreen instanceof BuildingPreviewScreen
                || currentScreen instanceof AreaSelectionScreen;

            if (shouldHandle) {
                ci.cancel(); // 阻止原版实体转动
                // 重置累积值
                this.accumulatedDX = 0;
                this.accumulatedDY = 0;
            }
        }
    }

    /**
     * 注入 grabMouse 方法，在自由相机激活时阻止原版鼠标锁定
     */
    @Inject(method = "grabMouse()V", at = @At("HEAD"), cancellable = true)
    public void grabMouseFreeCamera(CallbackInfo ci) {
        if (FreeCameraManager.isActive()) {
            ci.cancel(); // 阻止原版鼠标锁定
        }
    }

    /**
     * 注入 releaseMouse 方法，在自由相机激活时阻止原版鼠标释放
     */
    @Inject(method = "releaseMouse()V", at = @At("HEAD"), cancellable = true)
    public void releaseMouseFreeCamera(CallbackInfo ci) {
        if (FreeCameraManager.isActive()) {
            ci.cancel(); // 阻止原版鼠标释放
        }
    }
}
