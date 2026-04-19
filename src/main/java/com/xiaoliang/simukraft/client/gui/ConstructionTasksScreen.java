package com.xiaoliang.simukraft.client.gui;

import com.xiaoliang.simukraft.init.ModSoundEvents;
import com.xiaoliang.simukraft.network.ConstructionTasksRequestPacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstructionTasksScreen extends AbstractTransitionScreen {
    private final BlockPos cityCorePos;
    private List<ConstructionTaskInfo> tasks = new ArrayList<>();
    private int currentPage = 0;
    private static final int TASKS_PER_PAGE = 5;

    // 自动刷新相关
    private int refreshTimer = 0;
    private static final int REFRESH_INTERVAL = 10; // 0.5秒（10 ticks = 0.5秒）

    // 经验条纹理位置（原版GUI纹理）
    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");

    public ConstructionTasksScreen(BlockPos cityCorePos) {
        super(Component.translatable("gui.construction_tasks.title"));
        this.cityCorePos = cityCorePos;
        // 添加音效播放
        Minecraft.getInstance().getSoundManager().play(nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.CITY_CORE_OPEN.get()), 1.0F)));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // 创建返回按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.translatable("gui.back")),
            button -> this.closeScreen()
        ).pos(centerX - 50, this.height - 30).size(100, 20).build()));

        // 创建上一页按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.literal("<")),
            button -> prevPage()
        ).pos(centerX - 100, this.height - 30).size(30, 20).build()));

        // 创建下一页按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.literal(">")),
            button -> nextPage()
        ).pos(centerX + 70, this.height - 30).size(30, 20).build()));

        // 创建材料需求按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.translatable("gui.construction_tasks.material_requirements")),
            button -> this.openMaterialRequirements()
        ).pos(this.width - 130, 20).size(110, 20).build()));

        // 请求建造任务数据
        requestTasksData();
    }

    private void requestTasksData() {
        // 发送网络包请求建造任务数据
        NetworkManager.INSTANCE.sendToServer(new ConstructionTasksRequestPacket(cityCorePos));
    }

    public void setTasks(List<ConstructionTaskInfo> tasks) {
        this.tasks = tasks;
        // 保持当前页码，但如果超出范围则调整到最后页
        int maxPage = Math.max(0, (tasks.size() - 1) / TASKS_PER_PAGE);
        if (currentPage > maxPage) {
            currentPage = maxPage;
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    private void nextPage() {
        int maxPage = (tasks.size() - 1) / TASKS_PER_PAGE;
        if (currentPage < maxPage) {
            currentPage++;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int startY = 50;
        int lineHeight = 45;

        // 渲染标题
        Component title = Component.translatable("gui.construction_tasks.title");
        int titleWidth = this.font.width(nn(title));
        guiGraphics.drawString(nn(this.font), nn(title), centerX - titleWidth / 2, 20, 0xFFFFFF);

        // 渲染任务列表
        if (tasks.isEmpty()) {
            Component emptyText = Component.translatable("gui.construction_tasks.empty");
            int emptyWidth = this.font.width(nn(emptyText));
            guiGraphics.drawString(nn(this.font), nn(emptyText), centerX - emptyWidth / 2, startY + 50, 0x888888);
        } else {
            int startIndex = currentPage * TASKS_PER_PAGE;
            int endIndex = Math.min(startIndex + TASKS_PER_PAGE, tasks.size());

            for (int i = startIndex; i < endIndex; i++) {
                ConstructionTaskInfo task = tasks.get(i);
                int y = startY + (i - startIndex) * lineHeight;
                renderTask(guiGraphics, centerX, y, task);
            }

            // 渲染页码
            int maxPage = (tasks.size() - 1) / TASKS_PER_PAGE + 1;
            Component pageText = Component.literal((currentPage + 1) + " / " + maxPage);
            int pageWidth = this.font.width(nn(pageText));
            guiGraphics.drawString(nn(this.font), nn(pageText), centerX - pageWidth / 2, this.height - 25, 0xAAAAAA);
        }
    }

    private void renderTask(GuiGraphics guiGraphics, int centerX, int y, ConstructionTaskInfo task) {
        int barWidth = 200;
        int barHeight = 10;
        int barX = centerX - barWidth / 2;
        int barY = y + 20;

        // 渲染建筑名称
        Component nameText = Component.literal(nn(task.getBuildingName()));
        guiGraphics.drawString(nn(this.font), nn(nameText), centerX - barWidth / 2, y, 0xFFFFFF);

        // 渲染建造者名称
        Component builderText = Component.translatable("gui.construction_tasks.builder", nn(task.getBuilderName()));
        guiGraphics.drawString(nn(this.font), nn(builderText), centerX - barWidth / 2 + 120, y, 0xAAAAAA);

        // 渲染经验条（作为进度条）
        renderExperienceBar(guiGraphics, barX, barY, barWidth, barHeight, task.getProgress());

        // 渲染进度百分比
        Component progressText = Component.literal(task.getProgress() + "%");
        guiGraphics.drawString(nn(this.font), nn(progressText), barX + barWidth + 5, barY, 0xFFFFFF);

        // 渲染状态
        Component status = task.isCompleted()
            ? Component.translatable("gui.construction_tasks.status.completed").withStyle(style -> style.withColor(0x55FF55))
            : Component.translatable("gui.construction_tasks.status.working").withStyle(style -> style.withColor(0xFFFF55));
        guiGraphics.drawString(nn(this.font), nn(status), barX + barWidth + 40, barY, 0xFFFFFF);
    }

    /**
     * 渲染经验条作为进度条
     */
    private void renderExperienceBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int progress) {
        // 绑定原版GUI纹理
        guiGraphics.blit(nn(GUI_ICONS_LOCATION), x, y, 0, 64, width, 5); // 背景

        // 计算进度宽度
        int progressWidth = (int) (width * (progress / 100.0));
        if (progressWidth > 0) {
            guiGraphics.blit(nn(GUI_ICONS_LOCATION), x, y, 0, 69, progressWidth, 5); // 进度
        }
    }

    private void openMaterialRequirements() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new MaterialRequirementsScreen(cityCorePos));
        }
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new CityManagementScreen(cityCorePos));
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 自动刷新计时器
        refreshTimer++;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            requestTasksData(); // 请求最新数据
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    /**
     * 建造任务信息类
     */
    public static class ConstructionTaskInfo {
        private final String buildingName;
        private final String builderName;
        private final int progress;
        private final boolean completed;

        public ConstructionTaskInfo(String buildingName, String builderName, int progress, boolean completed) {
            this.buildingName = buildingName;
            this.builderName = builderName;
            this.progress = progress;
            this.completed = completed;
        }

        public String getBuildingName() { return buildingName; }
        public String getBuilderName() { return builderName; }
        public int getProgress() { return progress; }
        public boolean isCompleted() { return completed; }
    }
}
