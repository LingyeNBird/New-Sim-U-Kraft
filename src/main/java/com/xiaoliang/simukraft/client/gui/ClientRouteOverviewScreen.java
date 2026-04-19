package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.ClientRoutesResponsePacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestClientRoutesPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 客户端路径概览 — 只读显示该客户端关联的所有物流路径。
 */
public class ClientRouteOverviewScreen extends AbstractTransitionScreen
        implements ClientRoutesResponsePacket.ClientRoutesReceiver {

    private final BlockPos clientBlockPos;
    private List<RouteInfo> routeInfos = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_HEIGHT = 44;

    public ClientRouteOverviewScreen(BlockPos clientBlockPos) {
        super(Component.translatable("gui.client_route_overview.title"));
        this.clientBlockPos = clientBlockPos;
    }

    @Override
    protected void init() {
        super.init();

        // 请求服务端发送路径数据
        NetworkManager.INSTANCE.sendToServer(new RequestClientRoutesPacket(clientBlockPos));

        addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.button.back")), btn -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20).build()));
    }

    @Override
    public void onClientRoutesReceived(ClientRoutesResponsePacket packet) {
        routeInfos.clear();
        for (ClientRoutesResponsePacket.RouteInfo info : packet.getRoutes()) {
            routeInfos.add(new RouteInfo(info.name(), info.direction(), info.enabled(),
                    info.warehousePos(), info.warehouseHasNpc(), info.itemNames()));
        }
    }

    @Override
    protected void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        int a = (int) (getAlpha() * 200) & 0xFF;
        nn(guiGraphics).fill(0, 0, this.width, this.height, (a << 24) | 0x0A0A0A);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(nn(guiGraphics), mouseX, mouseY, partialTicks);

        int a = (int) (getAlpha() * 255) & 0xFF;
        int white = (a << 24) | 0xFFFFFF;
        int gray = (a << 24) | 0xAAAAAA;

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(nn(this.font), safeString("路径概览 (" + routeInfos.size() + " 条)"), centerX, 10, white);

        if (routeInfos.isEmpty()) {
            guiGraphics.drawCenteredString(nn(this.font), "§8该客户端暂无关联路径", centerX, this.height / 2, gray);
            return;
        }

        int startX = centerX - 150;
        int y = 28;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= routeInfos.size()) break;
            RouteInfo info = routeInfos.get(idx);

            // 行背景
            if (i % 2 == 0) {
                guiGraphics.fill(startX - 4, y - 2, startX + 304, y + ROW_HEIGHT - 6, (a << 24) | 0x151528);
            }

            // 状态指示
            String statusDot = info.enabled ? "§a●" : "§8○";
            String dirText = "SEND".equals(info.direction) ? "§a发送 →" : "§c← 接收";
            String npcStatus = info.warehouseHasNpc ? "§a运行中" : "§c管理员缺席";

            guiGraphics.drawString(nn(this.font), safeString(statusDot + " §f" + info.name + "  " + dirText), startX, y, white);
            guiGraphics.drawString(nn(this.font), safeString("§7服务端: " + info.warehousePos + "  " + npcStatus), startX + 10, y + 11, gray);

            String items = info.itemNames.size() <= 3
                    ? String.join(", ", info.itemNames)
                    : info.itemNames.get(0) + ", " + info.itemNames.get(1) + "... 共" + info.itemNames.size() + "种";
            guiGraphics.drawString(nn(this.font), safeString("§7传输: " + items), startX + 10, y + 22, gray);

            y += ROW_HEIGHT;
        }

        if (routeInfos.size() > VISIBLE_ROWS) {
            guiGraphics.drawCenteredString(nn(this.font),
                    safeString("§7滚轮翻页 (" + (scrollOffset + 1) + "-"
                            + Math.min(scrollOffset + VISIBLE_ROWS, routeInfos.size())
                            + "/" + routeInfos.size() + ")"),
                    centerX, this.height - 42, gray);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, routeInfos.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(String value) {
        return nn(value);
    }

    private record RouteInfo(String name, String direction, boolean enabled,
                              String warehousePos, boolean warehouseHasNpc, List<String> itemNames) {}
}
