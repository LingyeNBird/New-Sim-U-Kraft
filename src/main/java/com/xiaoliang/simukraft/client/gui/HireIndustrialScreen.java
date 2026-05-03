package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestIdleNPCsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class HireIndustrialScreen extends AbstractHireScreen {
    private final BlockPos controlBoxPos;
    private final String jobType;
    private final String buildingFileName;
    private final BlockPos lastPlayerPos;
    private long lastCheckTime = 0;

    public HireIndustrialScreen(BlockPos controlBoxPos, String jobType, String buildingFileName) {
        super(Component.translatable("gui.hire_industrial.title"));
        this.controlBoxPos = controlBoxPos;
        this.jobType = jobType;
        this.buildingFileName = buildingFileName;
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

        // 检查是否已经雇佣了员工
        boolean hasHiredEmployee = IndustrialClientData.hasHiredEmployee(controlBoxPos);

        // 设置确认按钮的文本（根据 jobType）
        String buttonTextKey;
        if ("shepherd".equals(jobType)) {
            buttonTextKey = "gui.button.hire_shepherd";
        } else if ("butcher".equals(jobType)) {
            buttonTextKey = "gui.button.hire_butcher";
        } else {
            buttonTextKey = "gui.button.hire";
        }
        
        // 更新父类创建的确认按钮的文本
        if (confirmButton != null) {
            confirmButton.setMessage(nn(Component.translatable(buttonTextKey)));
        }

        // 设置按钮激活状态
        confirmButton.active = !hasHiredEmployee;

        NetworkManager.INSTANCE.sendToServer(new RequestIdleNPCsPacket());

        // 根据雇佣状态设置提示信息
        if (hasHiredEmployee) {
            String messageKey;
            if ("shepherd".equals(jobType)) {
                messageKey = "message.simukraft.already_hired_shepherd";
            } else if ("butcher".equals(jobType)) {
                messageKey = "message.simukraft.already_hired_butcher";
            } else {
                messageKey = "message.simukraft.already_hired_employee";
            }
            this.statusText = nn(Component.translatable(messageKey).withStyle(style -> style.withColor(0xFF5555)));
        } else {
            this.statusText = nn(Component.translatable("message.simukraft.loading_npcs"));
        }
    }

    @Override
    protected void confirmSelection() {
        if (selectedNPCId != null) {
            // 再次检查是否已经雇佣了员工
            if (IndustrialClientData.hasHiredEmployee(controlBoxPos)) {
                String messageKey;
                if ("shepherd".equals(jobType)) {
                    messageKey = "message.simukraft.wool_farm_already_has_shepherd";
                } else if ("butcher".equals(jobType)) {
                    messageKey = "message.simukraft.beef_farm_already_has_butcher";
                } else {
                    messageKey = "message.simukraft.industrial_already_has_employee";
                }
                player().displayClientMessage(
                        nn(Component.translatable(messageKey).withStyle(ChatFormatting.RED)),
                        true
                );
                return;
            }

            // 使用v2命令包发送雇佣请求
            String dimensionId = level().dimension().location().toString();
            String workBlockHint = resolveWorkBlockHint(jobType);
            NetworkManager.INSTANCE.sendToServer(
                    EmploymentCommandPacket.hire(selectedNPCId, controlBoxPos, workBlockHint, jobType, dimensionId)
            );

            // 本地缓存预更新（乐观更新）
            CustomEntity npc = findNpcEntity(selectedNPCId);
            if (npc != null) {
                IndustrialClientData.setHiredEmployee(controlBoxPos, npc, jobType);
            } else {
                IndustrialClientData.setHiredEmployee(controlBoxPos, selectedNPCId, jobType);
            }

            // 返回到工业控制盒LDLib界面
            minecraft().setScreen(new IndustrialControlBoxLDLibScreen(controlBoxPos, buildingFileName));
        }
    }

    /**
     * 根据职业类型解析工作方块提示
     */
    private String resolveWorkBlockHint(String jobType) {
        return switch (jobType) {
            case "shepherd" -> "wool_farm";
            case "butcher" -> "beef_farm";
            default -> "industrial";
        };
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
        // 返回 IndustrialControlBoxLDLibScreen
        minecraft().setScreen(new IndustrialControlBoxLDLibScreen(controlBoxPos, buildingFileName));
    }
}
