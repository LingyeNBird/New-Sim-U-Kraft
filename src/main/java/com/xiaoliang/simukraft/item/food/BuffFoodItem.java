package com.xiaoliang.simukraft.item.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 带Buff效果的食物物品基类
 */
public class BuffFoodItem extends Item {

    public BuffFoodItem(Properties properties) {
        super(properties);
    }

    /**
     * 创建食物属性构建器
     *
     * @param nutrition  营养值（饥饿值）
     * @param saturation 饱和度
     * @param effects    Buff效果列表
     * @return FoodProperties.Builder
     */
    public static FoodProperties.Builder createFoodBuilder(
            int nutrition,
            float saturation,
            @NotNull List<EffectEntry> effects) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation);

        for (EffectEntry entry : effects) {
            builder.effect(Objects.requireNonNull(entry.effectSupplier()), entry.probability());
        }

        return builder;
    }

    /**
     * 效果条目记录类
     */
    public record EffectEntry(@NotNull Supplier<MobEffectInstance> effectSupplier, float probability) {
    }
}
