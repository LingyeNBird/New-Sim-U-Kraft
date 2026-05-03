package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.LogisticsActionPacket;
import com.xiaoliang.simukraft.network.LogisticsSyncPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestLogisticsStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 物流盒客户端界面 — 2 个按钮：设定端口 / 删除端口。
 */
public class LogisticsClientScreen extends AbstractTransitionScreen
        implements LogisticsSyncPacket.LogisticsSyncReceiver {

    private final BlockPos blockPos;
    private boolean hasPorts = false;
    private int portCount = 0;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    public LogisticsClientScreen(BlockPos blockPos) {
        super(Component.translatable("gui.logistics_client.title"));
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        // 从本地缓存快速加载（如果有）
        if (ClientConnectionData.hasConnectedContainers(blockPos)) {
            hasPorts = true;
            portCount = ClientConnectionData.getContainerCount(blockPos);
        }
        rebuildButtons();

        // 向服务器请求最新状态
        requestServerStatus();
    }

    private void requestServerStatus() {
        NetworkManager.INSTANCE.sendToServer(new RequestLogisticsStatusPacket(blockPos, false));
    }

    private void rebuildButtons() {
        clearWidgets();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 24;
        int btnWidth = 160;
        int btnHeight = 20;
        int gap = 24;

        // 连接容器（可重复，每次追加新容器）
        addRenderableWidget(nn(Button.builder(
                nn(hasPorts ? Component.translatable("gui.logistics_client.connect_ports.count", portCount) : Component.translatable("gui.logistics_client.connect_ports")),
                btn -> onSetPort())
                .bounds(centerX - btnWidth / 2, startY, btnWidth, btnHeight).build()));

        // 容器管理（查看已连接容器）
        addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.logistics_client.manage_ports")), btn -> onManagePorts())
                .bounds(centerX - btnWidth / 2, startY + gap, btnWidth, btnHeight).build()))
                .active = hasPorts;

        // 储量信息（查看容器内物品）
        addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.logistics_client.storage_info")), btn -> onStorageInfo())
                .bounds(centerX - btnWidth / 2, startY + gap * 2, btnWidth, btnHeight).build()))
                .active = hasPorts;

        // 路径概览（只读查看关联路径）
        addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.logistics_client.route_overview")), btn -> onRouteOverview())
                .bounds(centerX - btnWidth / 2, startY + gap * 3, btnWidth, btnHeight).build()))
                .active = hasPorts;
    }

    @Override
    protected void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        int alphaByte = (int) (getAlpha() * 180) & 0xFF;
        guiGraphics.fill(0, 0, this.width, this.height, (alphaByte << 24) | 0x111111);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int alphaByte = (int) (getAlpha() * 255) & 0xFF;
        int titleColor = (alphaByte << 24) | 0xFFFFFF;
        guiGraphics.drawCenteredString(nn(this.font), nn(this.title), this.width / 2, this.height / 2 - 44, titleColor);
    }

    private void onSetPort() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new LogisticsAreaSelectionScreen(
                    blockPos, LogisticsActionPacket.Action.SET_CLIENT_PORT));
        }
    }

    private void onManagePorts() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ContainerManageScreen(blockPos));
        }
    }

    private void onRouteOverview() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ClientRouteOverviewScreen(blockPos));
        }
    }

    private void onStorageInfo() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) return;

        // 使用缓存的容器位置数据
        var positions = ClientConnectionData.getContainerPositions(blockPos);
        if (!positions.isEmpty()) {
            minecraft.setScreen(new ClientStorageScreen(blockPos, positions));
        }
    }

    @Override
    public void onLogisticsSync(LogisticsSyncPacket packet) {
        if (packet.getSyncType() == LogisticsSyncPacket.SyncType.CLIENT_STATUS
                && packet.getBlockPos().equals(this.blockPos)) {
            this.hasPorts = packet.hasPorts();
            this.portCount = packet.getPortCount();
            rebuildButtons();

            // 更新本地缓存
            if (!packet.hasPorts()) {
                ClientConnectionData.removeClientData(blockPos);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
