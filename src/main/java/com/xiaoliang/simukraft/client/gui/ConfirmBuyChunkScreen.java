package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.BuyChunkPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public class ConfirmBuyChunkScreen extends AbstractTransitionScreen {
    private final UUID cityId;
    private final ChunkPos chunkPos;
    private final double cost; // 购买成本

    public ConfirmBuyChunkScreen(UUID cityId, ChunkPos chunkPos, double cost) {
        super(Component.translatable("gui.confirm_buy_chunk.title"));
        this.cityId = cityId;
        this.chunkPos = chunkPos;
        this.cost = cost;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 创建确认按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.translatable("gui.confirm_buy_chunk.confirm")),
            button -> this.onConfirm()
        ).pos(centerX - 110, centerY + 30).size(100, 20).build()));
        
        // 创建取消按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.translatable("gui.confirm_buy_chunk.cancel")),
            button -> this.closeScreen()
        ).pos(centerX + 10, centerY + 30).size(100, 20).build()));
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制深灰色背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF333333); // 深灰色背景
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 渲染标题
        Component title = Component.translatable("gui.confirm_buy_chunk.title");
        guiGraphics.drawCenteredString(nn(this.font), nn(title), centerX, centerY - 60, 0xFFFFFF);

        // 渲染确认信息
        Component message = Component.translatable("gui.confirm_buy_chunk.message");
        guiGraphics.drawCenteredString(nn(this.font), nn(message), centerX, centerY - 30, 0xFFFFFF);
        
        // 渲染区块坐标
        String chunkCoords = String.format("(%d, %d)", chunkPos.x, chunkPos.z);
        guiGraphics.drawCenteredString(nn(this.font), nn(Component.literal(nn(chunkCoords))), centerX, centerY - 10, 0xFFFFFF);
        
        // 渲染成本信息
        String costText = String.format("$%.2f", cost);
        guiGraphics.drawCenteredString(nn(this.font), nn(Component.literal(nn(costText))), centerX, centerY + 10, 0x00FF00);
    }

    private void onConfirm() {
        // 发送购买区块请求
        BuyChunkPacket packet = new BuyChunkPacket(cityId, chunkPos);
        NetworkManager.INSTANCE.sendToServer(packet);
        
        this.closeScreen();
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }
}