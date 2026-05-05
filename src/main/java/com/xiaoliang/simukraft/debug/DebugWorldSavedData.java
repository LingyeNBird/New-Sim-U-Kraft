package com.xiaoliang.simukraft.debug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

/**
 * 调试世界展区生成状态。
 */
public class DebugWorldSavedData extends SavedData {
    private static final String DATA_NAME = "simukraft_debug_world_state";

    private boolean exhibitionGenerated;
    private int exhibitionLayoutVersion;
    private boolean debugCityInitialized;

    public boolean isExhibitionGenerated() {
        return exhibitionGenerated;
    }

    public int getExhibitionLayoutVersion() {
        return exhibitionLayoutVersion;
    }

    public void markExhibitionGenerated(int layoutVersion) {
        if (!exhibitionGenerated || exhibitionLayoutVersion != layoutVersion) {
            exhibitionGenerated = true;
            exhibitionLayoutVersion = layoutVersion;
            setDirty();
        }
    }

    public void resetExhibitionGenerated() {
        if (exhibitionGenerated || exhibitionLayoutVersion != 0) {
            exhibitionGenerated = false;
            exhibitionLayoutVersion = 0;
            setDirty();
        }
    }

    public boolean isDebugCityInitialized() {
        return debugCityInitialized;
    }

    public void markDebugCityInitialized() {
        if (!debugCityInitialized) {
            debugCityInitialized = true;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        tag.putBoolean("exhibitionGenerated", exhibitionGenerated);
        tag.putInt("exhibitionLayoutVersion", exhibitionLayoutVersion);
        tag.putBoolean("debugCityInitialized", debugCityInitialized);
        return tag;
    }

    public static DebugWorldSavedData load(CompoundTag tag) {
        DebugWorldSavedData data = new DebugWorldSavedData();
        data.exhibitionGenerated = tag.getBoolean("exhibitionGenerated");
        data.exhibitionLayoutVersion = tag.getInt("exhibitionLayoutVersion");
        data.debugCityInitialized = tag.getBoolean("debugCityInitialized");
        return data;
    }

    public static DebugWorldSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server != null ? server.overworld() : level;
        if (overworld == null) {
            overworld = level;
        }

        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                DebugWorldSavedData::load,
                DebugWorldSavedData::new,
                DATA_NAME
        );
    }
}
