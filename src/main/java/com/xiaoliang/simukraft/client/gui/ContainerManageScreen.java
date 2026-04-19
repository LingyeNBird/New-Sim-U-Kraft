package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.ContainerListResponsePacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestContainerListPacket;
import com.xiaoliang.simukraft.network.UnbindContainerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 容器管理界面 — 列出客户端绑定的所有容器，显示类型和物品数，支持逐个解绑。
 */
public class ContainerManageScreen extends AbstractTransitionScreen
        implements ContainerListResponsePacket.ContainerListReceiver {

    private final BlockPos clientBlockPos;
    private List<ContainerEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 8;

    public ContainerManageScreen(BlockPos clientBlockPos) {
        super(Component.translatable("gui.container_manage.title"));
        this.clientBlockPos = clientBlockPos;
    }

    @Override
    protected void init() {
        super.init();

        // 请求服务端发送容器列表
        NetworkManager.INSTANCE.sendToServer(new RequestContainerListPacket(clientBlockPos));

        // 返回按钮
        addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.button.back")), btn -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20).build()));
    }

    @Override
    public void onContainerListReceived(ContainerListResponsePacket packet) {
        if (!packet.getClientBlockPos().equals(clientBlockPos)) return;

        entries.clear();
        for (ContainerListResponsePacket.ContainerEntry entry : packet.getEntries()) {
            entries.add(new ContainerEntry(entry.pos(), entry.blockName(), entry.kinds(), entry.total()));
        }
        rebuildList();
    }

    private void rebuildList() {
        // 移除旧的解绑按钮（保留返回按钮）
        List<net.minecraft.client.gui.components.events.GuiEventListener> toRemove = new ArrayList<>();
        for (var child : this.children()) {
            if (child instanceof Button btn && btn.getMessage().getString().equals(
                    Component.translatable("gui.container_manage.unbind").getString())) {
                toRemove.add(child);
            }
        }
        toRemove.forEach(this::removeWidget);

        int listX = this.width / 2 - 150;
        int listY = 40;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= entries.size()) break;
            int btnY = listY + i * ROW_HEIGHT;
            final int entryIndex = idx;

            addRenderableWidget(nn(Button.builder(nn(Component.translatable("gui.container_manage.unbind")), btn -> onUnbind(entryIndex))
                    .bounds(listX + 270, btnY, 30, 18).build()));
        }
    }

    private void onUnbind(int index) {
        if (index < 0 || index >= entries.size()) return;
        BlockPos posToRemove = entries.get(index).pos;

        // 发送解绑请求到服务端
        NetworkManager.INSTANCE.sendToServer(new UnbindContainerPacket(clientBlockPos, posToRemove));

        // 乐观更新本地列表
        entries.remove(index);
        rebuildList();
    }

    @Override
    protected void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        int a = (int) (getAlpha() * 200) & 0xFF;
        guiGraphics.fill(0, 0, this.width, this.height, (a << 24) | 0x0A0A0A);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int a = (int) (getAlpha() * 255) & 0xFF;
        int white = (a << 24) | 0xFFFFFF;
        int gray = (a << 24) | 0xAAAAAA;

        // 标题
        guiGraphics.drawCenteredString(nn(this.font), "容器管理 (" + entries.size() + " 个)", this.width / 2, 10, white);

        // 表头
        int listX = this.width / 2 - 150;
        int listY = 28;
        guiGraphics.drawString(nn(this.font), "§7坐标", listX, listY, gray);
        guiGraphics.drawString(nn(this.font), "§7类型", listX + 120, listY, gray);
        guiGraphics.drawString(nn(this.font), "§7物品", listX + 210, listY, gray);
        listY += 12;

        // 分割线
        guiGraphics.fill(listX, listY, listX + 300, listY + 1, (a << 24) | 0x444444);

        // 列表
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= entries.size()) break;

            ContainerEntry entry = entries.get(idx);
            int rowY = 40 + i * ROW_HEIGHT;

            // 行背景（交替色）
            if (i % 2 == 0) {
                guiGraphics.fill(listX - 2, rowY - 2, listX + 302, rowY + ROW_HEIGHT - 4, (a << 24) | 0x181828);
            }

            // 坐标
            String posStr = entry.pos.getX() + ", " + entry.pos.getY() + ", " + entry.pos.getZ();
            guiGraphics.drawString(nn(this.font), nn(posStr), listX, rowY + 4, white);

            // 类型
            String type = entry.blockName.length() > 10 ? entry.blockName.substring(0, 10) + ".." : entry.blockName;
            guiGraphics.drawString(nn(this.font), nn(type), listX + 120, rowY + 4, (a << 24) | 0xFFCC44);

            // 物品统计
            guiGraphics.drawString(nn(this.font), entry.kinds + "种/" + entry.total + "个", listX + 210, rowY + 4, gray);
        }

        // 空列表提示
        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(nn(this.font), "§8暂无已绑定的容器", this.width / 2, this.height / 2, gray);
        }

        // 滚动提示
        if (entries.size() > VISIBLE_ROWS) {
            guiGraphics.drawCenteredString(nn(this.font), "§7滚轮翻页 (" + (scrollOffset + 1) + "-"
                    + Math.min(scrollOffset + VISIBLE_ROWS, entries.size()) + "/" + entries.size() + ")",
                    this.width / 2, this.height - 42, gray);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta));
        rebuildList();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    private record ContainerEntry(BlockPos pos, String blockName, int kinds, int total) {}
}
