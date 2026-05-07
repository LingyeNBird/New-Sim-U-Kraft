package com.xiaoliang.simukraft.block;

import com.xiaoliang.simukraft.building.ControlBoxDataManager;
import com.xiaoliang.simukraft.building.ConstructionBoxMapping;
import com.xiaoliang.simukraft.building.MedicalBuildingManager;
import com.xiaoliang.simukraft.client.gui.OtherControlBoxScreen;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.service.EmploymentCommands;
import com.xiaoliang.simukraft.employment.service.EmploymentServices;
import com.xiaoliang.simukraft.network.EmploymentCommandPacket;
import com.xiaoliang.simukraft.utils.CityMessageUtils;
import com.xiaoliang.simukraft.utils.FileUtils;
import com.xiaoliang.simukraft.utils.NPCDataManager;
import com.xiaoliang.simukraft.world.BaseBuildingHiredData;
import com.xiaoliang.simukraft.world.ConstructionBoxData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("null")
public class OtherControlBoxBlock extends Block {
    public OtherControlBoxBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(Objects.requireNonNull(MapColor.METAL))
                .strength(0.8F)
                .sound(Objects.requireNonNull(SoundType.METAL)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                        @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        ConstructionBoxData.BoxInfo boxInfo = ConstructionBoxMapping.getBoxInfo(level, pos);
        String buildingFileName = boxInfo != null ? boxInfo.buildingFileName : null;
        String buildingName = boxInfo != null ? boxInfo.buildingName : null;
        UUID cityId = boxInfo != null ? boxInfo.cityId : null;

        if ((buildingName == null || buildingName.isBlank()) && buildingFileName != null) {
            String configName = MedicalBuildingManager.getBuildingName(buildingFileName);
            if (configName != null && !configName.isBlank()) {
                buildingName = configName;
            }
        }
        if (buildingName == null || buildingName.isBlank()) {
            buildingName = buildingFileName != null && !buildingFileName.isBlank() ? buildingFileName : "unknown";
        }

        ControlBoxDataManager.writeOtherControlBox(server, pos, buildingName, buildingFileName, null, cityId);
        if (buildingFileName != null && !buildingFileName.isBlank()) {
            FileUtils.updateSkFileCache("other", pos, buildingFileName);
        }
        ConstructionBoxMapping.removePendingBox(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            var releaseResult = EmploymentServices.get(server).onWorkBlockRemoved(
                    new EmploymentCommands.WorkBlockRemovedCommand(serverLevel.dimension().location().toString(), pos)
            );
            if (releaseResult.success() && releaseResult.assignment() != null) {
                EmploymentCommandPacket.applyFireSideEffectsAndBroadcast(server, releaseResult.assignment(), false);

                var npc = BaseBuildingHiredData.findNPCByUuid(server, releaseResult.assignment().npcUuid());
                if (releaseResult.assignment().jobType() == JobType.DOCTOR) {
                    String npcName = npc != null ? npc.getFullName() : NPCDataManager.getNPCNameByUUID(server, releaseResult.assignment().npcUuid());
                    CityMessageUtils.sendToCityGroup(
                            server,
                            npc != null ? npc.getCityId() : null,
                            Component.translatable("message.simukraft.other_control_box.destroyed", npcName)
                    );
                }
            }

            FileUtils.removeFromSkFileCache("other", pos);
            ControlBoxDataManager.deleteControlBox(server, pos, "other_control_box");
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new OtherControlBoxScreen(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
