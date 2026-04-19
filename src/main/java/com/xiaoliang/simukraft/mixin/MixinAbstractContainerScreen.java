package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.inventory.WarehouseGridMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 用于修改容器界面的渲染行为
 * 跳过仓库槽位（0-53）的渲染，因为我们自己在 WarehouseGridContainerScreen 中绘制
 */
@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen {

    /**
     * 在渲染槽位之前检查，如果是仓库槽位则跳过
     */
    @Inject(method = "renderSlot", 
            at = @At("HEAD"),
            cancellable = true)
    private void beforeRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        
        // 检查是否是仓库菜单且是仓库槽位（前54个）
        if (screen.getMenu() instanceof WarehouseGridMenu && slot.index < 54) {
            // 跳过仓库槽位的渲染
            ci.cancel();
        }
    }
}
