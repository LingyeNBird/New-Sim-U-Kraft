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

    public static final RegistryObject<SoundEvent> MALE_HELLO = registerSoundEvent("voice.male_hello");
    public static final RegistryObject<SoundEvent> FEMALE_HELLO = registerSoundEvent("voice.female_hello");
    public static final RegistryObject<SoundEvent> MALE_HURT = registerSoundEvent("voice.male_hurt");
    public static final RegistryObject<SoundEvent> FEMALE_HURT = registerSoundEvent("voice.female_hurt");
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
