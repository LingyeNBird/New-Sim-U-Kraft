package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;
import java.util.Objects;

public class FruitShopControlBoxScreen extends Screen {
    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    private final BlockPos controlBoxPos;
    private Button hireEmployeeButton;
    private Button fireEmployeeButton;
    
    public FruitShopControlBoxScreen(BlockPos pos) {
        super(Component.translatable("gui.fruit_shop_control_box.title"));
        this.controlBoxPos = pos;
        // 播放建筑盒打开界面音效
        nn(nn(Minecraft.getInstance()).getSoundManager())
            .play(nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.BUILD_BOX_OPEN.get()), 1.0F)));

        // 请求服务器同步雇佣状态
        NetworkManager.INSTANCE.sendToServer(
            new com.xiaoliang.simukraft.network.RequestWorkBlockHireStatusPacket(pos, "fruit_shop")
        );
    }

    @Override
    protected void init() {
        super.init();

        // 完成按钮 - 与建筑盒界面相同的位置和大小
        this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.button.done")),
                        button -> this.onClose())
                .bounds(5, 5, 45, 20)
                .build()));

        // 雇佣员工按钮 - 左下角，长一点的按钮
        hireEmployeeButton = Button.builder(
                        nn(Component.translatable("gui.button.hire_fruit_shop_owner")),
                        button -> {
                            // 直接使用新的商业雇佣界面，避免继续依赖已废弃兼容层
                            nn(Minecraft.getInstance()).setScreen(
                                new HireCommercialScreen(controlBoxPos, "shopkeeper", "fruit_shop")
                            );
                        })
                .bounds(5, this.height - 50, 80, 20)  // 左下角，宽度80像素
                .build();
        this.addRenderableWidget(nn(hireEmployeeButton));

        // 解雇员工按钮 - 在雇佣员工按钮下方
        fireEmployeeButton = Button.builder(
                        nn(Component.translatable("gui.button.fire_employee")),
                        button -> {
                            handleFireEmployee();
                        })
                .bounds(5, this.height - 25, 80, 20)  // 在雇佣员工按钮下方
                .build();
        this.addRenderableWidget(nn(fireEmployeeButton));

        // 更新按钮状态
        updateButtonStates();
    }

    private void updateButtonStates() {
        updateButtonStates(false);
    }

    private void updateButtonStates(boolean skipDataSync) {
        boolean hasHiredEmployee = CommercialClientData.hasHiredEmployee(controlBoxPos);
        
        // 如果已经雇佣了员工，雇佣按钮变为黑色不可点击，解雇按钮正常
        nn(hireEmployeeButton).active = !hasHiredEmployee;
        nn(fireEmployeeButton).active = hasHiredEmployee;
        
        // 设置按钮颜色
        if (hasHiredEmployee) {
            nn(hireEmployeeButton).setMessage(
                nn(Component.translatable("gui.button.hire_employee").withStyle(style -> style.withColor(0x666666)))
            );
            CustomEntity npc = CommercialClientData.getHiredEmployee(controlBoxPos);
            if (npc != null) {
                nn(fireEmployeeButton).setMessage(
                    nn(Component.translatable("gui.button.fire_employee_with_name", nn(npc.getFullName())))
                );
            }
        } else {
            nn(hireEmployeeButton).setMessage(
                nn(Component.translatable("gui.button.hire_employee").withStyle(style -> style.withColor(0xFFFFFF)))
            );
            nn(fireEmployeeButton).setMessage(nn(Component.translatable("gui.button.fire_employee")));
        }
    }

    private void handleFireEmployee() {
        if (CommercialClientData.hasHiredEmployee(controlBoxPos)) {
            CustomEntity npc = CommercialClientData.getHiredEmployee(controlBoxPos);
            java.util.UUID npcUuid = npc != null ? npc.getUUID() : CommercialClientData.getHiredEmployeeUUID(controlBoxPos);

            if (npcUuid != null) {
                NetworkManager.INSTANCE.sendToServer(EmploymentCommandPacket.fireByNpc(npcUuid));
                CommercialClientData.clearHiredEmployee(controlBoxPos);
                updateButtonStates(true);  // 跳过数据同步，立即更新UI
            }
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 绘制标题
        guiGraphics.drawString(nn(this.font), nn(this.title), this.width / 2 - nn(this.font).width(nn(this.title)) / 2, 20, 0xFFFFFF);

        // 绘制当前员工信息
        int infoY = 60;
        guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.label.current_employee")), 20, infoY, 0xAAAAAA);
        
        boolean hasHiredEmployee = CommercialClientData.hasHiredEmployee(controlBoxPos);
        if (hasHiredEmployee) {
            CustomEntity npc = CommercialClientData.getHiredEmployee(controlBoxPos);
            if (npc != null) {
                guiGraphics.drawString(nn(this.font), nn(Component.literal(nn(npc.getFullName()))), 20, infoY + 15, 0xFFFFFF);
                guiGraphics.drawString(nn(this.font), getNpcLevelText(npc), 20, infoY + 30, 0xAAAAAA);
            } else {
                guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.label.loading_npc_info")), 20, infoY + 15, 0xFFFF55);
            }
        } else {
            guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.label.no_employee")), 20, infoY + 15, 0xFF5555);
        }
    }

    @Nonnull
    private Component getNpcLevelText(@Nonnull CustomEntity npc) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        Object levelValue = server != null ? NPCDataManager.getNPCLevel(server, npc.getUUID()) : "?";
        return nn(Component.translatable("gui.label.npc_level", levelValue));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键关闭界面
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 获取控制盒位置
     */
    public BlockPos getControlBoxPos() {
        return controlBoxPos;
    }

    /**
     * 刷新按钮状态（用于服务器同步后刷新界面）
     */
    public void refreshButtonStates() {
        updateButtonStates();
    }

    // 定期刷新计时器
    private int refreshTimer = 0;
    private static final int REFRESH_INTERVAL = 10; // 0.5秒（10 ticks）

    @Override
    public void tick() {
        super.tick();

        // 定期刷新按钮状态，确保与服务器数据同步
        refreshTimer++;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            updateButtonStates();
        }
    }
}
