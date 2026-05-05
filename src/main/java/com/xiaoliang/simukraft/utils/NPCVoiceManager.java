package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.building.CommercialBuildingConfig;
import com.xiaoliang.simukraft.building.CommercialBuildingManager;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.Gender;
import com.xiaoliang.simukraft.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("null")
public final class NPCVoiceManager {
    private static final float DEFAULT_VOLUME = 1.0F;
    private static final double DEFAULT_HEARING_RADIUS = 20.0D;

    private NPCVoiceManager() {
    }

    public static SoundEvent getUiOpenSound(@Nullable CustomEntity npc) {
        return getGenderedSound(npc, ModSoundEvents.NPC_UI_OPEN_MALE, ModSoundEvents.NPC_UI_OPEN_FEMALE);
    }

    public static SoundEvent getHurtSound(@Nullable CustomEntity npc) {
        return getGenderedSound(npc, ModSoundEvents.NPC_HURT_MALE, ModSoundEvents.NPC_HURT_FEMALE);
    }

    public static SoundEvent getAmbientChatSound(@Nullable CustomEntity npc) {
        return getGenderedSound(npc, ModSoundEvents.NPC_CHAT_MALE, ModSoundEvents.NPC_CHAT_FEMALE);
    }

    public static SoundEvent getArrivalSound(@Nullable CustomEntity npc) {
        if (npc != null && "builder".equalsIgnoreCase(npc.getJob())) {
            return npc.getRandom().nextBoolean()
                    ? getGenderedSound(npc, ModSoundEvents.BUILDER_READY_MALE, ModSoundEvents.BUILDER_READY_FEMALE)
                    : Objects.requireNonNull(ModSoundEvents.BUILDER_ARRIVAL.get());
        }
        return getGenderedSound(npc, ModSoundEvents.NPC_ARRIVAL_MALE, ModSoundEvents.NPC_ARRIVAL_FEMALE);
    }

    public static SoundEvent getNightSound(@Nullable CustomEntity npc) {
        return getGenderedSound(npc, ModSoundEvents.NPC_NIGHT_MALE, ModSoundEvents.NPC_NIGHT_FEMALE);
    }

    public static SoundEvent getFoodSound(@Nullable CustomEntity npc, @Nullable String buildingFileName) {
        if (isBakery(buildingFileName)) {
            return getGenderedSound(npc, ModSoundEvents.NPC_BUY_FOOD_BAKERY_MALE, ModSoundEvents.NPC_BUY_FOOD_BAKERY_FEMALE);
        }
        return getGenderedSound(npc, ModSoundEvents.NPC_BUY_FOOD_BURGER_MALE, ModSoundEvents.NPC_BUY_FOOD_BURGER_FEMALE);
    }

    public static SoundEvent getPregnantSound() {
        return Objects.requireNonNull(ModSoundEvents.NPC_PREGNANT.get());
    }

    public static SoundEvent getBirthSound() {
        return Objects.requireNonNull(ModSoundEvents.NPC_BIRTH.get());
    }

    public static SoundEvent getStoreOpenSound() {
        return Objects.requireNonNull(ModSoundEvents.BUILDING_MATERIAL_STORE_OPEN.get());
    }

    public static SoundEvent getStorePaymentSound() {
        return Objects.requireNonNull(ModSoundEvents.BUILDING_MATERIAL_STORE_PAYMENT.get());
    }

    public static SoundEvent getStoreCashSound() {
        return Objects.requireNonNull(ModSoundEvents.BUILDING_MATERIAL_STORE_CASH.get());
    }

    public static void playArrivalVoice(ServerLevel level, CustomEntity npc, @Nullable BlockPos pos) {
        BlockPos playPos = pos != null ? pos : npc.blockPosition();
        playVoiceAt(level, playPos, getArrivalSound(npc), getVoicePitch(npc), SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static void playAmbientChat(ServerLevel level, CustomEntity npc) {
        playVoiceAt(level, npc.blockPosition(), getAmbientChatSound(npc), getVoicePitch(npc), SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static void playNightVoice(ServerLevel level, CustomEntity npc) {
        playVoiceAt(level, npc.blockPosition(), getNightSound(npc), getVoicePitch(npc), SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static void playFoodVoice(ServerLevel level, CustomEntity npc, @Nullable String buildingFileName) {
        playVoiceAt(level, npc.blockPosition(), getFoodSound(npc, buildingFileName), getVoicePitch(npc), SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static void playPregnantVoice(ServerLevel level, CustomEntity npc) {
        playVoiceAt(level, npc.blockPosition(), getPregnantSound(), getVoicePitch(npc), SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static void playBirthVoice(ServerLevel level, BlockPos pos) {
        playVoiceAt(level, pos, getBirthSound(), 1.0F, SoundSource.NEUTRAL, DEFAULT_HEARING_RADIUS);
    }

    public static boolean isBuildingMaterialStore(@Nullable String buildingFileName) {
        CommercialBuildingConfig config = getCommercialConfig(buildingFileName);
        if (CommercialStorageHelper.isBuildingMaterialStore(config)) {
            return true;
        }

        String normalized = normalize(buildingFileName);
        return normalized.contains("building_material")
                || normalized.contains("material_store")
                || normalized.contains("merchant")
                || normalized.contains("jcsd");
    }

    public static boolean isBakery(@Nullable String buildingFileName) {
        CommercialBuildingConfig config = getCommercialConfig(buildingFileName);
        if (config != null) {
            String buildingId = normalize(config.getBuildingId());
            String buildingName = normalize(config.getBuildingName());
            String jobType = normalize(config.getJobType());
            if (buildingId.contains("bakery") || buildingName.contains("bakery") || buildingName.contains("面包")
                    || jobType.contains("bakery")) {
                return true;
            }
        }

        String normalized = normalize(buildingFileName);
        return normalized.contains("bakery") || normalized.contains("bread");
    }

    private static void playVoiceAt(ServerLevel level, BlockPos pos, SoundEvent sound, float pitch, SoundSource source, double hearingRadius) {
        if (level == null || pos == null || sound == null) {
            return;
        }
        if (level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, hearingRadius, false) == null) {
            return;
        }

        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                sound,
                source,
                DEFAULT_VOLUME,
                pitch
        );
    }

    private static SoundEvent getGenderedSound(@Nullable CustomEntity npc,
                                               RegistryObject<SoundEvent> maleSound,
                                               RegistryObject<SoundEvent> femaleSound) {
        Gender gender = npc != null ? npc.getGender() : Gender.MALE;
        return gender == Gender.FEMALE
                ? Objects.requireNonNull(femaleSound.get())
                : Objects.requireNonNull(maleSound.get());
    }

    private static float getVoicePitch(@Nullable CustomEntity npc) {
        if (npc == null) {
            return 1.0F;
        }
        return 0.95F + npc.getRandom().nextFloat() * 0.1F;
    }

    @Nullable
    private static CommercialBuildingConfig getCommercialConfig(@Nullable String buildingFileName) {
        if (buildingFileName == null || buildingFileName.isBlank()) {
            return null;
        }
        try {
            return CommercialBuildingManager.getConfig(buildingFileName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
