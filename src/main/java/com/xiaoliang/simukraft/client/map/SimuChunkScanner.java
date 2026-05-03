package com.xiaoliang.simukraft.client.map;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * 区块扫描器：将实际加载的区块转换为地图颜色数据写入 {@link SimuMapRegionData}。
 * 参考 FTB Chunks 的区块数据收集逻辑，但完全独立实现。
 */
public class SimuChunkScanner {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SimuChunkScanner() {
    }

    /**
     * 扫描指定区块并将结果写入区域数据。
     *
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param region 目标区域
     * @return true 如果扫描成功
     */
    public static boolean scanChunk(int chunkX, int chunkZ, SimuMapRegion region) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return false;

        ChunkAccess chunk = level.getChunk(chunkX, chunkZ, Objects.requireNonNull(ChunkStatus.FULL), false);
        if (chunk == null) return false;

        SimuMapRegionData data = region.getOrCreateData();
        SimuBlockColors colors = SimuBlockColors.getInstance();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        int regOriginX = region.regionX * 512;
        int regOriginZ = region.regionZ * 512;
        int minBuild = level.getMinBuildHeight();

        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int worldX = baseX + localX;
                int worldZ = baseZ + localZ;

                int regionLocalX = worldX - regOriginX;
                int regionLocalZ = worldZ - regOriginZ;

                if (regionLocalX < 0 || regionLocalX >= 512 || regionLocalZ < 0 || regionLocalZ >= 512) {
                    continue;
                }

                int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                pos.set(worldX, topY, worldZ);

                BlockState topState = level.getBlockState(pos);

                int y = topY;
                while (topState.isAir() && y > minBuild) {
                    y--;
                    pos.setY(y);
                    topState = level.getBlockState(pos);
                }

                pos.set(worldX, topY, worldZ);
                FluidState fluidState = level.getFluidState(pos);
                if (!fluidState.isEmpty()) {
                    pos.set(worldX, y, worldZ);
                    int waterColor = colors.getBlockColor(Blocks.WATER.defaultBlockState(), level, pos);
                    int bottomColor = colors.getBlockColor(topState, level, pos);
                    int blended = SimuBlockColors.blendColors(bottomColor, (waterColor & 0x00FFFFFF) | 0xAA000000);

                    int light = 15;
                    try {
                        pos.set(worldX, topY + 1, worldZ);
                        light = level.getBrightness(LightLayer.BLOCK, pos);
                        light = Math.max(light, level.getBrightness(LightLayer.SKY, pos));
                    } catch (Exception ignored) {
                    }

                    data.setData(regionLocalX, regionLocalZ, (short) topY, blended, true, light);
                    continue;
                }

                pos.set(worldX, y, worldZ);
                int blockColor = colors.getBlockColor(topState, level, pos);

                int light = 15;
                try {
                    pos.set(worldX, y + 1, worldZ);
                    light = level.getBrightness(LightLayer.SKY, pos);
                    light = Math.max(light, level.getBrightness(LightLayer.BLOCK, pos));
                } catch (Exception ignored) {
                }

                data.setData(regionLocalX, regionLocalZ, (short) y, blockColor, false, light);
            }
        }

        return true;
    }

    /**
     * 扫描玩家周围的所有已加载区块。
     *
     * @param manager 地图管理器
     * @param radius  扫描半径（区块数）
     */
    public static void scanAroundPlayer(SimuMapManager manager, int radius) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                if (!level.hasChunk(cx, cz)) continue;

                int regionX = cx >> 5;
                int regionZ = cz >> 5;
                SimuMapRegion region = manager.getOrCreateRegion(regionX, regionZ);

                try {
                    scanChunk(cx, cz, region);
                } catch (Exception e) {
                    LOGGER.debug("Simukraft: Failed to scan chunk ({}, {}): {}", cx, cz, e.getMessage());
                }
            }
        }
    }
}
