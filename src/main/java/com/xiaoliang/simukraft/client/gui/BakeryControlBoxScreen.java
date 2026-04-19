package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

@SuppressWarnings({"null", "deprecation"})
public class BakeryControlBoxScreen extends Screen {
    private final BlockPos controlBoxPos;
    private Button hireEmployeeButton;
    private Button fireEmployeeButton;
    
    public BakeryControlBoxScreen(BlockPos pos) {
        super(Component.translatable("gui.bakery_control_box.title"));
        this.controlBoxPos = pos;
        // 播放建筑盒打开界面音效
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            com.xiaoliang.simukraft.init.ModSoundEvents.BUILD_BOX_OPEN.get(), 1.0F));

        // 请求服务器同步雇佣状态
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[BakeryControlBoxScreen] 发送雇佣状态同步请求，位置=" + pos);
        com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(
            new com.xiaoliang.simukraft.network.RequestWorkBlockHireStatusPacket(pos, "bakery")
        );
    }

    @Override
    protected void init() {
        super.init();

        // 完成按钮 - 与建筑盒界面相同的位置和大小
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.button.done"),
                        button -> this.onClose())
                .bounds(5, 5, 45, 20)
                .build());

        // 雇佣员工按钮 - 左下角，长一点的按钮
        hireEmployeeButton = Button.builder(
                        Component.translatable("gui.button.hire_bakery_owner"),
                        button -> {
                            // 切换到雇佣面包店老板界面（使用新的基于AbstractHireScreen的界面）
                            Minecraft.getInstance().setScreen(new HireCommercialEmployeeScreen(controlBoxPos, "bakery"));
                        })
                .bounds(5, this.height - 50, 80, 20)  // 左下角，宽度80像素
                .build();
        this.addRenderableWidget(hireEmployeeButton);

        // 解雇员工按钮 - 在雇佣员工按钮下方
        fireEmployeeButton = Button.builder(
                        Component.translatable("gui.button.fire_employee"),
                        button -> {
                            handleFireEmployee();
                        })
                .bounds(5, this.height - 25, 80, 20)  // 在雇佣员工按钮下方
                .build();
        this.addRenderableWidget(fireEmployeeButton);

        // 更新按钮状态
        updateButtonStates();
    }

    private void updateButtonStates() {
        updateButtonStates(false);
    }

    private void updateButtonStates(boolean skipDataSync) {
        boolean hasHiredEmployee = CommercialBuildingClientData.hasHiredEmployee(controlBoxPos, "bakery");
        
        // 如果已经雇佣了员工，雇佣按钮变为黑色不可点击，解雇按钮正常
        hireEmployeeButton.active = !hasHiredEmployee;
        fireEmployeeButton.active = hasHiredEmployee;
        
        // 设置按钮颜色
        if (hasHiredEmployee) {
            hireEmployeeButton.setMessage(Component.translatable("gui.button.hire_employee").withStyle(style -> style.withColor(0x666666)));
            CustomEntity npc = CommercialBuildingClientData.getHiredEmployee(controlBoxPos, "bakery");
            if (npc != null) {
                fireEmployeeButton.setMessage(Component.translatable("gui.button.fire_employee_with_name", npc.getFullName()));
            }
        } else {
            hireEmployeeButton.setMessage(Component.translatable("gui.button.hire_employee").withStyle(style -> style.withColor(0xFFFFFF)));
            fireEmployeeButton.setMessage(Component.translatable("gui.button.fire_employee"));
        }
    }

    private void handleFireEmployee() {
        if (CommercialBuildingClientData.hasHiredEmployee(controlBoxPos, "bakery")) {
            CustomEntity npc = CommercialBuildingClientData.getHiredEmployee(controlBoxPos, "bakery");
            java.util.UUID npcUuid = npc != null ? npc.getUUID() : CommercialClientData.getHiredEmployeeUUID(controlBoxPos);

            if (npcUuid != null) {
                com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(EmploymentCommandPacket.fireByNpc(npcUuid));
                CommercialBuildingClientData.clearHiredEmployee(controlBoxPos, "bakery");
                updateButtonStates(true);  // 跳过数据同步，立即更新UI
            }
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 黑色半透明背景 - 与建筑盒相同的背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);
        
        // 白色标题：建筑控制面板
        int titleColor = 0xFFFFFF; // 白色
        Component title = Component.translatable("gui.control_panel.title").withStyle(style -> style.withColor(titleColor));
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 10, titleColor);

        // 黄色文字内容
        int textColor = 0xFFF5F5A0; // 黄色

        // 第一行：建筑：面包店 by XiaoLiang小亮
        Component line1 = Component.translatable("gui.control_panel.building.bakery").withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(this.font, line1, 10, 35, textColor, false);

        // 第二行：类型：商业类（可用建筑）
        Component line2 = Component.translatable("gui.control_panel.type.commercial").withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(this.font, line2, 10, 50, textColor, false);

        // 第三行：员工状态
        Component line3;
        if (CommercialBuildingClientData.hasHiredEmployee(controlBoxPos, "bakery")) {
            CustomEntity npc = CommercialBuildingClientData.getHiredEmployee(controlBoxPos, "bakery");
            if (npc != null) {
                line3 = Component.translatable("gui.control_panel.employee.hired", npc.getFullName()).withStyle(style -> style.withColor(textColor));
            } else {
                line3 = Component.translatable("gui.control_panel.employee.hired_not_found").withStyle(style -> style.withColor(textColor));
            }
        } else {
            line3 = Component.translatable("gui.control_panel.employee.none").withStyle(style -> style.withColor(textColor));
        }
        guiGraphics.drawString(this.font, line3, 10, 65, textColor, false);
        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
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
