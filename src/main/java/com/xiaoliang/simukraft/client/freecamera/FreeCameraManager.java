package com.xiaoliang.simukraft.client.freecamera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 自由相机管理器
 * 用于在预览模式下控制自由移动的相机视角
 */
@Mod.EventBusSubscriber(modid = "simukraft", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FreeCameraManager {
    public static boolean active = false;
    public static Vec3 position = Vec3.ZERO;
    public static float yaw = 0;
    public static float pitch = 0;
    public static float speed = 12.0f; // 移动速度（每秒方块数）

    // 按键状态跟踪（volatile 确保主线程写入对渲染线程可见，兼容 Ixeris 等多线程输入模组）
    private static volatile boolean movingForward = false;
    private static volatile boolean movingBackward = false;
    private static volatile boolean movingLeft = false;
    private static volatile boolean movingRight = false;
    private static volatile boolean movingUp = false;
    private static volatile boolean movingDown = false;

    // 用于平滑移动的时间跟踪
    private static long lastUpdateTime = 0;

    /**
     * 切换自由相机模式
     */
    public static void toggle() {
        active = !active;
        if (active) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                position = player.getEyePosition();
                yaw = player.getYRot();
                pitch = player.getXRot();
                // 启用鼠标锁定
                CameraMouseLock.setLocked(true);
            }
            lastUpdateTime = System.nanoTime();
        } else {
            // 关闭时恢复鼠标
            CameraMouseLock.setLocked(false);
            // 重置按键状态
            resetMovementState();
        }
    }

    /**
     * 激活自由相机
     */
    public static void activate() {
        if (!active) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                position = player.getEyePosition();
                yaw = player.getYRot();
                pitch = player.getXRot();
            }
            active = true;
            lastUpdateTime = System.nanoTime();
            // 启用鼠标锁定
            CameraMouseLock.setLocked(true);
        }
    }

    /**
     * 关闭自由相机
     */
    public static void deactivate() {
        if (active) {
            active = false;
            // 关闭时恢复鼠标
            CameraMouseLock.setLocked(false);
            // 重置按键状态
            resetMovementState();
        }
    }

    /**
     * 重置移动状态
     */
    public static void resetMovementState() {
        movingForward = false;
        movingBackward = false;
        movingLeft = false;
        movingRight = false;
        movingUp = false;
        movingDown = false;
    }

    /**
     * 设置移动状态
     */
    public static void setMovingForward(boolean state) {
        movingForward = state;
    }

    public static void setMovingBackward(boolean state) {
        movingBackward = state;
    }

    public static void setMovingLeft(boolean state) {
        movingLeft = state;
    }

    public static void setMovingRight(boolean state) {
        movingRight = state;
    }

    public static void setMovingUp(boolean state) {
        movingUp = state;
    }

    public static void setMovingDown(boolean state) {
        movingDown = state;
    }

    /**
     * 客户端渲染每帧更新（用于平滑移动）
     * 使用 RenderTickEvent 获得更高的更新频率
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 检查玩家是否死亡，如果死亡则自动关闭自由相机
        if (!mc.player.isAlive()) {
            deactivate();
            return;
        }

        // 计算时间差（秒）
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
        lastUpdateTime = currentTime;

        // 限制最大时间差，防止卡顿导致的瞬移
        if (deltaTime > 0.1f) deltaTime = 0.1f;
        if (deltaTime <= 0) return;

        // 计算移动向量
        double moveX = 0;
        double moveZ = 0;
        double moveY = 0;

        // 水平移动
        if (movingForward || movingBackward || movingLeft || movingRight) {
            float yawRad = (float) Math.toRadians(yaw);
            float cosYaw = (float) Math.cos(yawRad);
            float sinYaw = (float) Math.sin(yawRad);

            double zInput = 0;
            double xInput = 0;

            if (movingForward) zInput += 1;
            if (movingBackward) zInput -= 1;
            if (movingLeft) xInput -= 1;
            if (movingRight) xInput += 1;

            // 归一化对角线移动
            if (xInput != 0 && zInput != 0) {
                double length = Math.sqrt(xInput * xInput + zInput * zInput);
                xInput /= length;
                zInput /= length;
            }

            moveX = -sinYaw * zInput - cosYaw * xInput;
            moveZ = cosYaw * zInput - sinYaw * xInput;
        }

        // 垂直移动
        if (movingUp) moveY += 1;
        if (movingDown) moveY -= 1;

        // 应用基于时间的移动
        if (moveX != 0 || moveY != 0 || moveZ != 0) {
            float moveDistance = speed * deltaTime;
            position = position.add(moveX * moveDistance, moveY * moveDistance, moveZ * moveDistance);
        }
    }

    /**
     * 处理水平位移输入（WASD）- 保留用于兼容性，但不再直接使用
     * @param x X轴移动量（A/D，正为右，负为左）
     * @param z Z轴移动量（W/S，正为前，负为后）
     */
    public static void handleMovement(double x, double z) {
        // 已弃用：使用 setMovingXXX 方法和 onRenderTick 代替
    }

    /**
     * 处理垂直位移（Space/Shift）- 保留用于兼容性，但不再直接使用
     * @param y Y轴移动量（正为上升，负为下降）
     */
    public static void handleVertical(double y) {
        // 已弃用：使用 setMovingXXX 方法和 onRenderTick 代替
    }

    /**
     * 处理旋转输入（鼠标移动）
     * @param deltaYaw 水平旋转变化量
     * @param deltaPitch 垂直旋转变化量
     */
    public static void handleRotation(float deltaYaw, float deltaPitch) {
        if (!active) return;

        // 直接应用旋转变化量
        yaw += deltaYaw;
        pitch += deltaPitch;

        // 限制俯仰角在 -90 到 90 度之间
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);

        // 规范化 yaw 角度到 0-360 度
        yaw = yaw % 360.0F;
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
    }

    /**
     * 获取当前相机位置
     */
    public static Vec3 getPosition() {
        return position;
    }

    /**
     * 获取当前水平旋转角度（yaw）
     */
    public static float getYaw() {
        return yaw;
    }

    /**
     * 获取当前垂直旋转角度（pitch）
     */
    public static float getPitch() {
        return pitch;
    }

    /**
     * 检查自由相机是否激活
     */
    public static boolean isActive() {
        return active;
    }
}
