package com.xiaoliang.simukraft.entity.ai;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import com.xiaoliang.simukraft.entity.WorkSubState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

@SuppressWarnings("null")
public class IdleNearbyStrollGoal extends Goal {
    private static final double MOVE_SPEED = 0.8D;
    private static final int SEARCH_RADIUS_XZ = 6;
    private static final int SEARCH_RADIUS_Y = 2;
    private static final int MAX_TRIES = 12;
    private static final int START_INTERVAL_MIN = 40;
    private static final int START_INTERVAL_RANDOM = 60;

    private final CustomEntity npc;
    private int nextStartTick;
    @Nullable
    private BlockPos targetPos;

    public IdleNearbyStrollGoal(CustomEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!npc.canStartAutonomousGoal()) return false;
        if (!(npc.level() instanceof ServerLevel level)) return false;
        if (npc.tickCount < nextStartTick) return false;
        if (npc.isSleeping()) return false;
        if (npc.getHunger() <= 12) return false;
        if (npc.getWorkStatus() != WorkStatus.IDLE) return false;
        if (!"unemployed".equals(npc.getJob())) return false;
        if (npc.getWorkSubState() != WorkSubState.NONE) return false;

        targetPos = findNearbyTarget(level);
        if (targetPos == null) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (targetPos == null) return;
        if (ServerConfig.isDebugLogEnabled()) {
            Simukraft.LOGGER.info("[IdleNearbyStrollGoal] NPC {} 开始附近闲逛，当前位置: {}，目标位置: {}，目的: 无工作闲逛", npc.getFullName(), npc.blockPosition(), targetPos);
        }
        npc.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, MOVE_SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.level().isClientSide) return false;
        if (npc.isUsingCustomPathfinder()) return false;
        if (npc.isSleeping()) return false;
        if (npc.getHunger() <= 12) return false;
        if (npc.getWorkStatus() != WorkStatus.IDLE) return false;
        if (!"unemployed".equals(npc.getJob())) return false;
        if (npc.getWorkSubState() != WorkSubState.NONE) return false;
        return !npc.getNavigation().isDone();
    }

    @Override
    public void stop() {
        targetPos = null;
        npc.getNavigation().stop();
        nextStartTick = npc.tickCount + START_INTERVAL_MIN + npc.getRandom().nextInt(START_INTERVAL_RANDOM + 1);
    }

    @Nullable
    private BlockPos findNearbyTarget(ServerLevel level) {
        BlockPos origin = npc.blockPosition();

        for (int i = 0; i < MAX_TRIES; i++) {
            int offsetX = npc.getRandom().nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;
            int offsetY = npc.getRandom().nextInt(SEARCH_RADIUS_Y * 2 + 1) - SEARCH_RADIUS_Y;
            int offsetZ = npc.getRandom().nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;

            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }

            BlockPos candidate = origin.offset(offsetX, offsetY, offsetZ);
            BlockPos groundPos = findStandablePos(level, candidate);
            if (groundPos == null) {
                continue;
            }
            if (origin.distSqr(groundPos) < 4.0D) {
                continue;
            }
            return groundPos;
        }

        return null;
    }

    @Nullable
    private BlockPos findStandablePos(ServerLevel level, BlockPos candidate) {
        for (int yOffset = 2; yOffset >= -2; yOffset--) {
            BlockPos pos = candidate.offset(0, yOffset, 0);
            if (canStandAt(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean canStandAt(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }

        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
