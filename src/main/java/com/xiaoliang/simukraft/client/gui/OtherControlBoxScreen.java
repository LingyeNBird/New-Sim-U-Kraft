package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.building.MedicalBuildingConfig;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestWorkBlockHireStatusPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 其他控制盒GUI
 * 简单的控制盒界面，仅显示基本信息和拆除按钮
 */
public class OtherControlBoxScreen extends Screen {

    private final BlockPos controlBoxPos;
    private final String buildingFileName;
    private Button hireDoctorButton;
    private Button fireDoctorButton;
    private int refreshTimer = 0;
    private static final int REFRESH_INTERVAL = 10;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    public OtherControlBoxScreen(BlockPos pos) {
        this(pos, null);
    }

    public OtherControlBoxScreen(BlockPos pos, @Nullable String buildingFileName) {
        super(Component.translatable("gui.other_control_box.title"));
        this.controlBoxPos = pos;
        this.buildingFileName = buildingFileName;

        // 播放建筑盒打开界面音效
        net.minecraft.client.Minecraft.getInstance().getSoundManager()
            .play(nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.BUILD_BOX_OPEN.get()), 1.0F)));
        NetworkManager.INSTANCE.sendToServer(new RequestWorkBlockHireStatusPacket(pos, "other_control_box"));
    }

    /**
     * 获取控制盒位置
     */
    public BlockPos getControlBoxPos() {
        return controlBoxPos;
    }

    @Override
    protected void init() {
        super.init();

        // 完成按钮
        this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.button.done")),
                        button -> this.onClose())
                .bounds(5, 5, 45, 20)
                .build()));

        // simukraft: 拆除按钮（右上角）
        int demolishBtnWidth = 60;
        this.addRenderableWidget(nn(Button.builder(
                        nn(Component.translatable("gui.button.demolish")),
                        button -> this.onDemolishClicked())
                .bounds(this.width - demolishBtnWidth - 5, 5, demolishBtnWidth, 20)
                .build()));

        hireDoctorButton = nn(Button.builder(
                        nn(Component.translatable("gui.button.hire_employee_with_job",
                                Component.translatable("gui.employee_info.job.doctor"))),
                        button -> nn(this.minecraft).setScreen(new HireDoctorScreen(controlBoxPos, getEffectiveBuildingFileName())))
                .bounds(5, this.height - 50, 95, 20)
                .build());
        this.addRenderableWidget(hireDoctorButton);

        fireDoctorButton = nn(Button.builder(
                        nn(Component.translatable("gui.button.fire_employee")),
                        button -> handleFireDoctor())
                .bounds(5, this.height - 25, 95, 20)
                .build());
        this.addRenderableWidget(fireDoctorButton);

        updateButtonStates();
    }

    /**
     * 点击拆除按钮处理
     */
    private void onDemolishClicked() {
        // 发送拆除请求到服务器
        com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(
            new com.xiaoliang.simukraft.network.DemolishBuildingPacket(controlBoxPos)
        );
        // 关闭界面
        this.onClose();
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 黑色半透明背景 - 与建筑盒相同的背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);

        // 白色标题：建筑控制面板
        int titleColor = 0xFFFFFF;
        Component title = Component.translatable("gui.other_control_box.panel_title").withStyle(style -> style.withColor(titleColor));
        guiGraphics.drawCenteredString(nn(this.font), nn(title), this.width / 2, 10, titleColor);

        // 黄色文字内容
        int textColor = 0xFFF5F5A0;
        MedicalBuildingConfig medicalConfig = resolveMedicalConfig();
        String buildingName = medicalConfig != null && medicalConfig.buildingName() != null
                ? medicalConfig.buildingName()
                : Component.translatable("gui.other_control_box.unknown_building").getString();

        // 第一行：建筑信息
        Component line1 = Component.translatable("gui.control_panel.building.name_format", buildingName)
                .withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(nn(this.font), nn(line1), 10, 35, textColor, false);

        Component line2 = Component.translatable("gui.other_control_box.type",
                        medicalConfig != null
                                ? Component.translatable("gui.other_control_box.type.medical")
                                : Component.translatable("gui.other_control_box.type.generic"))
                .withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(nn(this.font), nn(line2), 10, 50, textColor, false);

        Component line3 = Component.translatable("gui.other_control_box.parturition",
                        medicalConfig != null && medicalConfig.canParturition()
                                ? Component.translatable("gui.switch.on")
                                : Component.translatable("gui.switch.off"))
                .withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(nn(this.font), nn(line3), 10, 65, textColor, false);

        Component line4 = Component.translatable("gui.other_control_box.doctor",
                        getDoctorDisplayName())
                .withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(nn(this.font), nn(line4), 10, 80, textColor, false);

        Component line5 = Component.translatable(
                        medicalConfig != null && medicalConfig.canParturition()
                                ? "gui.other_control_box.parturition_desc_enabled"
                                : "gui.other_control_box.parturition_desc_disabled")
                .withStyle(style -> style.withColor(textColor));
        guiGraphics.drawString(nn(this.font), nn(line5), 10, 95, textColor, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        refreshTimer++;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            updateButtonStates();
        }
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

    public void refreshButtonStates() {
        updateButtonStates();
    }

    private void updateButtonStates() {
        MedicalBuildingConfig medicalConfig = resolveMedicalConfig();
        boolean medical = medicalConfig != null;
        boolean hasDoctor = OtherControlBoxClientData.hasHiredDoctor(controlBoxPos);

        if (hireDoctorButton != null) {
            hireDoctorButton.active = medical && !hasDoctor;
        }
        if (fireDoctorButton != null) {
            fireDoctorButton.active = medical && hasDoctor;
            Component buttonMessage;
            if (hasDoctor) {
                buttonMessage = Component.translatable("gui.button.fire_employee_with_name", getDoctorDisplayName());
            } else {
                buttonMessage = Component.translatable("gui.button.fire_employee");
            }
            fireDoctorButton.setMessage(nn(buttonMessage));
        }
    }

    private void handleFireDoctor() {
        java.util.UUID doctorUuid = OtherControlBoxClientData.getHiredDoctorUuid(controlBoxPos);
        if (doctorUuid == null) {
            return;
        }
        NetworkManager.INSTANCE.sendToServer(EmploymentCommandPacket.fireByNpc(doctorUuid));
        OtherControlBoxClientData.clearHiredDoctor(controlBoxPos);
        updateButtonStates();
    }

    @Nullable
    private MedicalBuildingConfig resolveMedicalConfig() {
        return OtherControlBoxClientData.getMedicalConfig(controlBoxPos, getEffectiveBuildingFileName());
    }

    @Nullable
    private String getEffectiveBuildingFileName() {
        String syncedName = OtherControlBoxClientData.getBuildingFileName(controlBoxPos);
        if (syncedName != null && !syncedName.isBlank()) {
            return syncedName;
        }
        return buildingFileName;
    }

    private String getDoctorDisplayName() {
        var doctor = OtherControlBoxClientData.getHiredDoctor(controlBoxPos);
        if (doctor != null) {
            return doctor.getFullName();
        }
        return Component.translatable(
                OtherControlBoxClientData.hasHiredDoctor(controlBoxPos)
                        ? "gui.control_panel.employee.hired_not_found"
                        : "gui.control_panel.employee.none"
        ).getString();
    }
}
