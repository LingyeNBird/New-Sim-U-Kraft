package com.xiaoliang.simukraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 指南物品
 * 右键后在客户端打开专用书本界面。
 */
public class GuideBookItem extends Item {

    public GuideBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = Objects.requireNonNull(player.getItemInHand(Objects.requireNonNull(hand)));
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.xiaoliang.simukraft.client.GuideBookClientHooks.openGuideScreen(stack));
        }
        return Objects.requireNonNull(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
    }
}
