package com.xiaoliang.simukraft.item.food;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

import java.util.List;
import java.util.Objects;

/**
 * 模组食物定义
 * 包含所有自定义食物的属性和效果
 */
public final class ModFoods {

    private ModFoods() {
        // 工具类，禁止实例化
    }

    private static MobEffectInstance effect(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(Objects.requireNonNull(effect), duration, amplifier);
    }

    // ========== 食物属性 ==========

    /**
     * 汉堡 - 提供饱和感和力量
     */
    public static final FoodProperties HAMBURGER = BuffFoodItem.createFoodBuilder(
            8,  // 营养值
            0.8f,  // 饱和度
            List.of(
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.SATURATION, 1, 0),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.DAMAGE_BOOST, 600, 0),
                            0.8f
                    )
            )
    ).build();

    /**
     * 薯条 - 提供速度和急迫
     */
    public static final FoodProperties FRENCH_FRIES = BuffFoodItem.createFoodBuilder(
            4,
            0.4f,
            List.of(
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.MOVEMENT_SPEED, 400, 0),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.DIG_SPEED, 300, 0),
                            0.6f
                    )
            )
    ).build();

    /**
     * 奶酪块 - 提供生命恢复和抗性
     */
    public static final FoodProperties CHEESE_CHUNK = BuffFoodItem.createFoodBuilder(
            2,
            0.3f,
            List.of(
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.REGENERATION, 200, 0),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.DAMAGE_RESISTANCE, 300, 0),
                            0.5f
                    )
            )
    ).build();

    /**
     * 奶酪汉堡 - 终极食物，提供多种强力效果
     */
    public static final FoodProperties CHEESE_BURGER = BuffFoodItem.createFoodBuilder(
            12,
            1.0f,
            List.of(
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.SATURATION, 1, 0),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.DAMAGE_BOOST, 1200, 1),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.REGENERATION, 300, 0),
                            1.0f
                    ),
                    new BuffFoodItem.EffectEntry(
                            () -> effect(MobEffects.ABSORPTION, 1200, 0),
                            0.8f
                    )
            )
    ).build();
}
