package com.xiaoliang.simukraft.event;

import com.xiaoliang.simukraft.entity.CustomEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * 当NPC传送完成时触发的事件
 */
@Cancelable
public class NPCTeleportCompleteEvent extends EntityEvent {
    private final CustomEntity npc;
    private final BlockPos targetPos;
    private final Level level;
    
    public NPCTeleportCompleteEvent(CustomEntity npc, BlockPos targetPos, Level level) {
        super(npc);
        this.npc = npc;
        this.targetPos = targetPos;
        this.level = level;
    }
    
    public CustomEntity getNPC() {
        return npc;
    }
    
    public BlockPos getTargetPos() {
        return targetPos;
    }
    
    public Level getLevel() {
        return level;
    }
}