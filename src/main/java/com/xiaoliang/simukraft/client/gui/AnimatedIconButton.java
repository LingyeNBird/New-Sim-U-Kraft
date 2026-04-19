package com.xiaoliang.simukraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 带动画效果的图标按钮
 * 使用原版普通按钮样式，设置图标作为主图标，Simukraft图标放在右下角并旋转
 * 鼠标放在上面时Simukraft图标缓慢转动
 * 集成更新提示功能（跳动动画 + 红色徽章）
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class AnimatedIconButton extends Button {
    @Nonnull
    private static ResourceLocation requireResourceLocation(ResourceLocation value) {
        return Objects.requireNonNull(value);
    }

    @Nonnull
    private static Quaternionf requireQuaternion(Quaternionf value) {
        return Objects.requireNonNull(value);
    }

    // 纹理资源
    private static final ResourceLocation SETTING_ICON = requireResourceLocation(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/setting_icon.png"));
    private static final ResourceLocation SIMUKRAFT_ICON = requireResourceLocation(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/icon.png"));

    // 模组自带的按钮纹理
    private static final ResourceLocation WIDGETS_TEXTURE = requireResourceLocation(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/widgets.png"));

    // 动画相关
    private float rotationAngle = 0.0f;
    private static final float ROTATION_SPEED = 3.0f; // 旋转速度（度/帧）

    // 更新提示动画
    private boolean hasUpdate = false;
    private float flashTime = 0.0f;
    private static final float FLASH_SPEED = 0.2f;
    private int flashAlpha = 255;

    public AnimatedIconButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    /**
     * 设置是否有新版本可用
     */
    public void setHasUpdate(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    /**
     * 检查是否有新版本
     */
    public boolean hasUpdate() {
        return hasUpdate;
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 更新旋转角度（仅在悬停时）
        if (this.isHovered()) {
            rotationAngle += ROTATION_SPEED * partialTick;
            if (rotationAngle >= 360.0f) {
                rotationAngle -= 360.0f;
            }
        } else {
            // 不悬停时逐渐回到0度
            if (rotationAngle > 0) {
                rotationAngle -= ROTATION_SPEED * 2 * partialTick;
                if (rotationAngle < 0) rotationAngle = 0;
            }
        }

        // 更新闪烁动画（当有新版本时）
        if (hasUpdate) {
            flashTime += FLASH_SPEED * partialTick;
            // 使用正弦波计算闪烁透明度 (100-255)
            flashAlpha = (int) (155 + Math.sin(flashTime * 2) * 100);
        }

        int bgY = this.getY();

        // 使用原版按钮的渲染方式
        renderButtonBackground(guiGraphics, bgY);

        // 渲染设置图标（主图标，居中）
        renderSettingIcon(guiGraphics, bgY);

        // 渲染Simukraft图标（右下角，带旋转）
        renderSimukraftIcon(guiGraphics, bgY);

        // 渲染红色闪烁边框（如果有新版本）
        if (hasUpdate) {
            renderRedFlashBorder(guiGraphics, bgY);
        }
    }

    /**
     * 渲染按钮背景
     * 直接拉伸 20x20 纹理占满整个按钮
     */
    private void renderButtonBackground(GuiGraphics guiGraphics, int renderY) {
        // 使用 blit 配合正确的 UV 坐标来拉伸纹理
        // 参数: 纹理, 屏幕X, 屏幕Y, 纹理U, 纹理V, 渲染宽度, 渲染高度, 纹理总宽, 纹理总高
        guiGraphics.blit(WIDGETS_TEXTURE, this.getX(), renderY, 0, 0,
                this.width, this.height, this.width, this.height);

        // 悬停时添加白色边框高亮效果
        if (this.isHovered()) {
            int borderColor = 0xFFFFFFFF; // 纯白色
            // 上边框
            guiGraphics.fill(this.getX(), renderY, this.getX() + this.width, renderY + 1, borderColor);
            // 下边框
            guiGraphics.fill(this.getX(), renderY + this.height - 1, this.getX() + this.width, renderY + this.height, borderColor);
            // 左边框
            guiGraphics.fill(this.getX(), renderY, this.getX() + 1, renderY + this.height, borderColor);
            // 右边框
            guiGraphics.fill(this.getX() + this.width - 1, renderY, this.getX() + this.width, renderY + this.height, borderColor);
        }
    }

    /**
     * 渲染设置图标（不旋转）
     * 图标大小根据按钮尺寸自适应
     */
    private void renderSettingIcon(GuiGraphics guiGraphics, int renderY) {
        // 图标大小为按钮尺寸的80%，但不超过20像素
        int iconSize = Math.min((int) (Math.min(this.width, this.height) * 0.8f), 20);
        int iconX = this.getX() + (this.width - iconSize) / 2;
        int iconY = renderY + (this.height - iconSize) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SETTING_ICON);
        guiGraphics.blit(SETTING_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }

    /**
     * 渲染Simukraft图标（右下角，带旋转）
     * 图标大小根据按钮尺寸自适应
     */
    private void renderSimukraftIcon(GuiGraphics guiGraphics, int renderY) {
        // 小图标大小为按钮尺寸的50%，但不超过14像素
        int smallIconSize = Math.min((int) (Math.min(this.width, this.height) * 0.5f), 14);
        int centerX = this.getX() + this.width - smallIconSize / 2 - Math.max(2, this.width / 12);
        int centerY = renderY + this.height - smallIconSize / 2 - Math.max(2, this.height / 12);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 移动到中心点
        poseStack.translate(centerX, centerY, 0);
        // 应用旋转
        poseStack.mulPose(requireQuaternion(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle)));
        // 移回原位
        poseStack.translate(-smallIconSize / 2.0, -smallIconSize / 2.0, 0);

        // 渲染Simukraft图标
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SIMUKRAFT_ICON);
        guiGraphics.blit(SIMUKRAFT_ICON, 0, 0, 0, 0,
                smallIconSize, smallIconSize, smallIconSize, smallIconSize);

        poseStack.popPose();
    }

    /**
     * 渲染红色闪烁边框
     */
    private void renderRedFlashBorder(GuiGraphics guiGraphics, int renderY) {
        // 红色带闪烁透明度
        int redColor = (flashAlpha << 24) | 0xFF0000;
        int borderThickness = 2;

        // 上边框
        guiGraphics.fill(this.getX(), renderY, this.getX() + this.width, renderY + borderThickness, redColor);
        // 下边框
        guiGraphics.fill(this.getX(), renderY + this.height - borderThickness, this.getX() + this.width, renderY + this.height, redColor);
        // 左边框
        guiGraphics.fill(this.getX(), renderY, this.getX() + borderThickness, renderY + this.height, redColor);
        // 右边框
        guiGraphics.fill(this.getX() + this.width - borderThickness, renderY, this.getX() + this.width, renderY + this.height, redColor);
    }
}
