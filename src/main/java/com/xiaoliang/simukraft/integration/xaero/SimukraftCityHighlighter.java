package com.xiaoliang.simukraft.integration.xaero;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SimukraftCityHighlighter extends ChunkHighlighter {

    private static final int[] CITY_COLORS = {
        0xFF1A6BB5, 0xFF2E7ECA, 0xFF3DA0DD, 0xFF4AB0EE, 0xFF5ABFFF,
        0xFF6B3DB5, 0xFF7A50CA, 0xFF8A60DD, 0xFF9A70EE, 0xFFAA80FF,
        0xFFB5551A, 0xFFCA6A2E, 0xFFDD7A3D, 0xFFEE8A4A, 0xFFFF9A5A,
        0xFF1AB55A, 0xFF2ECA6E, 0xFF3DDD7D, 0xFF4AEE8D, 0xFF5AFF9D,
    };

    public SimukraftCityHighlighter() {
        super(true);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        Map<UUID, Set<Long>> allCityChunks = ClientCityChunkData.getInstance().getAllCityChunks();
        if (allCityChunks.isEmpty()) return false;
        for (Set<Long> chunks : allCityChunks.values()) {
            for (long chunkLong : chunks) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);
                int rx = cx >> 5;
                int rz = cz >> 5;
                if (rx == regionX && rz == regionZ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        ClientCityChunkData ccd = ClientCityChunkData.getInstance();
        long chunkLong = ChunkPos.asLong(chunkX, chunkZ);
        UUID ownerCity = ccd.getChunkOwner(chunkLong);
        if (ownerCity == null) return null;

        int colorIndex = Math.abs(ownerCity.hashCode()) % CITY_COLORS.length;
        int rgb = CITY_COLORS[colorIndex];
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int centerColor = b << 24 | g << 16 | r << 8 | 0x40;
        int borderColor  = b << 24 | g << 16 | r << 8 | 0xCC;

        long topLong    = ChunkPos.asLong(chunkX, chunkZ - 1);
        long rightLong  = ChunkPos.asLong(chunkX + 1, chunkZ);
        long bottomLong = ChunkPos.asLong(chunkX, chunkZ + 1);
        long leftLong   = ChunkPos.asLong(chunkX - 1, chunkZ);

        this.resultStore[0] = centerColor;
        this.resultStore[1] = !ownerCity.equals(ccd.getChunkOwner(topLong))    ? borderColor : centerColor;
        this.resultStore[2] = !ownerCity.equals(ccd.getChunkOwner(rightLong))  ? borderColor : centerColor;
        this.resultStore[3] = !ownerCity.equals(ccd.getChunkOwner(bottomLong)) ? borderColor : centerColor;
        this.resultStore[4] = !ownerCity.equals(ccd.getChunkOwner(leftLong))   ? borderColor : centerColor;
        return this.resultStore;
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        Map<UUID, Set<Long>> allCityChunks = ClientCityChunkData.getInstance().getAllCityChunks();
        long accumulator = 0L;
        for (Set<Long> chunks : allCityChunks.values()) {
            for (long chunkLong : chunks) {
                int cx = ChunkPos.getX(chunkLong);
                int cz = ChunkPos.getZ(chunkLong);
                if ((cx >> 5) == regionX && (cz >> 5) == regionZ) {
                    accumulator = accumulator * 37L + chunkLong;
                }
            }
        }
        return (int)(accumulator ^ (accumulator >>> 32));
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        long chunkLong = ChunkPos.asLong(chunkX, chunkZ);
        return ClientCityChunkData.getInstance().isChunkOwned(chunkLong);
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        long chunkLong = ChunkPos.asLong(chunkX, chunkZ);
        UUID cityId = ClientCityChunkData.getInstance().getChunkOwner(chunkLong);
        if (cityId == null) return null;

        boolean isCurrentCity = cityId.equals(ClientCityChunkData.getInstance().getCityId());
        String label = isCurrentCity ? "[My City]" : "[City]";

        return Component.literal("■ " + label).withStyle(s -> s.withColor(
            isCurrentCity ? ChatFormatting.AQUA.getColor() : ChatFormatting.YELLOW.getColor()
        ));
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> list, ResourceKey<Level> dimension, int blockX, int blockZ, int width) {
        long chunkLong = ChunkPos.asLong(blockX >> 4, blockZ >> 4);
        UUID cityId = ClientCityChunkData.getInstance().getChunkOwner(chunkLong);
        if (cityId == null) return;
        boolean isCurrentCity = cityId.equals(ClientCityChunkData.getInstance().getCityId());
        list.add(Component.literal(isCurrentCity ? "Simukraft City (yours)" : "Simukraft City")
                .withStyle(isCurrentCity ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
    }
}
