package com.xiaoliang.simukraft.init;

import com.xiaoliang.simukraft.item.PortableCityCoreItem;
import com.xiaoliang.simukraft.item.food.BuffFoodItem;
import com.xiaoliang.simukraft.item.food.ModFoods;
import javax.annotation.Nonnull;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, com.xiaoliang.simukraft.Simukraft.MOD_ID);

    private static @Nonnull FoodProperties food(@Nonnull FoodProperties properties) {
        return Objects.requireNonNull(properties);
    }

    // 便携城市核心
    public static final RegistryObject<Item> PORTABLE_CITY_CORE = ITEMS.register("portable_city_core",
            PortableCityCoreItem::new);

    // ========== 食物 ==========

    // 汉堡
    public static final RegistryObject<Item> HAMBURGER = ITEMS.register("hamburger",
            () -> new BuffFoodItem(new Item.Properties().food(food(ModFoods.HAMBURGER))));

    // 薯条
    public static final RegistryObject<Item> FRENCH_FRIES = ITEMS.register("french_fries",
            () -> new BuffFoodItem(new Item.Properties().food(food(ModFoods.FRENCH_FRIES))));

    // 奶酪块
    public static final RegistryObject<Item> CHEESE_CHUNK = ITEMS.register("cheese_chunk",
            () -> new BuffFoodItem(new Item.Properties().food(food(ModFoods.CHEESE_CHUNK))));

    // 奶酪汉堡
    public static final RegistryObject<Item> CHEESE_BURGER = ITEMS.register("cheese_burger",
            () -> new BuffFoodItem(new Item.Properties().food(food(ModFoods.CHEESE_BURGER))));
}
