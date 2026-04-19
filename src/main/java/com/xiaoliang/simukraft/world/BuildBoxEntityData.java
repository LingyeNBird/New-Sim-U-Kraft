package com.xiaoliang.simukraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public final class BuildBoxEntityData extends SavedData {
    private static final String STORAGE_ID = "buildbox_entities";
    private static final String TAG_ENTRIES = "buildBoxEntities";

    private final Map<BlockPos, UUID> buildBoxEntityMap = new ConcurrentHashMap<>();

    private BuildBoxEntityData() {
    }

    public static BuildBoxEntityData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Level must be a ServerLevel");
        }

        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(
                BuildBoxEntityData::load,
                BuildBoxEntityData::new,
                STORAGE_ID
        );
    }

    public boolean hasEntityForBuildBox(@Nonnull BlockPos pos) {
        return buildBoxEntityMap.containsKey(Objects.requireNonNull(pos));
    }

    public UUID getEntityUuidForBuildBox(@Nonnull BlockPos pos) {
        return buildBoxEntityMap.get(Objects.requireNonNull(pos));
    }

    public void addBuildBoxEntity(@Nonnull BlockPos pos, @Nonnull UUID entityUuid) {
        buildBoxEntityMap.put(Objects.requireNonNull(pos), Objects.requireNonNull(entityUuid));
    }

    public void removeBuildBoxEntity(@Nonnull BlockPos pos) {
        buildBoxEntityMap.remove(Objects.requireNonNull(pos));
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : buildBoxEntityMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("pos", Objects.requireNonNull(NbtUtils.writeBlockPos(Objects.requireNonNull(entry.getKey()))));
            entryTag.putUUID("entityUuid", Objects.requireNonNull(entry.getValue()));
            listTag.add(entryTag);
        }
        tag.put(TAG_ENTRIES, listTag);
        return tag;
    }

    public static BuildBoxEntityData load(@Nonnull CompoundTag tag) {
        BuildBoxEntityData data = new BuildBoxEntityData();
        if (tag.contains(TAG_ENTRIES)) {
            ListTag listTag = tag.getList(TAG_ENTRIES, 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                BlockPos pos = Objects.requireNonNull(NbtUtils.readBlockPos(Objects.requireNonNull(entryTag.getCompound("pos"))));
                UUID entityUuid = Objects.requireNonNull(entryTag.getUUID("entityUuid"));
                data.buildBoxEntityMap.put(pos, entityUuid);
            }
        }
        return data;
    }
}
