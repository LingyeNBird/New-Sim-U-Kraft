package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class HireBuilderScreen extends AbstractHireScreen {
    private final BlockPos buildBoxPos;
    private final BlockPos lastPlayerPos;
    private long lastCheckTime = 0;

    public HireBuilderScreen(BlockPos buildBoxPos) {
        super(Component.translatable("gui.hire_builder.title"));
        this.buildBoxPos = buildBoxPos;
        this.lastPlayerPos = player().blockPosition();
    }

    @Override
    public void tick() {
        super.tick();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime > 100) { // 每100毫秒检查一次
            lastCheckTime = currentTime;
            BlockPos currentPos = player().blockPosition();
            if (!currentPos.equals(lastPlayerPos)) {
                player().displayClientMessage(
                    nn(Component.translatable("message.simukraft.must_stand_still").withStyle(style -> style.withColor(0xFF0000))),
                    false
                );
                this.onClose();
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        prevPageButton = addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.pagination.previous")),
                        button -> {
                            currentPage--;
                            onPageChanged();
                        })
                .pos(width / 2 - 100, height - 30)
                .size(80, 20)
                .build()));

        nextPageButton = addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.pagination.next")),
                        button -> {
                            currentPage++;
                            onPageChanged();
                        })
                .pos(width / 2 + 20, height - 30)
                .size(80, 20)
                .build()));

        // 检查是否已经雇佣了建筑师
        boolean hasHiredBuilder = BuildBoxData.hasHiredBuilder(buildBoxPos);

        confirmButton = nn(Button.builder(
                        nn(Component.translatable("gui.button.hire")),
                        button -> confirmSelection())
                .pos(width - 90, height - 30)
                .size(80, 20)
                .build());
        
        // 设置按钮激活状态
        nn(confirmButton).active = !hasHiredBuilder;
        
        this.addRenderableWidget(nn(confirmButton));

        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());
        
        // 根据雇佣状态设置提示信息
        if (hasHiredBuilder) {
            this.statusText = nn(Component.translatable("message.simukraft.already_hired_builder").withStyle(style -> style.withColor(0xFF5555)));
        } else {
            this.statusText = nn(Component.translatable("message.simukraft.loading_npcs"));
        }
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId != null) {
            BlockPos targetBuildBoxPos = this.buildBoxPos;

            // 再次检查是否已经雇佣了建筑师
            if (BuildBoxData.hasHiredBuilder(targetBuildBoxPos)) {
                player().displayClientMessage(
                        nn(Component.translatable("message.simukraft.buildbox_already_has_builder").withStyle(ChatFormatting.RED)),
                        true
                );
                return;
            }

            // 使用v2命令包发送雇佣请求
            String dimensionId = level().dimension().location().toString();
            NetworkManager.INSTANCE.sendToServer(
                    EmploymentCommandPacket.hire(selectedNPCId, targetBuildBoxPos, "build_box", "builder", dimensionId)
            );

            // 本地缓存预更新（乐观更新，等待服务器确认后最终状态）
            BuildBoxData.setHiredBuilder(targetBuildBoxPos, selectedNPCId);

            onClose();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new BuildBoxScreen(buildBoxPos));
    }
}
