package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;


public class HirePlannerScreen extends AbstractHireScreen {
    private final BlockPos buildBoxPos;
    public HirePlannerScreen(BlockPos buildBoxPos) {
        super(Component.translatable("gui.hire_planner.title"));
        this.buildBoxPos = buildBoxPos;
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

        confirmButton = addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.button.hire")),
                        button -> confirmSelection())
                .pos(width - 90, height - 30)
                .size(80, 20)
                .build()));

        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());
        this.statusText = nn(Component.translatable("message.simukraft.loading_npcs"));
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId != null) {
            String dimensionId = level().dimension().location().toString();
            NetworkManager.INSTANCE.sendToServer(
                    EmploymentCommandPacket.hire(selectedNPCId, this.buildBoxPos, "build_box", "planner", dimensionId)
            );
            onClose();
        }
    }

    @Override
    public void onClose() {
        minecraft().setScreen(new BuildBoxScreen(buildBoxPos));
    }
}
