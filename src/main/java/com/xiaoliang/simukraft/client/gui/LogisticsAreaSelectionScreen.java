package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.client.freecamera.FreeCameraManager;
import com.xiaoliang.simukraft.client.preview.AreaSelectionManager;
import com.xiaoliang.simukraft.network.LogisticsActionPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 物流选区界面 — 自由相机模式下选择箱子/木桶区域。
 * 选好后按 Enter 确认，自动发送 LogisticsActionPacket。
 */
@SuppressWarnings("null")
public class LogisticsAreaSelectionScreen extends Screen {

    private final BlockPos blockPos;  // 物流盒位置
    private final LogisticsActionPacket.Action action;
    private boolean mouseGrabbed = false;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    public LogisticsAreaSelectionScreen(BlockPos blockPos, LogisticsActionPacket.Action action) {
        super(Component.translatable("gui.logistics_area_selection.title"));
        this.blockPos = blockPos;
        this.action = action;
    }

    @Override
    protected void init() {
        super.init();
        AreaSelectionManager.startSelection(AreaSelectionManager.SelectionMode.LOGISTICS);
        FreeCameraManager.activate();
    }

    @Override
    public void added() {
        super.added();
        if (this.minecraft != null) {
            this.minecraft.mouseHandler.grabMouse();
            mouseGrabbed = true;
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null && mouseGrabbed) {
            this.minecraft.mouseHandler.releaseMouse();
            mouseGrabbed = false;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int centerX = this.width / 2;
        int y = 5;

        String actionName = switch (action) {
            case CREATE_WAREHOUSE -> Component.translatable("gui.logistics_area_selection.action.create_warehouse").getString();
            case SET_CLIENT_PORT -> Component.translatable("gui.logistics_area_selection.action.set_client_port").getString();
            default -> Component.translatable("gui.logistics_area_selection.action.default").getString();
        };

        // 大标题
        guiGraphics.drawCenteredString(nn(this.font),
                safeString("§e§l[物流] " + actionName), centerX, y, 0xFFFFFF);
        y += 14;

        // 操作提示
        guiGraphics.drawCenteredString(nn(this.font),
                nn(Component.translatable("gui.logistics_area_selection.hint.controls")), centerX, y, 0xAAAAAA);
        y += 11;
        guiGraphics.drawCenteredString(nn(this.font),
                nn(Component.translatable("gui.logistics_area_selection.hint.camera")), centerX, y, 0xAAAAAA);
        y += 14;

        // 费用提示
        if (action == LogisticsActionPacket.Action.CREATE_WAREHOUSE) {
            guiGraphics.drawCenteredString(nn(this.font),
                    nn(Component.translatable("gui.logistics_area_selection.cost.create_warehouse")), centerX, y, 0xFFD700);
            y += 14;
        }

        // 选点状态
        BlockPos p1 = AreaSelectionManager.getPoint1();
        BlockPos p2 = AreaSelectionManager.getPoint2();

        String p1Text = p1 != null
                ? Component.translatable("gui.logistics_area_selection.point1.set", p1.getX(), p1.getY(), p1.getZ()).getString()
                : Component.translatable("gui.logistics_area_selection.point1.not_set").getString();
        String p2Text = p2 != null
                ? Component.translatable("gui.logistics_area_selection.point2.set", p2.getX(), p2.getY(), p2.getZ()).getString()
                : Component.translatable("gui.logistics_area_selection.point2.not_set").getString();

        guiGraphics.drawCenteredString(nn(this.font), safeString(p1Text), centerX, y, 0xFFFFFF);
        y += 11;
        guiGraphics.drawCenteredString(nn(this.font), safeString(p2Text), centerX, y, 0xFFFFFF);

        // 确认提示和费用总计
        if (AreaSelectionManager.hasBothPoints()) {
            y += 16;
            guiGraphics.drawCenteredString(nn(this.font), nn(Component.translatable("gui.logistics_area_selection.confirm_hint")), centerX, y, 0x00FF00);

            // 显示预计容器数量和总费用
            if (action == LogisticsActionPacket.Action.CREATE_WAREHOUSE) {
                y += 12;
                int containerCount = estimateContainerCount();
                double totalCost = containerCount * 2.0;
                guiGraphics.drawCenteredString(nn(this.font),
                    nn(Component.translatable("gui.logistics_area_selection.cost.total", containerCount, totalCost)), centerX, y, 0xFFD700);
            }
        }

        // 底部 ESC 提示
        guiGraphics.drawCenteredString(nn(this.font), nn(Component.translatable("gui.logistics_area_selection.cancel_hint")), centerX, this.height - 12, 0x666666);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null) return true;

        Vec3 cameraPos = nn(FreeCameraManager.getPosition());
        float yaw = FreeCameraManager.getYaw();
        float pitch = FreeCameraManager.getPitch();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 lookDir = nn(new Vec3(x, y, z).normalize());
        Vec3 scaledLookDir = nn(lookDir.scale(100.0));
        Vec3 endPos = nn(cameraPos.add(scaledLookDir));
        BlockHitResult hitResult = level.clip(new ClipContext(
                cameraPos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            if (button == 0) {
                AreaSelectionManager.setPoint1(pos);
                player.displayClientMessage(
                        nn(Component.translatable("gui.logistics_area_selection.point1.set_message", pos.getX(), pos.getY(), pos.getZ())), true);
            } else if (button == 1) {
                AreaSelectionManager.setPoint2(pos);
                player.displayClientMessage(
                        nn(Component.translatable("gui.logistics_area_selection.point2.set_message", pos.getX(), pos.getY(), pos.getZ())), true);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            cancel();
            return true;
        }
        if (keyCode == 257 && AreaSelectionManager.hasBothPoints()) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        BlockPos p1 = AreaSelectionManager.getPoint1();
        BlockPos p2 = AreaSelectionManager.getPoint2();

        AreaSelectionManager.endSelection();
        FreeCameraManager.deactivate();
        if (this.minecraft != null && mouseGrabbed) {
            this.minecraft.mouseHandler.releaseMouse();
            mouseGrabbed = false;
        }

        // 发送网络包
        NetworkManager.INSTANCE.sendToServer(new LogisticsActionPacket(action, blockPos, p1, p2));

        Minecraft minecraft = this.minecraft;
        LocalPlayer player = minecraft != null ? minecraft.player : null;
        if (player != null) {
            String actionName = switch (action) {
                case CREATE_WAREHOUSE -> Component.translatable("gui.logistics_area_selection.result.create_warehouse").getString();
                case SET_CLIENT_PORT -> Component.translatable("gui.logistics_area_selection.result.set_client_port").getString();
                default -> Component.translatable("gui.logistics_area_selection.result.default").getString();
            };
            player.displayClientMessage(
                    nn(Component.translatable("gui.logistics_area_selection.success", safeString(actionName))), false);
        }

        // 确认后重新打开物流盒界面，让玩家看到状态更新
        reopenBlockScreen();
    }

    private void cancel() {
        AreaSelectionManager.endSelection();
        FreeCameraManager.deactivate();
        if (this.minecraft != null && mouseGrabbed) {
            this.minecraft.mouseHandler.releaseMouse();
            mouseGrabbed = false;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    /**
     * 确认后延迟一帧重新打开物流盒界面（等网络包处理完）
     */
    private void reopenBlockScreen() {
        if (this.minecraft == null) return;

        // 延迟 5 tick 再打开，确保服务端已处理完网络包
        final var mc = this.minecraft;
        final var pos = this.blockPos;
        final var act = this.action;

        mc.setScreen(null);
        mc.tell(() -> mc.tell(() -> mc.tell(() -> mc.tell(() -> mc.tell(() -> {
            if (act == LogisticsActionPacket.Action.CREATE_WAREHOUSE
                    || act == LogisticsActionPacket.Action.DELETE_WAREHOUSE) {
                mc.setScreen(new LogisticsServerScreen(pos));
            } else {
                mc.setScreen(new LogisticsClientScreen(pos));
            }
        })))));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 估算选区内的容器数量
     */
    private int estimateContainerCount() {
        BlockPos p1 = AreaSelectionManager.getPoint1();
        BlockPos p2 = AreaSelectionManager.getPoint2();
        if (p1 == null || p2 == null) return 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;

        int minX = Math.min(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX());
        int maxY = Math.max(p1.getY(), p2.getY());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var blockState = mc.level.getBlockState(pos);
                    var block = blockState.getBlock();
                    // 检查是否是箱子或木桶
                    if (block instanceof net.minecraft.world.level.block.ChestBlock ||
                        block instanceof net.minecraft.world.level.block.BarrelBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
