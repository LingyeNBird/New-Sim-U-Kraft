package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.entity.FloatingBuildBoxEntity;
import com.xiaoliang.simukraft.init.ModEntities;
import com.xiaoliang.simukraft.world.BuildBoxEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * 悬浮建筑盒实体管理器：统一处理生成与移除，避免重复生成和脏数据。
 */
public final class BuildBoxFloatingEntityManager {
    private static final double FLOATING_BOX_X_OFFSET = 1.1D;

    private BuildBoxFloatingEntityManager() {
    }

    public static void ensureSpawned(ServerLevel level, BlockPos buildBoxPos, Player anchorPlayer) {
        if (level == null || buildBoxPos == null) {
            return;
        }

        BuildBoxEntityData entityData = BuildBoxEntityData.get(level);
        if (entityData.hasEntityForBuildBox(buildBoxPos)) {
            UUID trackedUuid = entityData.getEntityUuidForBuildBox(buildBoxPos);
            if (trackedUuid != null) {
                Entity trackedEntity = level.getEntity(trackedUuid);
                if (trackedEntity instanceof FloatingBuildBoxEntity && trackedEntity.isAlive()) {
                    // 已存在实体也对齐到最新偏移，避免旧存档位置不更新
                    trackedEntity.setPos(
                            buildBoxPos.getX() + 0.5D + FLOATING_BOX_X_OFFSET,
                            buildBoxPos.getY() + 2.5D,
                            buildBoxPos.getZ() + 0.5D
                    );
                    return;
                }
            }
            // 记录存在但实体已失效，先清理再重建
            entityData.removeBuildBoxEntity(buildBoxPos);
            entityData.setDirty();
        }

        FloatingBuildBoxEntity entity = ModEntities.FLOATING_BUILD_BOX.get().create(level);
        if (entity == null) {
            return;
        }

        double x = buildBoxPos.getX() + 0.5D + FLOATING_BOX_X_OFFSET;
        double y = buildBoxPos.getY() + 2.5D;
        double z = buildBoxPos.getZ() + 0.5D;

        if (anchorPlayer != null) {
            float playerYaw = anchorPlayer.getYRot();
            double angleRad = Math.toRadians(playerYaw);
            x += -Math.sin(angleRad - Math.PI / 2D) * 1.5D;
            z += Math.cos(angleRad - Math.PI / 2D) * 1.5D;
        }

        entity.setPos(x, y, z);
        level.addFreshEntity(entity);

        entityData.addBuildBoxEntity(buildBoxPos, Objects.requireNonNull(entity.getUUID()));
        entityData.setDirty();
    }

    public static void remove(ServerLevel level, BlockPos buildBoxPos) {
        if (level == null || buildBoxPos == null) {
            return;
        }

        BuildBoxEntityData entityData = BuildBoxEntityData.get(level);
        if (!entityData.hasEntityForBuildBox(buildBoxPos)) {
            return;
        }

        UUID entityUuid = entityData.getEntityUuidForBuildBox(buildBoxPos);
        if (entityUuid != null) {
            Entity entity = level.getEntity(entityUuid);
            if (entity instanceof FloatingBuildBoxEntity) {
                entity.discard();
            }
        }

        entityData.removeBuildBoxEntity(buildBoxPos);
        entityData.setDirty();
    }
}
