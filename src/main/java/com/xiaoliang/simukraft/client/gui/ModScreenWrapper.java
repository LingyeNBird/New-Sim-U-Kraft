package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * 通用界面包装器
 * 用于绕过Inventory Tweaks等模组的屏幕初始化事件冲突
 * 可以包装任意Screen，在主菜单和暂停菜单都能正常打开
 */
@OnlyIn(Dist.CLIENT)
public class ModScreenWrapper extends Screen {

    private final Screen parent;
    private final Screen targetScreen;
    private boolean opened = false;

    public ModScreenWrapper(Screen parent, Screen targetScreen) {
        super(Component.empty());
        this.parent = parent;
        this.targetScreen = targetScreen;
    }

    @Override
    protected void init() {
        super.init();
        // 延迟打开实际界面，避开当前屏幕初始化事件
        if (!opened) {
            opened = true;
            // 使用延迟任务，确保在下一tick执行
            Minecraft.getInstance().tell(() -> {
                Minecraft.getInstance().setScreen(targetScreen);
            });
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染父屏幕背景
        if (parent != null) {
            parent.render(graphics, mouseX, mouseY, partialTicks);
        } else {
            this.renderBackground(graphics);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }
}
