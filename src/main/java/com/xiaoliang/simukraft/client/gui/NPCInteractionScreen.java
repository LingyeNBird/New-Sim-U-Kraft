package com.xiaoliang.simukraft.client.gui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.xiaoliang.simukraft.client.CityNameCache;
import com.xiaoliang.simukraft.client.NPCResidenceCache;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.Gender;
import com.xiaoliang.simukraft.network.GetCityNamePacket;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.network.RequestNPCResidencePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class NPCInteractionScreen extends ModularUIGuiContainer {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("simukraft", "textures/gui/npc_background.png");
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 250;

    private final CustomEntity npc;

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }

    public NPCInteractionScreen(CustomEntity npc) {
        super(createModularUI(npc), 0);
        this.npc = npc;

        loadCityName();
        requestResidenceInfo();
    }

    private static ModularUI createModularUI(CustomEntity npc) {
        // 将数据保存到NPC的NBT中以便后续更新
        npc.getPersistentData().putString("_temp_city_ref", "loading");
        npc.getPersistentData().putString("_temp_residence_ref", "loading");

        Player player = Minecraft.getInstance().player;
        NPCUIHolder holder = new NPCUIHolder();
        ModularUI modularUI = new ModularUI(new Size(BACKGROUND_WIDTH, BACKGROUND_HEIGHT), holder, player);

        // 创建根容器
        WidgetGroup rootGroup = new WidgetGroup();
        rootGroup.setSize(BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        rootGroup.setBackground(new ResourceTexture(BACKGROUND.toString()));

        // 标题 - 使用 ImageWidget 配合 TextTexture 实现居中
        TextTexture titleTexture = new TextTexture("gui.npc_interaction.greeting");
        titleTexture.setWidth(BACKGROUND_WIDTH);
        ImageWidget titleWidget = new ImageWidget(0, 10, BACKGROUND_WIDTH, 20, titleTexture);
        rootGroup.addWidget(titleWidget);

        // 信息区域起始位置
        int labelX = 20;
        int startY = 40;
        int lineHeight = 20;
        int valueOffsetX = 80;

        // 姓名
        createInfoRow(rootGroup, "name", labelX, startY, valueOffsetX,
                npc::getFullName);

        // 性别
        createInfoRow(rootGroup, "gender", labelX, startY + lineHeight, valueOffsetX,
                () -> {
                    Gender gender = npc.getGender();
                    String genderKey = gender != null ? gender.name().toLowerCase() : "unknown";
                    return Component.translatable("gui.npc.gender." + genderKey).getString();
                });

        // 工作状态
        createInfoRow(rootGroup, "work_status", labelX, startY + lineHeight * 2, valueOffsetX,
                () -> npc.getStatusDisplayComponent().getString());

        // 职业
        createInfoRow(rootGroup, "job", labelX, startY + lineHeight * 3, valueOffsetX,
                () -> {
                    String job = npc.getJob();
                    // 先检查工业建筑配置
                    String industrialJobName = IndustrialClientData.getJobNameByJobType(job);
                    if (industrialJobName != null) {
                        return industrialJobName;
                    }
                    // 再检查商业建筑配置
                    String commercialJobName = CommercialClientData.getJobNameByJobType(job);
                    if (commercialJobName != null) {
                        return commercialJobName;
                    }
                    return Component.translatable("job." + job).getString();
                });

        // 年龄
        createInfoRow(rootGroup, "age", labelX, startY + lineHeight * 4, valueOffsetX,
                () -> npc.getNpcAge() + Component.translatable("gui.npc_interaction.age_unit").getString());

        // 健康状态
        createInfoRow(rootGroup, "health", labelX, startY + lineHeight * 5, valueOffsetX,
                () -> {
                    boolean isSick = npc.isSick();
                    return Component.translatable(isSick ? "gui.npc_interaction.sick" : "gui.npc_interaction.healthy").getString();
                });

        // 城市 - 从NPC的NBT数据读取
        createInfoRow(rootGroup, "city", labelX, startY + lineHeight * 6, valueOffsetX,
                () -> {
                    String cityData = npc.getPersistentData().getString("_temp_city_ref");
                    if (!cityData.equals("loading")) {
                        return cityData;
                    }
                    return Component.translatable("gui.city.loading").getString();
                });
        
        // 居住情况 - 从NPC的NBT数据读取
        createInfoRow(rootGroup, "residence", labelX, startY + lineHeight * 7, valueOffsetX,
                () -> {
                    String residenceData = npc.getPersistentData().getString("_temp_residence_ref");
                    if (!residenceData.equals("loading")) {
                        return residenceData;
                    }
                    return Component.translatable("gui.npc_interaction.loading").getString();
                });

        // 再见按钮 - 使用标准 LDLib 按钮样式
        ButtonWidget goodbyeButton = new ButtonWidget();
        goodbyeButton.setSelfPosition(BACKGROUND_WIDTH - 70, BACKGROUND_HEIGHT - 30);
        goodbyeButton.setSize(60, 20);
        
        // 准备按钮纹理 - 使用自定义纹理（直接拉伸）
        var buttonBackground = new ResourceTexture("simukraft:textures/gui/button.png");
        var buttonHover = new ResourceTexture("simukraft:textures/gui/button.png").setColor(0xFFAAAAAA); // 悬停时变亮
        var buttonText = new TextTexture("gui.npc_interaction.goodbye");

        goodbyeButton.setButtonTexture(buttonBackground, buttonText);
        goodbyeButton.setHoverTexture(buttonHover, buttonText);
        goodbyeButton.setOnPressCallback(clickData -> {
            var currentScreen = Minecraft.getInstance().screen;
            if (currentScreen != null) {
                currentScreen.onClose();
            }
        });
        
        rootGroup.addWidget(goodbyeButton);

        modularUI.widget(rootGroup);
        modularUI.initWidgets();
        
        return modularUI;
    }

    /**
     * 创建信息行 - 标签 + 值
     */
    private static LabelWidget createInfoRow(WidgetGroup parent, String key, int x, int y, int valueOffsetX, Supplier<String> valueSupplier) {
        // 标签
        LabelWidget label = new LabelWidget();
        label.setSelfPosition(x, y);
        label.setText("gui.npc_interaction." + key);
        label.setColor(0x000000);
        parent.addWidget(label);

        // 值 - 使用 LabelWidget 支持动态更新
        LabelWidget valueWidget = new LabelWidget();
        valueWidget.setSelfPosition(x + valueOffsetX, y);
        valueWidget.setTextProvider(valueSupplier);
        valueWidget.setColor(0x191970);
        valueWidget.setClientSideWidget(); // 设置为客户端 widget，支持动态更新
        parent.addWidget(valueWidget);
        
        return valueWidget;
    }

    private void requestResidenceInfo() {
        String npcNameStr = npc.getFullName();
        NPCResidenceCache.ResidenceInfo cachedInfo = NPCResidenceCache.getResidenceInfo(npcNameStr);
        if (cachedInfo != null) {
            updateResidenceText(cachedInfo);
            return;
        }
        NetworkManager.INSTANCE.sendToServer(new RequestNPCResidencePacket(npcNameStr));
        
        // 延迟更新
        Minecraft.getInstance().tell(() -> {
            new Thread(() -> {
                try {
                    Thread.sleep(600);
                    Minecraft.getInstance().tell(() -> {
                        NPCResidenceCache.ResidenceInfo info = NPCResidenceCache.getResidenceInfo(npcNameStr);
                        if (info != null) {
                            updateResidenceText(info);
                        } else {
                            npc.getPersistentData().putString("_temp_residence_ref",
                                safeString(Component.translatable("gui.npc_interaction.no_residence").getString()));
                        }
                    });
                } catch (InterruptedException e) {
                    // ignore
                }
            }).start();
        });
    }

    private void updateResidenceText(NPCResidenceCache.ResidenceInfo cachedInfo) {
        if (cachedInfo.hasResidence) {
            if (cachedInfo.position != null && !cachedInfo.position.isEmpty()) {
                npc.getPersistentData().putString("_temp_residence_ref",
                    safeString(Component.translatable("gui.npc_interaction.has_residence", cachedInfo.position).getString()));
            } else {
                npc.getPersistentData().putString("_temp_residence_ref",
                    safeString(Component.translatable("gui.npc_interaction.has_residence_unknown").getString()));
            }
        } else {
            npc.getPersistentData().putString("_temp_residence_ref",
                safeString("§7" + Component.translatable("gui.npc_interaction.no_residence").getString()));
        }
    }

    private void loadCityName() {
        String cityIdStr = npc.getCityIdString();
        if (cityIdStr != null && !cityIdStr.isEmpty()) {
            try {
                UUID cityId = UUID.fromString(cityIdStr);
                String cachedName = CityNameCache.get(cityId);
                if (cachedName != null) {
                    npc.getPersistentData().putString("_temp_city_ref", cachedName);
                    return;
                }
                NetworkManager.INSTANCE.sendToServer(new GetCityNamePacket(cityId));
                Minecraft.getInstance().tell(() -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Minecraft.getInstance().tell(() -> {
                                String name = CityNameCache.get(cityId);
                                if (name != null) {
                                    npc.getPersistentData().putString("_temp_city_ref", name);
                                } else {
                                    npc.getPersistentData().putString("_temp_city_ref",
                                        safeString(Component.translatable("gui.city.unknown").getString()));
                                }
                            });
                        } catch (InterruptedException e) {
                            npc.getPersistentData().putString("_temp_city_ref",
                                safeString(Component.translatable("gui.city.unknown").getString()));
                        }
                    }).start();
                });
            } catch (IllegalArgumentException e) {
                npc.getPersistentData().putString("_temp_city_ref",
                    safeString(Component.translatable("gui.city.none").getString()));
            }
        } else {
            npc.getPersistentData().putString("_temp_city_ref",
                safeString(Component.translatable("gui.city.none").getString()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class NPCUIHolder implements IUIHolder {
        @Override
        public ModularUI createUI(Player entityPlayer) {
            return null;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public void markAsDirty() {
        }
    }
}
