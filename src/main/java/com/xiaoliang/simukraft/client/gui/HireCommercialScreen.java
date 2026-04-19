package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class HireCommercialScreen extends AbstractHireScreen {
    private final BlockPos controlBoxPos;
    private final String jobType;
    private final String buildingFileName;
    private final BlockPos lastPlayerPos;
    private long lastCheckTime = 0;

    public HireCommercialScreen(BlockPos controlBoxPos, String jobType, String buildingFileName) {
        super(Component.translatable("gui.hire_commercial.title"));
        this.controlBoxPos = controlBoxPos;
        this.jobType = jobType;
        this.buildingFileName = buildingFileName;
        this.lastPlayerPos = player().blockPosition();
    }

    @Override
    public void tick() {
        super.tick();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime > 100) {
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

        boolean hasHiredEmployee = CommercialClientData.hasHiredEmployee(controlBoxPos);

        // 从配置获取职业名称，支持自定义职业
        String jobName = CommercialClientData.getJobNameByJobType(jobType, buildingFileName);
        if (jobName == null || jobName.isEmpty()) {
            jobName = jobType; // 如果配置中没有，使用 jobType 作为后备
        }

        if (confirmButton != null) {
            confirmButton.setMessage(nn(Component.translatable("gui.button.hire_employee_with_job", jobName)));
        }

        confirmButton.active = !hasHiredEmployee;

        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());

        if (hasHiredEmployee) {
            this.statusText = nn(Component.translatable("message.simukraft.already_hired_employee_with_job", jobName)
                    .withStyle(style -> style.withColor(0xFF5555)));
        } else {
            this.statusText = nn(Component.translatable("message.simukraft.loading_npcs"));
        }
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId != null) {
            if (CommercialClientData.hasHiredEmployee(controlBoxPos)) {
                // 从配置获取职业名称，支持自定义职业
                String jobName = CommercialClientData.getJobNameByJobType(jobType, buildingFileName);
                if (jobName == null || jobName.isEmpty()) {
                    jobName = jobType;
                }
                player().displayClientMessage(
                        nn(Component.translatable("message.simukraft.commercial_already_has_employee_with_job", jobName)
                                .withStyle(ChatFormatting.RED)),
                        true
                );
                return;
            }

            // 使用v2命令包发送雇佣请求
            String dimensionId = level().dimension().location().toString();
            String workBlockHint = resolveWorkBlockHint();
            NetworkManager.INSTANCE.sendToServer(
                    EmploymentCommandPacket.hire(selectedNPCId, controlBoxPos, workBlockHint, jobType, dimensionId)
            );

            // 本地缓存预更新（乐观更新）
            CustomEntity npc = findNpcEntity(selectedNPCId);
            if (npc != null) {
                CommercialClientData.setHiredEmployee(controlBoxPos, npc, jobType);
            } else {
                CommercialClientData.setHiredEmployee(controlBoxPos, selectedNPCId, jobType);
            }

            onClose();
        }
    }

    /**
     * 根据建筑配置解析工作方块提示
     */
    private String resolveWorkBlockHint() {
        // 从配置获取工作方块提示
        com.xiaoliang.simukraft.building.CommercialBuildingConfig config = 
            CommercialClientData.getConfig(buildingFileName);
        if (config != null) {
            // 如果配置中有自定义的工作方块提示，使用它
            String customHint = config.getWorkBlockHint();
            if (customHint != null && !customHint.isEmpty()) {
                return customHint;
            }
        }
        // 默认使用通用商业建筑提示
        return "commercial";
    }

    /**
     * 根据UUID查找NPC实体
     */
    private CustomEntity findNpcEntity(UUID npcUuid) {
        for (var entity : level().entitiesForRendering()) {
            if (entity instanceof CustomEntity && entity.getUUID().equals(npcUuid)) {
                return (CustomEntity) entity;
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        minecraft().setScreen(new CommercialControlBoxScreen(controlBoxPos, buildingFileName));
    }
}
