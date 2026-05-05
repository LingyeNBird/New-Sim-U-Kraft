package com.xiaoliang.simukraft.mixin;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldCreationUiState.class)
public abstract class MixinWorldCreationUiState {
    @Shadow
    @Final
    private List<WorldCreationUiState.WorldTypeEntry> normalPresetList;

    @Shadow
    private WorldCreationUiState.WorldTypeEntry worldType;

    @Shadow
    public abstract void setGameMode(WorldCreationUiState.SelectedGameMode mode);

    @Shadow
    public abstract void setAllowCheats(boolean allowCheats);

    @Shadow
    public abstract void setBonusChest(boolean bonusChest);

    @Shadow
    public abstract void onChanged();

    @Inject(method = "updatePresetLists", at = @At("TAIL"))
    private void simukraft$moveDebugPresetToSecond(CallbackInfo ci) {
        int debugIndex = -1;
        for (int i = 0; i < this.normalPresetList.size(); i++) {
            if (simukraft$isDebugPreset(this.normalPresetList.get(i))) {
                debugIndex = i;
                break;
            }
        }

        if (debugIndex <= 1) {
            return;
        }

        List<WorldCreationUiState.WorldTypeEntry> reordered = new ArrayList<>(this.normalPresetList);
        WorldCreationUiState.WorldTypeEntry debugEntry = reordered.remove(debugIndex);
        reordered.add(1, debugEntry);
        this.normalPresetList.clear();
        this.normalPresetList.addAll(reordered);
    }

    @Inject(method = "setWorldType", at = @At("TAIL"))
    private void simukraft$applyDebugPresetDefaults(WorldCreationUiState.WorldTypeEntry entry, CallbackInfo ci) {
        if (!simukraft$isDebugPreset(entry)) {
            return;
        }

        this.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        this.setAllowCheats(true);
        this.setBonusChest(false);
        this.onChanged();
    }

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void simukraft$lockGameMode(WorldCreationUiState.SelectedGameMode mode, CallbackInfo ci) {
        if (simukraft$isDebugPreset(this.worldType) && mode != WorldCreationUiState.SelectedGameMode.CREATIVE) {
            ci.cancel();
        }
    }

    @Inject(method = "setAllowCheats", at = @At("HEAD"), cancellable = true)
    private void simukraft$lockCheats(boolean allowCheats, CallbackInfo ci) {
        if (simukraft$isDebugPreset(this.worldType) && !allowCheats) {
            ci.cancel();
        }
    }

    @Inject(method = "setBonusChest", at = @At("HEAD"), cancellable = true)
    private void simukraft$lockBonusChest(boolean bonusChest, CallbackInfo ci) {
        if (simukraft$isDebugPreset(this.worldType) && bonusChest) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean simukraft$isDebugPreset(WorldCreationUiState.WorldTypeEntry entry) {
        if (entry == null) {
            return false;
        }

        Holder<WorldPreset> presetHolder = entry.preset();
        if (presetHolder == null) {
            return false;
        }

        return presetHolder.unwrapKey()
                .map(key -> ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "debug_void").equals(key.location()))
                .orElse(false);
    }
}
