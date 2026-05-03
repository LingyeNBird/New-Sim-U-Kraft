package com.xiaoliang.simukraft.client.gui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.xiaoliang.simukraft.building.CommercialBuildingConfig;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 商业建筑交易选择界面 - 使用LDLib框架
 * 选择购买或出售
 */
@OnlyIn(Dist.CLIENT)
public class CommercialTradeSelectScreen extends ModularUIGuiContainer {

    // ==================== 布局常量 ====================

    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 160;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 12;

    // 颜色定义
    private static final int COLOR_WINDOW_BG = 0xFF2A2A2A;
    private static final int COLOR_WINDOW_BORDER = 0xFF555555;
    private static final int COLOR_BUTTON_BG = 0xFF3A3A3A;
    private static final int COLOR_BUTTON_HOVER = 0xFF4A4A4A;
    private static final int COLOR_BUTTON_BORDER = 0xFFADD8E6;
    private static final int COLOR_TEXT_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_HINT = 0xFFAAAAAA;

    // ==================== 成员变量 ====================

    // ==================== 构造函数 ====================

    public CommercialTradeSelectScreen(BlockPos pos, String buildingFileName) {
        super(createHolderAndUI(pos, buildingFileName), 0);

        playOpenSound();
    }

    private void playOpenSound() {
        Minecraft.getInstance().getSoundManager().play(
                nn(SimpleSoundInstance.forUI(nn(ModSoundEvents.CITY_CORE_OPEN.get()), 1.0F)));
    }

    private static ModularUI createHolderAndUI(BlockPos pos, String buildingFileName) {
        TradeSelectUIHolder holder = new TradeSelectUIHolder(pos, buildingFileName);
        return holder.createModularUI();
    }

    // ==================== UI 创建 ====================

    private static ModularUI createUI(TradeSelectUIHolder holder) {
        Minecraft mc = Minecraft.getInstance();

        ModularUI modularUI = new ModularUI(new Size(WINDOW_WIDTH, WINDOW_HEIGHT), holder, nn(mc.player));

        // 根容器 - LDLib 会自动居中，所以位置设为 (0, 0)
        WidgetGroup rootGroup = new WidgetGroup();
        rootGroup.setSelfPosition(0, 0);
        rootGroup.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        // 主窗口背景（带圆角）
        WidgetGroup windowGroup = new WidgetGroup();
        windowGroup.setSelfPosition(0, 0);
        windowGroup.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        windowGroup.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_WINDOW_BG).setRadius(8),
                new ColorBorderTexture(1, COLOR_WINDOW_BORDER).setRadius(8)
        ));
        rootGroup.addWidget(windowGroup);

        // 标题
        TextTexture titleTexture = new TextTexture(safeString(holder.buildingName), COLOR_TEXT_TITLE);
        titleTexture.setType(TextTexture.TextType.NORMAL);
        ImageWidget titleWidget = new ImageWidget(0, 12, WINDOW_WIDTH, 20, titleTexture);
        windowGroup.addWidget(titleWidget);

        // 提示文本
        TextTexture hintTexture = new TextTexture("gui.commercial_trade_select.hint", COLOR_TEXT_HINT);
        hintTexture.setType(TextTexture.TextType.NORMAL);
        ImageWidget hintWidget = new ImageWidget(0, 32, WINDOW_WIDTH, 14, hintTexture);
        windowGroup.addWidget(hintWidget);

        // 按钮起始Y位置
        int buttonStartY = 55;

        // 购买按钮
        ButtonWidget buyButton = createButton(
                (WINDOW_WIDTH - BUTTON_WIDTH) / 2,
                buttonStartY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "gui.commercial_trade_select.buy_items",
                holder.canBuy,
                clickData -> holder.onBuyButtonClick()
        );
        windowGroup.addWidget(buyButton);

        // 出售按钮
        ButtonWidget sellButton = createButton(
                (WINDOW_WIDTH - BUTTON_WIDTH) / 2,
                buttonStartY + BUTTON_HEIGHT + BUTTON_SPACING,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "gui.commercial_trade_select.sell_items",
                holder.canSell,
                clickData -> holder.onSellButtonClick()
        );
        windowGroup.addWidget(sellButton);

        // 返回按钮
        ButtonWidget backButton = createButton(
                (WINDOW_WIDTH - BUTTON_WIDTH) / 2,
                buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "gui.button.back",
                true,
                clickData -> holder.onBackButtonClick()
        );
        windowGroup.addWidget(backButton);

        modularUI.widget(rootGroup);
        modularUI.initWidgets();
        return modularUI;
    }

    private static ButtonWidget createButton(int x, int y, int width, int height,
                                              String textKey,
                                              boolean active,
                                              java.util.function.Consumer<ClickData> onPress) {
        ButtonWidget button = new ButtonWidget();
        button.setSelfPosition(x, y);
        button.setSize(width, height);

        TextTexture buttonText = new TextTexture(textKey, 0xFFFFFFFF);
        buttonText.setType(TextTexture.TextType.NORMAL);

        button.setButtonTexture(
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BUTTON_BG).setRadius(4),
                        new ColorBorderTexture(1, COLOR_WINDOW_BORDER).setRadius(4)
                ),
                buttonText
        );
        button.setHoverTexture(
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BUTTON_HOVER).setRadius(4),
                        new ColorBorderTexture(1, COLOR_BUTTON_BORDER).setRadius(4)
                ),
                buttonText
        );
        button.setOnPressCallback(onPress);
        button.setActive(active);

        return button;
    }

    @Nonnull
    private static <T> T nn(T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(String value) {
        return nn(value);
    }

    // ==================== UI Holder 类 ====================

    public static class TradeSelectUIHolder implements IUIHolder {
        private final BlockPos controlBoxPos;
        private final String buildingFileName;
        private final String buildingName;
        private final boolean canBuy;
        private final boolean canSell;

        public TradeSelectUIHolder(BlockPos pos, String buildingFileName) {
            this.controlBoxPos = pos;
            this.buildingFileName = buildingFileName;

            CommercialBuildingConfig config = CommercialClientData.getConfig(buildingFileName);
            if (config != null) {
                this.buildingName = config.getBuildingName();
                CommercialBuildingConfig.ShopMode shopMode = config.getShopMode();
                // NPC_SELL: 仅NPC，玩家不能打开
                // PLAYER_SELL: 仅玩家，玩家可以买卖
                // MIXED: 混合模式，玩家可以买卖
                this.canBuy = shopMode == CommercialBuildingConfig.ShopMode.PLAYER_SELL
                        || shopMode == CommercialBuildingConfig.ShopMode.MIXED;
                this.canSell = shopMode == CommercialBuildingConfig.ShopMode.PLAYER_SELL
                        || shopMode == CommercialBuildingConfig.ShopMode.MIXED;
            } else {
                // 如果配置无法加载，使用建筑文件名作为建筑名称
                // 默认不允许购买和出售（NPC_SELL 模式）
                this.buildingName = buildingFileName != null ? buildingFileName : "未知建筑";
                this.canBuy = false;
                this.canSell = false;
            }
        }

        public ModularUI createModularUI() {
            return CommercialTradeSelectScreen.createUI(this);
        }

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

        // ==================== 按钮回调 ====================

        public void onBuyButtonClick() {
            if (canBuy) {
                Minecraft.getInstance().setScreen(
                        new CommercialBuyScreen(controlBoxPos, buildingFileName)
                );
            }
        }

        public void onSellButtonClick() {
            if (canSell) {
                Minecraft.getInstance().setScreen(
                        new CommercialSellScreen(controlBoxPos, buildingFileName)
                );
            }
        }

        public void onBackButtonClick() {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
