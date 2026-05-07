package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class HireDoctorScreen extends AbstractHireScreen {
    private final BlockPos controlBoxPos;
    private final String buildingFileName;
    private final BlockPos lastPlayerPos;
    private long lastCheckTime = 0L;

    public HireDoctorScreen(BlockPos controlBoxPos, String buildingFileName) {
        super(Component.translatable("gui.hire_doctor.title"));
        this.controlBoxPos = controlBoxPos;
        this.buildingFileName = buildingFileName;
        this.lastPlayerPos = player().blockPosition();
    }

    @Override
    public void tick() {
        super.tick();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime <= 100L) {
            return;
        }
        lastCheckTime = currentTime;
        if (!player().blockPosition().equals(lastPlayerPos)) {
            player().displayClientMessage(
                    nn(Component.translatable("message.simukraft.must_stand_still").withStyle(style -> style.withColor(0xFF0000))),
                    false
            );
            onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (confirmButton != null) {
            confirmButton.setMessage(nn(Component.translatable("gui.button.hire_employee_with_job", "医生")));
            confirmButton.active = !OtherControlBoxClientData.hasHiredDoctor(controlBoxPos);
        }
        if (OtherControlBoxClientData.hasHiredDoctor(controlBoxPos)) {
            this.statusText = nn(Component.translatable("message.simukraft.already_hired_employee_with_job", "医生")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId == null) {
            return;
        }
        if (OtherControlBoxClientData.hasHiredDoctor(controlBoxPos)) {
            player().displayClientMessage(
                    nn(Component.translatable("message.simukraft.commercial_already_has_employee_with_job", "医生")
                            .withStyle(ChatFormatting.RED)),
                    true
            );
            return;
        }

        String dimensionId = level().dimension().location().toString();
        NetworkManager.INSTANCE.sendToServer(
                EmploymentCommandPacket.hire(selectedNPCId, controlBoxPos, "other_control_box", "doctor", dimensionId)
        );

        CustomEntity npc = findNpcEntity(selectedNPCId);
        OtherControlBoxClientData.setHiredDoctor(controlBoxPos, npc != null ? npc.getUUID() : selectedNPCId);
        onClose();
    }

    private CustomEntity findNpcEntity(UUID npcUuid) {
        for (var entity : level().entitiesForRendering()) {
            if (entity instanceof CustomEntity customEntity && npcUuid.equals(customEntity.getUUID())) {
                return customEntity;
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        minecraft().setScreen(new OtherControlBoxScreen(controlBoxPos, buildingFileName));
    }
}
