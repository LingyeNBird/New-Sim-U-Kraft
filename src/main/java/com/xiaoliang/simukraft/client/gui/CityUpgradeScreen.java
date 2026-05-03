package com.xiaoliang.simukraft.client.gui;

import com.mojang.logging.LogUtils;
import com.xiaoliang.simukraft.client.ClientSimukraftData;
import com.xiaoliang.simukraft.client.gui.components.UpgradeCanvas;
import com.xiaoliang.simukraft.world.CityUpgradeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

/**
 * 城市升级界面类
 */
public class CityUpgradeScreen extends AbstractTransitionScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final BlockPos cityCorePos;
    private UpgradeCanvas upgradeCanvas;
    private final int cityLevel;
    private Button submitButton;
    private Button cancelButton;
    @Nullable
    private Component feedbackMessage;
    private int feedbackColor = 0xFFFFFF;
    private boolean pendingUpgradeRequest;
    
    // 右侧面板滑动动画相关变量
    private int panelWidth; // 面板宽度
    private int targetPanelX; // 目标位置（弹出或收回）
    private int currentPanelX; // 当前位置
    private final int animationSpeed = 15; // 动画速度

    /**
     * 构造城市升级界面
     * @param cityCorePos 城市核心位置
     * @param cityLevel 城市等级
     */
    public CityUpgradeScreen(BlockPos cityCorePos, int cityLevel) {
        super(Component.translatable("gui.city_upgrade.title"));
        this.cityCorePos = cityCorePos;
        this.cityLevel = cityLevel;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        
        // 创建返回按钮
        this.addRenderableWidget(nn(Button.builder(
            nn(Component.translatable("gui.city_upgrade.back")),
            button -> this.closeScreen()
        ).pos(centerX - 50, this.height - 40).size(100, 20).build()));
        
        // 初始化升级画布
        int canvasWidth = this.width - 40;
        int canvasHeight = this.height - 100;
        this.upgradeCanvas = new UpgradeCanvas(20, 60, canvasWidth, canvasHeight, this, cityCorePos, cityLevel);
        this.addRenderableWidget(upgradeCanvas);
        
        // 初始化面板宽度
        panelWidth = this.width / 3;
        
        // 初始化面板位置（默认收回）
        currentPanelX = this.width; // 初始位置：屏幕右侧外部
        targetPanelX = this.width; // 目标位置：收回
        
        // 创建右侧面板的提交和取消按钮
        int panelX = this.width - panelWidth;
        int buttonWidth = panelWidth - 40;
        
        // 提交按钮
        submitButton = nn(Button.builder(
            nn(Component.translatable("gui.city_upgrade.submit")),
            button -> this.handleSubmit()
        ).pos(panelX + 20, this.height - 70).size(buttonWidth / 2, 20).build());
        this.addRenderableWidget(submitButton);
        
        // 取消按钮
        cancelButton = nn(Button.builder(
            nn(Component.translatable("gui.city_upgrade.cancel")),
            button -> this.handleCancel()
        ).pos(panelX + 20 + buttonWidth / 2 + 10, this.height - 70).size(buttonWidth / 2, 20).build());
        this.addRenderableWidget(cancelButton);
        
        // 默认隐藏按钮
        nn(submitButton).visible = false;
        nn(cancelButton).visible = false;
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制背景
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // 绘制标题
        int centerX = this.width / 2;
        guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.title")), centerX - 50, 20, 0xFFFFFF);
        
        // 更新动画
        updateAnimation();
        
        // 绘制右侧面板
        renderRightPanel(guiGraphics, mouseX, mouseY);
        
        // 重新渲染按钮，确保它们在右侧面板之上
        if (submitButton != null) {
            submitButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (cancelButton != null) {
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
    
    /**
     * 更新面板滑动动画
     */
    private void updateAnimation() {
        // 根据选中的标记更新目标位置
        UpgradeCanvas.MapMarker selectedMarker = upgradeCanvas.getSelectedMarker();
        if (selectedMarker != null) {
            // 弹出面板
            targetPanelX = this.width - panelWidth;
        } else {
            // 收回面板
            targetPanelX = this.width;
        }
        
        // 动画逻辑
        if (currentPanelX != targetPanelX) {
            if (currentPanelX < targetPanelX) {
                // 向右移动（收回）
                currentPanelX += animationSpeed;
                if (currentPanelX > targetPanelX) {
                    currentPanelX = targetPanelX;
                }
            } else {
                // 向左移动（弹出）
                currentPanelX -= animationSpeed;
                if (currentPanelX < targetPanelX) {
                    currentPanelX = targetPanelX;
                }
            }
        }
        
        // 更新按钮位置和可见性
        updateButtons();
    }
    
    /**
     * 更新按钮位置和可见性
     */
    private void updateButtons() {
        UpgradeCanvas.MapMarker selectedMarker = upgradeCanvas.getSelectedMarker();
        boolean isVisible = selectedMarker != null && currentPanelX <= this.width - panelWidth + 10;
        
        boolean isUpgradeable = false;
        if (selectedMarker != null) {
            // 从标记中获取升级等级
            String hoverText = selectedMarker.getHoverText();
            int upgradeLevel = 0;
            if (!hoverText.isEmpty()) {
                try {
                    upgradeLevel = Integer.parseInt(hoverText.substring(0, hoverText.indexOf(":")));
                    // 检查是否是可升级的节点（即升级等级等于当前城市等级+1）
                    isUpgradeable = upgradeLevel == cityLevel + 1;
                } catch (Exception e) {
                    upgradeLevel = 0;
                }
            }
        }
        
        // 始终显示提交按钮，但根据节点状态禁用或启用
        submitButton.visible = isVisible;
        cancelButton.visible = isVisible;
        submitButton.active = isUpgradeable && !pendingUpgradeRequest;
        cancelButton.active = !pendingUpgradeRequest;
        
        // 更新按钮位置
        int buttonX = currentPanelX + 20;
        int buttonWidth = panelWidth - 40;
        submitButton.setPosition(buttonX, this.height - 70);
        cancelButton.setPosition(buttonX + buttonWidth / 2 + 10, this.height - 70);
    }
    
    /**
     * 绘制右侧面板
     */
    private void renderRightPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 面板位置和大小
        int panelY = 0;
        int panelHeight = this.height;
        
        // 绘制半透明灰色背景
        guiGraphics.fill(currentPanelX, panelY, currentPanelX + panelWidth, panelY + panelHeight, 0x80333333);
        
        // 获取选中的标记
        UpgradeCanvas.MapMarker selectedMarker = upgradeCanvas.getSelectedMarker();
        
        if (selectedMarker != null) {
            // 绘制面板标题
            guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.panel_title")), currentPanelX + 10, panelY + 10, 0xFFFFFF);
            
            // 从标记中获取升级等级（通过解析hoverText）
            String hoverText = selectedMarker.getHoverText();
            int upgradeLevel = 0;
            String upgradeName = "";
            if (!hoverText.isEmpty()) {
                try {
                    upgradeLevel = Integer.parseInt(hoverText.substring(0, hoverText.indexOf(":")));
                    upgradeName = hoverText.substring(hoverText.indexOf(":") + 1);
                } catch (Exception e) {
                    upgradeLevel = 0;
                    upgradeName = hoverText;
                }
            }
            
            // 获取升级配置
            CityUpgradeManager upgradeManager = CityUpgradeManager.getInstance();
            CityUpgradeManager.CityUpgrade upgrade = upgradeManager.getUpgrade(upgradeLevel);
            CityUpgradeManager.Requirements requirements = upgrade != null ? upgrade.requirements() : null;
            
            // 绘制标记信息（只显示等级和名称）
            int yOffset = 30;
            guiGraphics.drawString(nn(this.font), nn(Component.literal(upgradeLevel + "." + upgradeName)), currentPanelX + 10, panelY + yOffset, 0xFFFFFF);
            yOffset += this.font.lineHeight + 5;
            
            // 绘制升级描述
            if (upgrade != null && !upgrade.description().isEmpty()) {
                guiGraphics.drawString(nn(this.font), nn(Component.literal(safeString(upgrade.description()))), currentPanelX + 10, panelY + yOffset, 0xFFFFFF);
                yOffset += this.font.lineHeight + 10;
            }
            
            // 绘制要求
            guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.requirements")), currentPanelX + 10, panelY + yOffset, 0xFFFFFF);
            yOffset += this.font.lineHeight + 5;
            
            // 根据升级要求绘制物品图标
            if (requirements != null) {
                // 获取玩家背包
                net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
                
                // 从玩家背包中获取物品数量
                int currentWood = countItemsInInventory(player, Items.OAK_LOG);
                int currentCobblestone = countItemsInInventory(player, Items.COBBLESTONE);
                int currentIron = countItemsInInventory(player, Items.IRON_INGOT);
                int currentGold = countItemsInInventory(player, Items.GOLD_INGOT);
                int currentLapis = countItemsInInventory(player, Items.LAPIS_LAZULI);
                
                // 绘制人口要求
                int currentPopulation = ClientSimukraftData.getCurrentCityPopulation();
                int currentDiamond = countItemsInInventory(player, Items.DIAMOND);
                if (requirements.population() > 0) {
                    boolean hasEnough = currentPopulation >= requirements.population();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000; // 满足为绿色，不满足为红色
                    guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.requirement_population", currentPopulation, requirements.population())), currentPanelX + 20, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 5;
                }
                
                // 绘制木头
                if (requirements.wood() > 0) {
                    boolean hasEnough = currentWood >= requirements.wood();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack woodStack = new ItemStack(nn(Items.OAK_LOG), requirements.wood());
                    drawItemStack(guiGraphics, woodStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentWood + " / " + requirements.wood())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制圆石
                if (requirements.cobblestone() > 0) {
                    boolean hasEnough = currentCobblestone >= requirements.cobblestone();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack cobblestoneStack = new ItemStack(nn(Items.COBBLESTONE), requirements.cobblestone());
                    drawItemStack(guiGraphics, cobblestoneStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentCobblestone + " / " + requirements.cobblestone())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制铁锭
                if (requirements.ironIngot() > 0) {
                    boolean hasEnough = currentIron >= requirements.ironIngot();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack ironStack = new ItemStack(nn(Items.IRON_INGOT), requirements.ironIngot());
                    drawItemStack(guiGraphics, ironStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentIron + " / " + requirements.ironIngot())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制金锭
                if (requirements.goldIngot() > 0) {
                    boolean hasEnough = currentGold >= requirements.goldIngot();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack goldStack = new ItemStack(nn(Items.GOLD_INGOT), requirements.goldIngot());
                    drawItemStack(guiGraphics, goldStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentGold + " / " + requirements.goldIngot())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制钻石
                if (requirements.diamond() > 0) {
                    boolean hasEnough = currentDiamond >= requirements.diamond();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack diamondStack = new ItemStack(nn(Items.DIAMOND), requirements.diamond());
                    drawItemStack(guiGraphics, diamondStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentDiamond + " / " + requirements.diamond())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制青金石
                if (requirements.lapisLazuli() > 0) {
                    boolean hasEnough = currentLapis >= requirements.lapisLazuli();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    
                    ItemStack lapisStack = new ItemStack(nn(Items.LAPIS_LAZULI), requirements.lapisLazuli());
                    drawItemStack(guiGraphics, lapisStack, currentPanelX + 20, panelY + yOffset - 4);
                    guiGraphics.drawString(nn(this.font), nn(Component.literal(currentLapis + " / " + requirements.lapisLazuli())), currentPanelX + 40, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 8;
                }
                
                // 绘制资金要求
                double currentFunds = ClientSimukraftData.getCurrentCityFunds();
                if (requirements.funds() > 0.0) {
                    boolean hasEnough = currentFunds >= requirements.funds();
                    int textColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.requirement_funds", safeString(String.format(Locale.US, "%.2f", currentFunds)), requirements.funds())), currentPanelX + 20, panelY + yOffset, textColor);
                    yOffset += this.font.lineHeight + 10;
                }
            }
            
            // 绘制解锁功能
            if (upgrade != null && !upgrade.unlocks().isEmpty()) {
                guiGraphics.drawString(nn(this.font), nn(Component.translatable("gui.city_upgrade.unlocks")), currentPanelX + 10, panelY + yOffset, 0xFFFFFF);
                yOffset += this.font.lineHeight + 5;
                guiGraphics.drawString(nn(this.font), nn(Component.literal(safeString(upgrade.unlocks()))), currentPanelX + 20, panelY + yOffset, 0xFFFFFF);
                yOffset += this.font.lineHeight + 10;
            }

            if (feedbackMessage != null) {
                int availableWidth = panelWidth - 20;
                int feedbackY = this.height - 110;
                for (FormattedCharSequence line : this.font.split(nn(feedbackMessage), availableWidth)) {
                    guiGraphics.drawString(nn(this.font), nn(line), currentPanelX + 10, feedbackY, feedbackColor);
                    feedbackY += this.font.lineHeight + 2;
                    if (feedbackY >= this.height - 78) {
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * 计算玩家背包中指定物品的数量
     */
    private int countItemsInInventory(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        if (player == null) return 0;
        int count = 0;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * 绘制物品图标
     */
    private void drawItemStack(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        guiGraphics.renderItem(nn(itemStack), x, y);
        guiGraphics.renderItemDecorations(nn(this.font), nn(itemStack), x, y);
    }
    
    /**
     * 处理提交按钮点击
     */
    private void handleSubmit() {
        // 实际实现中需要发送网络包到服务器，处理升级请求
        UpgradeCanvas.MapMarker selectedMarker = upgradeCanvas.getSelectedMarker();
        if (selectedMarker != null) {
            LOGGER.debug("选中的升级标记: {}", selectedMarker.getHoverText());
            
            // 从标记中获取升级等级
            String hoverText = selectedMarker.getHoverText();
            int upgradeLevel = 0;
            if (!hoverText.isEmpty()) {
                try {
                    upgradeLevel = Integer.parseInt(hoverText.substring(0, hoverText.indexOf(":")));
                    LOGGER.debug("解析得到的升级等级: {}", upgradeLevel);
                } catch (Exception e) {
                    LOGGER.error("解析升级等级失败", e);
                    upgradeLevel = 0;
                }
            }
            
            // 发送升级请求到服务器
            if (upgradeLevel > 0) {
                LOGGER.debug("发送升级请求，城市核心位置: {}, 升级等级: {}", cityCorePos, upgradeLevel);
                pendingUpgradeRequest = true;
                feedbackMessage = Component.translatable("gui.city_upgrade.pending");
                feedbackColor = 0xFFE6B800;
                com.xiaoliang.simukraft.network.CityUpgradeRequestPacket packet = new com.xiaoliang.simukraft.network.CityUpgradeRequestPacket(cityCorePos, upgradeLevel);
                com.xiaoliang.simukraft.network.NetworkManager.INSTANCE.sendToServer(packet);
            } else {
                LOGGER.warn("无效的升级等级: {}", upgradeLevel);
                feedbackMessage = Component.translatable("message.simukraft.city_upgrade.invalid_target");
                feedbackColor = 0xFFFF5555;
            }
        } else {
            LOGGER.warn("未选中任何标记");
            feedbackMessage = Component.translatable("message.simukraft.city_upgrade.invalid_target");
            feedbackColor = 0xFFFF5555;
        }
    }
    
    /**
     * 处理取消按钮点击
     */
    private void handleCancel() {
        // 取消选择
        pendingUpgradeRequest = false;
        feedbackMessage = null;
        upgradeCanvas.setSelectedMarker(null);
        LOGGER.debug("已取消城市升级节点选择");
    }

    public void handleUpgradeResult(boolean success, Component message) {
        pendingUpgradeRequest = false;
        feedbackMessage = message;
        feedbackColor = success ? 0xFF55FF55 : 0xFFFF5555;
        if (success) {
            closeScreen();
        }
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    /**
     * 关闭当前屏幕，返回城市管理界面
     */
    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new CityManagementScreen(cityCorePos));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 直接调用父类的mouseClicked方法，让所有组件都有机会处理点击事件
        // 包括右侧面板内的按钮
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return nn(value);
    }
}
