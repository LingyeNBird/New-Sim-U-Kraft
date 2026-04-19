package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.building.IndustrialBuildingConfig;
import com.xiaoliang.simukraft.building.IndustrialBuildingManager;
import com.xiaoliang.simukraft.building.RecipeConfig;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.SelectRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IndustrialControlBoxScreen extends Screen {
    private final BlockPos controlBoxPos;
    private final String buildingFileName;
    private Button hireEmployeeButton;
    private Button fireEmployeeButton;
    private CycleButton<String> recipeButton;

    // 配方相关
    private List<RecipeConfig> availableRecipes = new ArrayList<>();
    private String currentRecipeId = null;
    private boolean isMultiRecipe = false;
    private IndustrialBuildingConfig buildingConfig = null;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    public IndustrialControlBoxScreen(BlockPos pos, String buildingFileName) {
        super(Component.translatable("gui.industrial_control_box.title"));
        this.controlBoxPos = pos;
        this.buildingFileName = buildingFileName;
        // 播放建筑盒打开界面音效
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.BUILD_BOX_OPEN.get()), 1.0F)));

        // 请求服务器同步雇佣状态
        com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(
            new com.xiaoliang.simukraft.network.RequestWorkBlockHireStatusPacket(pos, "industrial")
        );

        // 加载建筑配置
        loadBuildingConfig();
    }

    /**
     * 加载建筑配置和配方信息
     */
    private void loadBuildingConfig() {
        String effectiveFileName = getEffectiveBuildingFileName();
        if (effectiveFileName != null && !effectiveFileName.isEmpty()) {
            buildingConfig = IndustrialBuildingManager.getConfig(effectiveFileName);
            if (buildingConfig != null) {
                availableRecipes = buildingConfig.getRecipes();
                isMultiRecipe = buildingConfig.isMultiRecipe();

                // 如果没有选择配方，使用默认配方
                if (currentRecipeId == null && isMultiRecipe && !availableRecipes.isEmpty()) {
                    currentRecipeId = availableRecipes.get(0).getRecipeId();
                }
            }
        }
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

        // 获取职业类型 - 优先从客户端数据获取，如果没有则从建筑数据文件读取
        String jobType = IndustrialClientData.getJobType(controlBoxPos);
        if (jobType == null) {
            // 从建筑数据文件读取职业类型
            jobType = readJobTypeFromBuildingFile();
        }
        if (jobType == null) {
            jobType = "worker"; // 默认职业类型
        }

        // 根据职业类型设置雇佣按钮文本
        String hireButtonKey;
        if ("shepherd".equals(jobType)) {
            hireButtonKey = "gui.button.hire_shepherd";
        } else if ("butcher".equals(jobType)) {
            hireButtonKey = "gui.button.hire_butcher";
        } else {
            hireButtonKey = "gui.button.hire_employee";
        }

        // 雇佣员工按钮 - 左下角，长一点的按钮
        String finalJobType = jobType;
        hireEmployeeButton = nn(Button.builder(
                        nn(Component.translatable(hireButtonKey)),
                        button -> {
                            // 切换到雇佣界面
                            Minecraft.getInstance().setScreen(new HireIndustrialScreen(controlBoxPos, finalJobType, buildingFileName));
                        })
                .bounds(5, this.height - 50, 80, 20)  // 左下角，宽度80像素
                .build());
        this.addRenderableWidget(hireEmployeeButton);

        // 解雇员工按钮 - 在雇佣员工按钮下方
        fireEmployeeButton = nn(Button.builder(
                        nn(Component.translatable("gui.button.fire_employee")),
                        button -> {
                            handleFireEmployee();
                        })
                .bounds(5, this.height - 25, 80, 20)  // 在雇佣员工按钮下方
                .build());
        this.addRenderableWidget(fireEmployeeButton);

        // 配方选择按钮（仅多配方建筑显示）
        if (isMultiRecipe && !availableRecipes.isEmpty()) {
            createRecipeButton();
        }

        // 更新按钮状态
        updateButtonStates();
    }

    /**
     * 创建配方选择按钮
     */
    private void createRecipeButton() {
        // 构建配方名称列表
        List<String> recipeNames = new ArrayList<>();
        List<String> recipeIds = new ArrayList<>();

        for (RecipeConfig recipe : availableRecipes) {
            recipeNames.add(recipe.getRecipeName());
            recipeIds.add(recipe.getRecipeId());
        }

        // 找到当前配方索引
        int currentIndex = 0;
        if (currentRecipeId != null) {
            for (int i = 0; i < recipeIds.size(); i++) {
                if (recipeIds.get(i).equals(currentRecipeId)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        // 创建配方选择按钮
        final List<String> finalRecipeIds = recipeIds;
        final List<String> finalRecipeNames = recipeNames;
        final int[] selectedIndex = {currentIndex};

        recipeButton = nn(CycleButton.<String>builder(recipeName -> Component.literal(safeString(recipeName)))
                .withValues(recipeNames)
                .withInitialValue(safeString(recipeNames.get(currentIndex)))
                .create(100, this.height - 50, 100, 20,
                        nn(Component.translatable("gui.industrial.recipe")),
                        (button, recipeName) -> {
                            int index = finalRecipeNames.indexOf(recipeName);
                            if (index >= 0 && index < finalRecipeIds.size()) {
                                selectedIndex[0] = index;
                                String newRecipeId = finalRecipeIds.get(index);
                                if (!newRecipeId.equals(currentRecipeId)) {
                                    currentRecipeId = newRecipeId;
                                    // 发送配方选择到服务器
                                    NetworkManager.INSTANCE.sendToServer(
                                        new SelectRecipePacket(controlBoxPos, currentRecipeId, buildingFileName)
                                    );
                                }
                            }
                        }));

        this.addRenderableWidget(recipeButton);
    }

    private void updateButtonStates() {
        boolean hasHiredEmployee = IndustrialClientData.hasHiredEmployee(controlBoxPos);

        // 如果已经雇佣了员工，雇佣按钮变为黑色不可点击，解雇按钮正常
        hireEmployeeButton.active = !hasHiredEmployee;
        fireEmployeeButton.active = hasHiredEmployee;

        // 设置按钮颜色
        if (hasHiredEmployee) {
            hireEmployeeButton.setMessage(nn(Component.translatable("gui.button.hire_employee").withStyle(style -> style.withColor(0x666666))));
            CustomEntity npc = IndustrialClientData.getHiredEmployee(controlBoxPos);
            if (npc != null) {
                fireEmployeeButton.setMessage(nn(Component.translatable("gui.button.fire_employee_with_name", npc.getFullName())));
            }
        } else {
            String jobType = IndustrialClientData.getJobType(controlBoxPos);
            String hireButtonKey;
            if ("shepherd".equals(jobType)) {
                hireButtonKey = "gui.button.hire_shepherd";
            } else if ("butcher".equals(jobType)) {
                hireButtonKey = "gui.button.hire_butcher";
            } else {
                hireButtonKey = "gui.button.hire_employee";
            }
            hireEmployeeButton.setMessage(nn(Component.translatable(hireButtonKey).withStyle(style -> style.withColor(0xFFFFFF))));
            fireEmployeeButton.setMessage(nn(Component.translatable("gui.button.fire_employee")));
        }
    }

    private void handleFireEmployee() {
        if (IndustrialClientData.hasHiredEmployee(controlBoxPos)) {
            CustomEntity npc = IndustrialClientData.getHiredEmployee(controlBoxPos);
            java.util.UUID npcUuid = npc != null ? npc.getUUID() : IndustrialClientData.getHiredEmployeeUUID(controlBoxPos);

            if (npcUuid != null) {
                NetworkManager.INSTANCE.sendToServer(EmploymentCommandPacket.fireByNpc(npcUuid));

                IndustrialClientData.clearHiredEmployee(controlBoxPos);
                updateButtonStates();

                // 不再在客户端显示解雇消息，改为由服务器统一发送
            }
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 黑色半透明背景 - 与建筑盒相同的背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC8000000, 0xC8000000);

        // 白色标题：建筑控制面板
        int titleColor = 0xFFFFFF; // 白色
        Component title = nn(Component.translatable("gui.control_panel.title").withStyle(style -> style.withColor(titleColor)));
        guiGraphics.drawCenteredString(nn(this.font), title, this.width / 2, 10, titleColor);

        // 黄色文字内容
        int textColor = 0xFFF5F5A0; // 黄色

        // 第一行：建筑信息 - 从JSON配置文件中获取建筑名称
        String buildingName = getBuildingNameFromConfig();
        Component line1 = nn(Component.translatable("gui.control_panel.building.name_format", safeString(buildingName)).withStyle(style -> style.withColor(textColor)));
        guiGraphics.drawString(nn(this.font), line1, 10, 35, textColor, false);

        // 第二行：类型：工业类（可用建筑）
        Component line2 = nn(Component.translatable("gui.control_panel.type.industrial").withStyle(style -> style.withColor(textColor)));
        guiGraphics.drawString(nn(this.font), line2, 10, 50, textColor, false);

        // 第三行：员工状态
        Component line3;
        if (IndustrialClientData.hasHiredEmployee(controlBoxPos)) {
            CustomEntity npc = IndustrialClientData.getHiredEmployee(controlBoxPos);
            if (npc != null) {
                line3 = nn(Component.translatable("gui.control_panel.employee.hired", npc.getFullName()).withStyle(style -> style.withColor(textColor)));
            } else {
                line3 = nn(Component.translatable("gui.control_panel.employee.hired_not_found").withStyle(style -> style.withColor(textColor)));
            }
        } else {
            line3 = nn(Component.translatable("gui.control_panel.employee.none").withStyle(style -> style.withColor(textColor)));
        }
        guiGraphics.drawString(nn(this.font), line3, 10, 65, textColor, false);

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
     * 获取建筑文件名
     */
    public String getBuildingFileName() {
        return buildingFileName;
    }

    /**
     * 刷新按钮状态（用于服务器同步后刷新界面）
     */
    public void refreshButtonStates() {
        updateButtonStates();
    }

    /**
     * 更新配方信息（从服务器同步）
     */
    public void updateRecipeInfo(String recipeId, boolean multiRecipe) {
        this.currentRecipeId = recipeId;
        this.isMultiRecipe = multiRecipe;

        // 重新加载建筑配置
        loadBuildingConfig();

        // 如果配方按钮已存在，更新其状态
        if (recipeButton != null && isMultiRecipe && !availableRecipes.isEmpty()) {
            // 找到当前配方对应的名称
            for (RecipeConfig recipe : availableRecipes) {
                if (recipe.getRecipeId().equals(recipeId)) {
                    // 更新按钮显示
                    break;
                }
            }
        }
    }

    /**
     * 从建筑数据文件读取职业类型
     */
    private String readJobTypeFromBuildingFile() {
        try {
            var minecraft = Minecraft.getInstance();
            return com.xiaoliang.simukraft.client.utils.ClientFileUtils.readIndustrialJobTypeClient(
                minecraft, controlBoxPos);
        } catch (Exception e) {
            System.err.println("IndustrialControlBoxScreen: 无法从建筑数据文件读取职业类型: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取有效的建筑文件名（优先使用从服务器同步的）
     */
    private String getEffectiveBuildingFileName() {
        // 优先使用从服务器同步的建筑文件名（多人游戏）
        String syncedName = IndustrialClientData.getBuildingFileName(controlBoxPos);
        if (syncedName != null && !syncedName.isEmpty()) {
            return syncedName;
        }
        // 其次使用从本地文件读取的建筑文件名（单人游戏）
        if (buildingFileName != null && !buildingFileName.isEmpty() && !"unknown".equals(buildingFileName)) {
            return buildingFileName;
        }
        return null;
    }

    /**
     * 从JSON配置文件获取建筑名称
     */
    private String getBuildingNameFromConfig() {
        String effectiveFileName = getEffectiveBuildingFileName();
        if (effectiveFileName == null || effectiveFileName.isEmpty()) {
            return Component.translatable("gui.control_panel.building.industrial_default").getString();
        }

        try {
            // 从IndustrialBuildingManager获取配置
            com.xiaoliang.simukraft.building.IndustrialBuildingConfig config =
                com.xiaoliang.simukraft.building.IndustrialBuildingManager.getConfig(effectiveFileName);
            if (config != null && config.getBuildingName() != null && !config.getBuildingName().isEmpty()) {
                return config.getBuildingName();
            }
        } catch (Exception e) {
            System.err.println("IndustrialControlBoxScreen: 无法从配置文件获取建筑名称: " + e.getMessage());
        }

        // 如果无法获取配置，返回建筑文件名作为后备
        return effectiveFileName;
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
