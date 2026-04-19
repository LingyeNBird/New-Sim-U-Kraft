package com.xiaoliang.simukraft.block;

import com.xiaoliang.simukraft.building.ControlBoxDataManager;
import com.xiaoliang.simukraft.utils.ClientRuntimeBridge;
import com.xiaoliang.simukraft.utils.ResidentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * 住宅控制盒方块
 * 统一的住宅类型控制盒，从同名sk文件获取基本信息
 */
public class ResidentialControlBoxBlock extends Block {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public ResidentialControlBoxBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.METAL))
                .strength(1.0F)
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(
                Objects.requireNonNull(state),
                Objects.requireNonNull(level),
                Objects.requireNonNull(pos),
                Objects.requireNonNull(oldState),
                isMoving
        );

        if (!level.isClientSide && !oldState.is(Objects.requireNonNull(state.getBlock()))) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                // 修复：传入level参数以支持持久化存储
                // 从ConstructionBoxMapping获取建筑名称和城市ID
                com.xiaoliang.simukraft.world.ConstructionBoxData.BoxInfo boxInfo = 
                    com.xiaoliang.simukraft.building.ConstructionBoxMapping.getBoxInfo(level, pos);
                String buildingFileName = boxInfo != null ? boxInfo.buildingFileName : "unknown";
                java.util.UUID cityId = boxInfo != null ? boxInfo.cityId : null;

                // 使用ControlBoxDataManager写入数据（使用英文文件名以便读取租金）
                ControlBoxDataManager.writeResidentialControlBox(server, pos, buildingFileName, null, cityId);

                // 尝试为同城市的NPC分配住宅（优先分配给npcid小的）
                if (cityId != null) {
                    boolean assigned = ResidentManager.assignResidenceToCityNPCs(server, pos, cityId);
                    if (!assigned) {
                        LOGGER.info("[ResidentialControlBoxBlock] 住宅 {} 暂时空置，等待新NPC生成", pos);
                    }
                } else {
                    LOGGER.warn("[ResidentialControlBoxBlock] 警告：控制盒没有城市ID，无法分配住宅");
                }

                // 修复：传入level参数以支持持久化存储
                // 移除已处理的控制盒
                com.xiaoliang.simukraft.building.ConstructionBoxMapping.removePendingBox(level, pos);
            }
        }
    }

    @Override
    public void playerWillDestroy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Player player) {
        super.playerWillDestroy(level, pos, state, player);
        
        if (!level.isClientSide) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                // 1. 先让该住宅的NPC进入无家可归状态
                com.xiaoliang.simukraft.utils.ResidentManager.removeResidentFromResidence(server, pos);
                
                // 2. 使用ControlBoxDataManager删除数据
                ControlBoxDataManager.deleteControlBox(server, pos, "residential_control_box");
            }
        }
    }
    
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            try {
                ClientRuntimeBridge.openScreen(
                        "com.xiaoliang.simukraft.client.gui.ResidentialControlBoxScreen",
                        new Class<?>[]{BlockPos.class},
                        pos
                );
            } catch (Exception e) {
                System.err.println("[ResidentialControlBoxBlock] 打开GUI失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return Objects.requireNonNull(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
