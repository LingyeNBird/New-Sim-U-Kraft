package com.xiaoliang.simukraft.client.freecamera;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 相机鼠标锁定管理器
 * 用于管理自由相机模式下的鼠标锁定状态
 */
public class CameraMouseLock {
    private static boolean isLocked = false;
    private static int lastMouseX = 0;
    private static int lastMouseY = 0;

    /**
     * 设置鼠标锁定状态。
     * GLFW 函数必须在主线程调用，兼容 Ixeris 等将 GLFW 事件轮询移至主线程的模组。
     * 此方法通过 Minecraft 的主线程队列提交 GLFW 调用，确保线程安全。
     *
     * @param locked true 锁定鼠标，false 解锁鼠标
     */
    public static void setLocked(boolean locked) {
        isLocked = locked;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // 将 GLFW 调用提交到 MC 主线程，兼容 Ixeris（主线程/渲染线程分离架构）
        mc.execute(() -> {
            var window = mc.getWindow();
            if (window == null) return;

            long windowHandle = window.getWindow();
            if (windowHandle == 0) return;

            if (locked) {
                // 锁定模式：隐藏光标并捕获鼠标
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            } else {
                // 解锁模式：显示光标
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            }
        });
    }

    /**
     * 检查鼠标是否被锁定
     */
    public static boolean isLocked() {
        return isLocked;
    }

    /**
     * 获取上次鼠标 X 坐标
     */
    public static int getLastMouseX() {
        return lastMouseX;
    }

    /**
     * 获取上次鼠标 Y 坐标
     */
    public static int getLastMouseY() {
        return lastMouseY;
    }

    /**
     * 设置上次鼠标坐标
     */
    public static void setLastMousePos(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
    }
}
