package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 雇佣仓库管理员界面 — 照抄建筑盒模式。
 */
public class HireWarehouseManagerScreen extends AbstractHireScreen {

    private final BlockPos controlBoxPos;

    public HireWarehouseManagerScreen(BlockPos controlBoxPos) {
        super(Component.translatable("gui.hire_warehouse_manager.title"));
        this.controlBoxPos = controlBoxPos;
    }

    @Override
    protected void init() {
        super.init();

        if (confirmButton != null) {
            confirmButton.setMessage(nn(Component.translatable("gui.hire_warehouse_manager.confirm")));
        }

        // 请求服务端发送空闲 NPC 列表
        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());
        this.statusText = nn(Component.translatable("message.simukraft.loading_npcs"));
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId == null) return;

        String dimensionId = level().dimension().location().toString();

        // 照抄建筑盒：发送雇佣请求
        NetworkManager.INSTANCE.sendToServer(
                EmploymentCommandPacket.hire(
                        selectedNPCId,
                        controlBoxPos,
                        "logistics",
                        "warehouse_manager",
                        dimensionId
                )
        );

        // 照抄建筑盒：显示消息并返回
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    nn(Component.translatable("gui.hire_warehouse_manager.request_sent")), false);
        }

        // 照抄建筑盒：返回物流服务器界面
        nn(this.minecraft).setScreen(new LogisticsServerScreen(controlBoxPos));
    }
}
