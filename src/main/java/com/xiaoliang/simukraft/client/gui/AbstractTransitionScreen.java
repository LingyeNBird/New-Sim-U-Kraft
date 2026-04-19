package com.xiaoliang.simukraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Objects;

public abstract class AbstractTransitionScreen extends Screen {
    protected float alpha = 0.0F;
    protected final float transitionSpeed = 0.05F;

    protected AbstractTransitionScreen(Component title) {
        super(title);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics safeGuiGraphics = Objects.requireNonNull(guiGraphics);
        // 更新透明度，实现平滑过渡效果
        if (alpha < 1.0F) {
            alpha += transitionSpeed;
            if (alpha > 1.0F) {
                alpha = 1.0F;
            }
        }
        
        // 先绘制背景，确保背景在底层
        drawBackground(safeGuiGraphics);
        
        // 然后调用父类的render方法，绘制按钮等组件
        super.render(safeGuiGraphics, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void renderBackground(@Nonnull GuiGraphics guiGraphics) {
        // 重写renderBackground方法，不绘制默认背景，避免覆盖我们的半透明背景
    }
    
    /**
     * 获取当前透明度，用于子类绘制背景时使用
     */
    protected float getAlpha() {
        return alpha;
    }
    
    /**
     * 绘制带有透明度的背景，子类可以调用此方法来绘制背景
     */
    protected void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        int backgroundColor = (int)(0xC8 * alpha) << 24 | 0x000000;
        guiGraphics.fillGradient(0, 0, this.width, this.height, backgroundColor, backgroundColor);
    }
}
