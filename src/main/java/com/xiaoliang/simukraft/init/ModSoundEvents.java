package com.xiaoliang.simukraft.init;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Simukraft.MOD_ID);

    public static final RegistryObject<SoundEvent> NPC_UI_OPEN_MALE = registerSoundEvent("voice.ui_open.male");
    public static final RegistryObject<SoundEvent> NPC_UI_OPEN_FEMALE = registerSoundEvent("voice.ui_open.female");
    public static final RegistryObject<SoundEvent> NPC_HURT_MALE = registerSoundEvent("voice.hurt.male");
    public static final RegistryObject<SoundEvent> NPC_HURT_FEMALE = registerSoundEvent("voice.hurt.female");
    public static final RegistryObject<SoundEvent> NPC_ARRIVAL_MALE = registerSoundEvent("voice.arrival.male");
    public static final RegistryObject<SoundEvent> NPC_ARRIVAL_FEMALE = registerSoundEvent("voice.arrival.female");
    public static final RegistryObject<SoundEvent> NPC_CHAT_MALE = registerSoundEvent("voice.chat.male");
    public static final RegistryObject<SoundEvent> NPC_CHAT_FEMALE = registerSoundEvent("voice.chat.female");
    public static final RegistryObject<SoundEvent> NPC_NIGHT_MALE = registerSoundEvent("voice.night.male");
    public static final RegistryObject<SoundEvent> NPC_NIGHT_FEMALE = registerSoundEvent("voice.night.female");
    public static final RegistryObject<SoundEvent> NPC_BUY_FOOD_BURGER_MALE = registerSoundEvent("voice.food.burger.male");
    public static final RegistryObject<SoundEvent> NPC_BUY_FOOD_BURGER_FEMALE = registerSoundEvent("voice.food.burger.female");
    public static final RegistryObject<SoundEvent> NPC_BUY_FOOD_BAKERY_MALE = registerSoundEvent("voice.food.bakery.male");
    public static final RegistryObject<SoundEvent> NPC_BUY_FOOD_BAKERY_FEMALE = registerSoundEvent("voice.food.bakery.female");
    public static final RegistryObject<SoundEvent> NPC_PREGNANT = registerSoundEvent("voice.birth.pregnant");
    public static final RegistryObject<SoundEvent> NPC_BIRTH = registerSoundEvent("voice.birth.birth");
    public static final RegistryObject<SoundEvent> BUILDER_READY_MALE = registerSoundEvent("voice.builder.ready.male");
    public static final RegistryObject<SoundEvent> BUILDER_READY_FEMALE = registerSoundEvent("voice.builder.ready.female");
    public static final RegistryObject<SoundEvent> BUILDER_ARRIVAL = registerSoundEvent("voice.builder.arrival");
    public static final RegistryObject<SoundEvent> BUILDING_MATERIAL_STORE_OPEN = registerSoundEvent("voice.store.open");
    public static final RegistryObject<SoundEvent> BUILDING_MATERIAL_STORE_PAYMENT = registerSoundEvent("voice.store.payment");
    public static final RegistryObject<SoundEvent> BUILDING_MATERIAL_STORE_CASH = registerSoundEvent("voice.store.cash");
    public static final RegistryObject<SoundEvent> BUILD_BOX_PLACE = registerSoundEvent("block.build_box.place");
    public static final RegistryObject<SoundEvent> BUILD_BOX_BREAK = registerSoundEvent("block.build_box.break");
    public static final RegistryObject<SoundEvent> BUILD_BOX_OPEN = registerSoundEvent("ui.build_box.open");
    public static final RegistryObject<SoundEvent> CITY_CORE_OPEN = registerSoundEvent("ui.city_core.open");
    public static final RegistryObject<SoundEvent> CONSTRUCTION_COMPLETE = registerSoundEvent("construction.complete");
    public static final RegistryObject<SoundEvent> PLAYER_WAKE_UP = registerSoundEvent("player.wake_up");
    public static final RegistryObject<SoundEvent> MONEY_COLLECT = registerSoundEvent("money.collect");
    public static final RegistryObject<SoundEvent> BUILDING_START = registerSoundEvent("construction.start");
    public static final RegistryObject<SoundEvent> FARMLAND_BOX_PLACE = registerSoundEvent("block.farmland_box.place");
    public static final RegistryObject<SoundEvent> FARMLAND_BOX_BREAK = registerSoundEvent("block.farmland_box.break");
    public static final RegistryObject<SoundEvent> FIRST_DREAM = registerSoundEvent("music.first_dream");


    private static RegistryObject<SoundEvent> registerSoundEvent(String path) {
        String safePath = Objects.requireNonNull(path);
        ResourceLocation location = Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, safePath));
        return SOUND_EVENTS.register(safePath, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
