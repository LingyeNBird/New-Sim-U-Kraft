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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 更新通知按钮
 * 当有新版本可用时，图标会跳动动画
 */
@OnlyIn(Dist.CLIENT)
public class UpdateNotificationButton extends Button {
    @Nonnull
    private static <T> T nn(@Nullable T value) {
        return Objects.requireNonNull(value);
    }


    @Nonnull
    private static final ResourceLocation SETTING_ICON = nn(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/setting_icon.png"));
    @Nonnull
    private static final ResourceLocation SIMUKRAFT_ICON = nn(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/icon.png"));

    @Nonnull
    private static final ResourceLocation WIDGETS_TEXTURE = nn(ResourceLocation.fromNamespaceAndPath(
            "simukraft", "textures/gui/widgets.png"));

    // 动画相关
    private float rotationAngle = 0.0f;
    private static final float ROTATION_SPEED = 3.0f;
    private float bounceOffset = 0.0f;
    private float bounceTime = 0.0f;
    private static final float BOUNCE_SPEED = 0.15f;
    private static final float BOUNCE_AMPLITUDE = 3.0f;

    private boolean hasUpdate = false;
    private int badgePulse = 0;

    public UpdateNotificationButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    public void setHasUpdate(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

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
            if (rotationAngle > 0) {
                rotationAngle -= ROTATION_SPEED * 2 * partialTick;
                if (rotationAngle < 0) rotationAngle = 0;
            }
        }

        // 更新跳动动画（当有新版本时）
        if (hasUpdate) {
            bounceTime += BOUNCE_SPEED * partialTick;
            bounceOffset = (float) Math.sin(bounceTime) * BOUNCE_AMPLITUDE;
            badgePulse = (int) ((Math.sin(bounceTime * 2) + 1) * 127.5);
        } else {
            bounceOffset = 0;
        }

        // 使用跳动后的Y坐标
        int renderY = this.getY() + (int) bounceOffset;

        // 渲染按钮背景
        renderButtonBackground(guiGraphics, renderY);

        // 渲染设置图标（主图标，居中，不旋转）
        renderSettingIcon(guiGraphics, renderY);

        // 渲染Simukraft图标（右下角，带旋转）
        renderSimukraftIcon(guiGraphics, renderY);

        // 渲染更新徽章（如果有新版本）
        if (hasUpdate) {
            renderUpdateBadge(guiGraphics, renderY);
        }
    }

    /**
     * 渲染按钮背景
     */
    private void renderButtonBackground(GuiGraphics guiGraphics, int renderY) {
        guiGraphics.blit(WIDGETS_TEXTURE, this.getX(), renderY, 0, 0,
                this.width, this.height, this.width, this.height);

        // 悬停时添加白色边框高亮效果
        if (this.isHovered()) {
            int borderColor = 0xFFFFFFFF;
            guiGraphics.fill(this.getX(), renderY, this.getX() + this.width, renderY + 1, borderColor);
            guiGraphics.fill(this.getX(), renderY + this.height - 1, this.getX() + this.width, renderY + this.height, borderColor);
            guiGraphics.fill(this.getX(), renderY, this.getX() + 1, renderY + this.height, borderColor);
            guiGraphics.fill(this.getX() + this.width - 1, renderY, this.getX() + this.width, renderY + this.height, borderColor);
        }

        // 有新版本时添加金色高亮边框
        if (hasUpdate) {
            int goldColor = 0xFFFFD700 | (badgePulse << 24);
            guiGraphics.fill(this.getX(), renderY, this.getX() + this.width, renderY + 1, goldColor);
            guiGraphics.fill(this.getX(), renderY + this.height - 1, this.getX() + this.width, renderY + this.height, goldColor);
            guiGraphics.fill(this.getX(), renderY, this.getX() + 1, renderY + this.height, goldColor);
            guiGraphics.fill(this.getX() + this.width - 1, renderY, this.getX() + this.width, renderY + this.height, goldColor);
        }
    }

    /**
     * 渲染设置图标（不旋转）
     */
    private void renderSettingIcon(GuiGraphics guiGraphics, int renderY) {
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
     */
    private void renderSimukraftIcon(GuiGraphics guiGraphics, int renderY) {
        int smallIconSize = Math.min((int) (Math.min(this.width, this.height) * 0.5f), 14);
        int centerX = this.getX() + this.width - smallIconSize / 2 - Math.max(2, this.width / 12);
        int centerY = renderY + this.height - smallIconSize / 2 - Math.max(2, this.height / 12);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY, 0);
        poseStack.mulPose(nn(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle)));
        poseStack.translate(-smallIconSize / 2.0, -smallIconSize / 2.0, 0);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SIMUKRAFT_ICON);
        guiGraphics.blit(SIMUKRAFT_ICON, 0, 0, 0, 0,
                smallIconSize, smallIconSize, smallIconSize, smallIconSize);

        poseStack.popPose();
    }

    /**
     * 渲染更新徽章
     */
    private void renderUpdateBadge(GuiGraphics guiGraphics, int renderY) {
        int badgeSize = 8;
        int badgeX = this.getX() + this.width - badgeSize - 2;
        int badgeY = renderY + 2;

        // 绘制红色圆形徽章
        int badgeColor = 0xFFFF4444 | (badgePulse << 24);
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeColor);

        // 绘制白色感叹号
        int exclamationColor = 0xFFFFFFFF;
        int centerX = badgeX + badgeSize / 2;
        guiGraphics.fill(centerX, badgeY + 1, centerX + 1, badgeY + 4, exclamationColor);
        guiGraphics.fill(centerX, badgeY + 5, centerX + 1, badgeY + 6, exclamationColor);
    }
}
