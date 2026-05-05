package com.xiaoliang.simukraft.debug;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.block.ResidentialControlBoxBlock;
import com.xiaoliang.simukraft.building.ControlBoxDataManager;
import com.xiaoliang.simukraft.building.ConstructionBoxMapping;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.init.ModBlocks;
import com.xiaoliang.simukraft.init.ModEntities;
import com.xiaoliang.simukraft.network.NetworkManager;
import com.xiaoliang.simukraft.utils.BuildingDataManager;
import com.xiaoliang.simukraft.world.CityChunkData;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 调试世界展区生成器。
 */
@SuppressWarnings("null")
public final class DebugWorldGenerator {
    public static final ResourceLocation DEBUG_DIMENSION_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "debug_void");

    private static final int BASE_Y = 64;
    private static final int WORLD_EDIT_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int BLOCK_GRID_SPACING = 6;
    private static final int BUILDING_SPACING = 1;
    private static final int MAX_BUILDINGS_PER_SEGMENT_ROW = 10;
    private static final int BUILDING_START_OFFSET_Z = 72;
    private static final int EXHIBITION_CLEAR_MARGIN = 8;
    private static final int EXHIBITION_CLEAR_HEIGHT = 128;
    private static final int EXHIBITION_CLEAR_HALF_WIDTH = 1024;
    private static final int EXHIBITION_CLEAR_DEPTH = 2048;
    private static final int EXHIBITION_LAYOUT_VERSION = 3;
    private static final int DEBUG_RESIDENT_SPAWN_LIMIT = 5;//调试世界居民分配最大数量,条这个就行了
    private static final int MAX_STRUCTURE_DIMENSION = 96;
    private static final int MAX_STRUCTURE_VOLUME = 180000;
    private static final long MAX_NBT_FILE_BYTES = 2_000_000L;
    private static final String DEBUG_CITY_NAME = "debug";
    private static final String DEBUG_CITY_MAYOR_NAME = "debug_system";
    private static final UUID DEBUG_CITY_MAYOR_ID =
            UUID.nameUUIDFromBytes("simukraft:debug_city_mayor".getBytes(StandardCharsets.UTF_8));
    private static final int DEBUG_CITY_CORE_OFFSET_Z = 16;
    private static final List<String> BUILDING_CATEGORIES = List.of("residential", "commercial", "industry", "other");

    private DebugWorldGenerator() {
    }

    public static boolean isDebugWorld(ServerLevel level) {
        if (level == null) {
            return false;
        }

        return level.dimensionTypeRegistration().unwrapKey()
                .map(key -> DEBUG_DIMENSION_TYPE_ID.equals(key.location()))
                .orElse(false);
    }

    public static synchronized void ensureGenerated(ServerLevel level) {
        ensureGenerated(level, null);
    }

    public static synchronized void ensureGenerated(ServerLevel level, @Nullable ServerPlayer player) {
        if (level == null || !isDebugWorld(level)) {
            return;
        }

        DebugWorldSavedData data = DebugWorldSavedData.get(level);
        DebugCityContext debugCity = ensureDebugCity(level, player, data);
        if (debugCity == null) {
            return;
        }

        if (data.isExhibitionGenerated() && data.getExhibitionLayoutVersion() >= EXHIBITION_LAYOUT_VERSION) {
            return;
        }

        try {
            if (data.isExhibitionGenerated()) {
                clearExhibitionArea(level);
            }
            GenerationTask.create(level, debugCity).generateAll(level);
            data.markExhibitionGenerated(EXHIBITION_LAYOUT_VERSION);
            Simukraft.LOGGER.info("[DebugWorldGenerator] 调试世界展区生成完成，布局版本={}", EXHIBITION_LAYOUT_VERSION);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DebugWorldGenerator] 调试世界展区生成失败", e);
        }
    }

    public static void ensurePlayerSpawnSafety(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!isDebugWorld(level)) {
            return;
        }

        BlockPos spawnCenter = getDebugSpawnCenter();
        BlockPos safeStandPos = spawnCenter.above();
        level.setDefaultSpawnPos(spawnCenter, 0.0F);

        // 调试世界是纯虚空维度，进入时直接拉回平台，避免先在原始虚空出生点下落。
        player.teleportTo(level, safeStandPos.getX() + 0.5D, safeStandPos.getY(), safeStandPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    @Nullable
    private static DebugCityContext ensureDebugCity(ServerLevel level,
                                                   @Nullable ServerPlayer player,
                                                   DebugWorldSavedData data) {
        BlockPos cityCorePos = getDebugCityCorePos();
        CityData cityData = CityData.get(level);
        CityData.CityInfo cityInfo = cityData.getCityByCorePos(cityCorePos);

        if (cityInfo == null) {
            if (player == null) {
                return null;
            }
            cityInfo = createDebugCityForPlayer(level, cityData, player, cityCorePos);
            data.markDebugCityInitialized();
        } else if (player != null && shouldTransferDebugCityToPlayer(cityInfo, player)) {
            String playerName = player.getGameProfile().getName();
            if (cityData.transferMayor(cityInfo.getCityId(), playerName, player.getUUID(), level)) {
                cityInfo = cityData.getCity(cityInfo.getCityId());
                data.markDebugCityInitialized();
                NetworkManager.broadcastAllCityCores(level.getServer());
                Simukraft.LOGGER.info("[DebugWorldGenerator] 调试城市市长已直接转移给玩家 {}，无需重建展区", playerName);
            }
        } else if (!level.getBlockState(cityCorePos).is(ModBlocks.CITY_CORE.get())) {
            setBlock(level, cityCorePos, ModBlocks.CITY_CORE.get().defaultBlockState());
        }

        if (!data.isDebugCityInitialized()) {
            data.markDebugCityInitialized();
        }

        if (player != null) {
            ensureDebugOfficial(level, cityData, cityInfo, player);
        }

        return new DebugCityContext(cityInfo.getCityId(), cityInfo.getCityCorePos());
    }

    private static void ensureDebugOfficial(ServerLevel level,
                                            CityData cityData,
                                            CityData.CityInfo cityInfo,
                                            ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        if (!cityInfo.isMayor(player.getUUID()) && !cityInfo.isOfficial(playerName)) {
            cityData.addOfficialToCity(cityInfo.getCityId(), playerName, player.getUUID(), level);
            Simukraft.LOGGER.info("[DebugWorldGenerator] 玩家 {} 已加入调试城市 {} 的官员列表", playerName, cityInfo.getCityName());
        }

        cityData.refreshPlayerCityAccess(player);
        cityData.syncCityHUDData(cityInfo.getCityId(), level);
    }

    private static boolean shouldTransferDebugCityToPlayer(CityData.CityInfo cityInfo, ServerPlayer player) {
        return cityInfo != null
                && player != null
                && DEBUG_CITY_NAME.equals(cityInfo.getCityName())
                && DEBUG_CITY_MAYOR_ID.equals(cityInfo.getMayorId())
                && !cityInfo.isMayor(player.getUUID());
    }

    private static CityData.CityInfo createDebugCityForPlayer(ServerLevel level,
                                                              CityData cityData,
                                                              ServerPlayer player,
                                                              BlockPos cityCorePos) {
        String playerName = player.getGameProfile().getName();
        setBlock(level, cityCorePos, ModBlocks.CITY_CORE.get().defaultBlockState());
        CityData.CityInfo cityInfo = cityData.createCity(playerName, player.getUUID(), DEBUG_CITY_NAME, cityCorePos, level);

        CityChunkData cityChunkData = CityChunkData.get(level);
        ChunkPos cityCoreChunk = new ChunkPos(cityCorePos);
        if (cityChunkData.isAreaAvailable(cityCoreChunk) || cityChunkData.getCityChunks(cityInfo.getCityId()).isEmpty()) {
            cityChunkData.assignAreaToCity(cityInfo.getCityId(), cityCoreChunk);
        }

        com.xiaoliang.simukraft.integration.IntegrationBridge.onCityChunksClaimed(
                level.getServer(),
                cityInfo.getCityId(),
                cityInfo.getMayorId(),
                cityChunkData.getCityChunks(cityInfo.getCityId())
        );
        NetworkManager.broadcastAllCityCores(level.getServer());
        Simukraft.LOGGER.info("[DebugWorldGenerator] 已创建调试城市 {}，市长={}, 城市ID={}",
                DEBUG_CITY_NAME, playerName, cityInfo.getCityId());
        return cityInfo;
    }

    private static BlockPos getDebugCityCorePos() {
        return new BlockPos(0, BASE_Y, DEBUG_CITY_CORE_OFFSET_Z);
    }

    private static BlockPos getDebugSpawnCenter() {
        return new BlockPos(0, BASE_Y, 0);
    }

    private static void buildSpawnPlatform(ServerLevel level, BlockPos center) {
        fill(level, center.offset(-56, -1, -56), center.offset(56, -1, 56), Blocks.POLISHED_ANDESITE.defaultBlockState());
        fill(level, center.offset(-2, -1, -2), center.offset(2, -1, 2), Blocks.SEA_LANTERN.defaultBlockState());

        setBlock(level, center.offset(-10, -1, -10), Blocks.GLOWSTONE.defaultBlockState());
        setBlock(level, center.offset(10, -1, -10), Blocks.GLOWSTONE.defaultBlockState());
        setBlock(level, center.offset(-10, -1, 10), Blocks.GLOWSTONE.defaultBlockState());
        setBlock(level, center.offset(10, -1, 10), Blocks.GLOWSTONE.defaultBlockState());
    }

    private static void clearExhibitionArea(ServerLevel level) {
        BlockPos clearOrigin = getDebugSpawnCenter().offset(0, 0, BUILDING_START_OFFSET_Z);
        BlockPos from = clearOrigin.offset(-EXHIBITION_CLEAR_HALF_WIDTH, -1, -EXHIBITION_CLEAR_MARGIN);
        BlockPos to = clearOrigin.offset(EXHIBITION_CLEAR_HALF_WIDTH, EXHIBITION_CLEAR_HEIGHT, EXHIBITION_CLEAR_DEPTH);
        fill(level, from, to, Blocks.AIR.defaultBlockState());
        Simukraft.LOGGER.info("[DebugWorldGenerator] 已清理出生平台南侧旧展区，准备按新布局重建");
    }

    private static void buildBlockDisplayFloor(ServerLevel level, BlockPos origin, int blockCount) {
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(blockCount)));
        int rows = (int) Math.ceil((double) blockCount / columns);
        int width = Math.max(18, columns * BLOCK_GRID_SPACING + 6);
        int depth = Math.max(18, rows * BLOCK_GRID_SPACING + 6);
        fill(level, origin.offset(-3, -1, -3), origin.offset(width, -1, depth), Blocks.STONE.defaultBlockState());
    }

    private static void placeBlockDisplay(ServerLevel level, BlockPos displayBase, Block block) {
        setBlock(level, displayBase, Blocks.SMOOTH_STONE.defaultBlockState());

        if (block instanceof LiquidBlock) {
            buildLiquidDisplay(level, displayBase, block.defaultBlockState());
            return;
        }

        setBlock(level, displayBase.above(), block.defaultBlockState());
    }

    private static void buildLiquidDisplay(ServerLevel level, BlockPos displayBase, BlockState liquidState) {
        fill(level, displayBase.offset(-1, 0, -1), displayBase.offset(1, 0, 1), Blocks.GLASS.defaultBlockState());
        fill(level, displayBase.offset(-1, 1, -1), displayBase.offset(1, 1, 1), Blocks.AIR.defaultBlockState());

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                    setBlock(level, displayBase.offset(dx, 1, dz), Blocks.GLASS.defaultBlockState());
                }
            }
        }

        setBlock(level, displayBase.above(), liquidState);
    }

    private static void prepareBuildingPad(ServerLevel level, BlockPos placePos, Vec3i size) {
        fill(
                level,
                placePos.offset(0, -1, 0),
                placePos.offset(Math.max(size.getX() - 1, 0), -1, Math.max(size.getZ() - 1, 0)),
                Blocks.STONE.defaultBlockState()
        );
    }

    @Nullable
    private static PlacedTemplate loadTemplate(RegistryAccess registryAccess,
                                               BuildingDataManager.BuildingInfo info,
                                               String category) {
        String fileName = info.getFileName();
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String buildingName = fileName.endsWith(".sk") ? fileName.substring(0, fileName.length() - 3) : fileName;
        Vec3i metadataSize = parseSize(info.getSize());
        if (!isSafeToLoad(metadataSize)) {
            Simukraft.LOGGER.warn("[DebugWorldGenerator] 跳过超大建筑 category={}, file={}, size={}", category, fileName, info.getSize());
            return null;
        }

        Long nbtFileSize = BuildingDataManager.getBuildingNbtFileSize(buildingName, category);
        if (nbtFileSize != null && nbtFileSize > MAX_NBT_FILE_BYTES) {
            Simukraft.LOGGER.warn("[DebugWorldGenerator] 跳过超大NBT文件 category={}, file={}, bytes={}", category, fileName, nbtFileSize);
            return null;
        }

        CompoundTag nbt = BuildingDataManager.loadBuildingData(buildingName, category);
        if (nbt == null) {
            Simukraft.LOGGER.warn("[DebugWorldGenerator] 建筑NBT不存在 category={}, file={}", category, fileName);
            return null;
        }

        try {
            TemplateMarkers markers = extractTemplateMarkers(nbt);
            StructureTemplate template = new StructureTemplate();
            template.load(registryAccess.registryOrThrow(Registries.BLOCK).asLookup(), nbt);
            if (!isSafeToLoad(template.getSize())) {
                Simukraft.LOGGER.warn("[DebugWorldGenerator] 跳过超大模板 category={}, file={}, actualSize={}", category, fileName, template.getSize());
                return null;
            }
            return new PlacedTemplate(template, template.getSize(), markers);
        } catch (Exception e) {
            Simukraft.LOGGER.error("[DebugWorldGenerator] 建筑模板加载失败 category={}, file={}", category, fileName, e);
            return null;
        }
    }

    private static boolean isSafeToLoad(@Nullable Vec3i size) {
        if (size == null) {
            return true;
        }

        int x = Math.max(size.getX(), 0);
        int y = Math.max(size.getY(), 0);
        int z = Math.max(size.getZ(), 0);
        long volume = (long) x * y * z;
        return x <= MAX_STRUCTURE_DIMENSION
                && y <= MAX_STRUCTURE_DIMENSION
                && z <= MAX_STRUCTURE_DIMENSION
                && volume <= MAX_STRUCTURE_VOLUME;
    }

    @Nullable
    private static Vec3i parseSize(@Nullable String sizeText) {
        if (sizeText == null || sizeText.isBlank()) {
            return null;
        }

        String normalized = sizeText
                .toLowerCase()
                .replace(" ", "")
                .replace("×", "x")
                .replace("*", "x");
        String[] parts = normalized.split("x");
        if (parts.length != 3) {
            return null;
        }

        try {
            return new Vec3i(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static TemplateMarkers extractTemplateMarkers(CompoundTag nbt) {
        ListTag paletteList = nbt.getList("palette", Tag.TAG_COMPOUND);
        if (paletteList.isEmpty()) {
            ListTag palettes = nbt.getList("palettes", Tag.TAG_LIST);
            if (!palettes.isEmpty() && palettes.get(0) instanceof ListTag firstPalette) {
                paletteList = firstPalette;
            }
        }

        if (paletteList.isEmpty()) {
            return TemplateMarkers.EMPTY;
        }

        List<BlockPos> pendingControlBoxes = new ArrayList<>();
        List<BlockPos> residentialControlBoxes = new ArrayList<>();
        List<BlockPos> otherControlBoxes = new ArrayList<>();
        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= paletteList.size()) {
                continue;
            }

            String blockName = paletteList.getCompound(stateIndex).getString("Name");
            BlockPos relativePos = readRelativeBlockPos(blockTag);
            if (relativePos == null) {
                continue;
            }

            switch (blockName) {
                case "simukraft:residential_control_box" -> {
                    pendingControlBoxes.add(relativePos);
                    residentialControlBoxes.add(relativePos);
                }
                case "simukraft:commercial_control_box", 
                     "simukraft:industrial_control_box",
                     "simukraft:nsuk_farmland_box" ->
                        pendingControlBoxes.add(relativePos);
                case "simukraft:other_control_box" -> otherControlBoxes.add(relativePos);
                default -> {
                }
            }
        }

        return new TemplateMarkers(pendingControlBoxes, residentialControlBoxes, otherControlBoxes);
    }

    @Nullable
    private static BlockPos readRelativeBlockPos(CompoundTag blockTag) {
        ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
        if (posTag.size() != 3) {
            return null;
        }

        return new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
    }

    private static void registerBusinessBlocks(ServerLevel level,
                                               BlockPos placePos,
                                               PlacedTemplate template,
                                               String buildingName,
                                               String buildingFileName,
                                               UUID cityId) {
        List<BlockPos> pendingControlBoxes = offsetPositions(placePos, template.markers().pendingControlBoxes());
        if (!pendingControlBoxes.isEmpty()) {
            ConstructionBoxMapping.registerPendingBoxes(level, pendingControlBoxes, cityId, buildingName, buildingFileName);
        }
    }

    private static void finalizeBusinessBlocks(ServerLevel level,
                                               BlockPos placePos,
                                               PlacedTemplate template,
                                               String buildingName,
                                               UUID cityId) {
        for (BlockPos residentialPos : offsetPositions(placePos, template.markers().residentialControlBoxes())) {
            ResidentialControlBoxBlock.activatePendingResidence(level, residentialPos);
        }

        for (BlockPos otherPos : offsetPositions(placePos, template.markers().otherControlBoxes())) {
            if (level.getServer() != null) {
                ControlBoxDataManager.writeOtherControlBox(level.getServer(), otherPos, buildingName, null, cityId);
            }
        }
    }

    private static void spawnDebugResidents(ServerLevel level, UUID cityId, int residenceCount) {
        if (residenceCount <= 0) {
            return;
        }

        CityData cityData = CityData.get(level);
        int spawnCount = Math.min(residenceCount, DEBUG_RESIDENT_SPAWN_LIMIT);
        for (int i = 0; i < spawnCount; i++) {
            CustomEntity npc = new CustomEntity(ModEntities.CUSTOM_ENTITY.get(), level);
            npc.setPos(level.getSharedSpawnPos().getX() + 0.5, BASE_Y + 1, level.getSharedSpawnPos().getZ() + 0.5);
            npc.setCityId(cityId);
            npc.initializeName();
            cityData.addCitizenToCity(cityId, npc.getUUID(), level);
            level.addFreshEntity(npc);
        }
    }

    private static void clearPendingBusinessBlocks(ServerLevel level, BlockPos placePos, PlacedTemplate template) {
        for (BlockPos controlBoxPos : offsetPositions(placePos, template.markers().pendingControlBoxes())) {
            ConstructionBoxMapping.removePendingBox(level, controlBoxPos);
        }
    }

    private static List<BlockPos> offsetPositions(BlockPos origin, List<BlockPos> relativePositions) {
        if (relativePositions.isEmpty()) {
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>(relativePositions.size());
        for (BlockPos relativePos : relativePositions) {
            positions.add(origin.offset(relativePos));
        }
        return positions;
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        MutableBlockPos cursor = new MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(level, cursor.set(x, y, z), state);
                }
            }
        }
    }

    private static void setBlock(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, WORLD_EDIT_FLAGS);
    }

    private record PlacedTemplate(StructureTemplate template, Vec3i size, TemplateMarkers markers) {
    }

    private record PlannedBuilding(String category, BuildingDataManager.BuildingInfo info, PlacedTemplate template) {
    }

    private record TemplateMarkers(List<BlockPos> pendingControlBoxes,
                                   List<BlockPos> residentialControlBoxes,
                                   List<BlockPos> otherControlBoxes) {
        private static final TemplateMarkers EMPTY = new TemplateMarkers(List.of(), List.of(), List.of());
    }

    private record ExhibitionFootprint(int width, int depth, int residentialCount) {
    }

    private record DebugCityContext(UUID cityId, BlockPos cityCorePos) {
    }

    private static final class GenerationTask {
        private final BlockPos spawnCenter = getDebugSpawnCenter();
        private final BlockPos blockDisplayOrigin = spawnCenter.offset(-42, 1, -42);
        private final List<Block> modBlocks;
        private final List<PlannedBuilding> buildings;
        private final DebugCityContext debugCity;

        private GenerationTask(List<Block> modBlocks, List<PlannedBuilding> buildings, DebugCityContext debugCity) {
            this.modBlocks = modBlocks;
            this.buildings = buildings;
            this.debugCity = debugCity;
        }

        public static GenerationTask create(ServerLevel level, DebugCityContext debugCity) {
            List<Block> modBlocks = ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(block -> {
                        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                        return key != null && Simukraft.MOD_ID.equals(key.getNamespace());
                    })
                    .sorted(Comparator.comparing(block -> {
                        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                        return key == null ? "" : key.getPath();
                    }))
                    .toList();

            List<PlannedBuilding> buildings = new ArrayList<>();
            for (String category : BUILDING_CATEGORIES) {
                for (BuildingDataManager.BuildingInfo info : getSortedBuildings(category)) {
                    PlacedTemplate template = loadTemplate(level.registryAccess(), info, category);
                    if (template == null || template.size().getX() <= 0 || template.size().getZ() <= 0) {
                        continue;
                    }
                    buildings.add(new PlannedBuilding(category, info, template));
                }
            }

            return new GenerationTask(modBlocks, buildings, debugCity);
        }

        public void generateAll(ServerLevel level) {
            buildSpawnPlatform(level, spawnCenter);
            buildBlockDisplayFloor(level, blockDisplayOrigin, modBlocks.size());
            setBlock(level, debugCity.cityCorePos(), ModBlocks.CITY_CORE.get().defaultBlockState());

            int columns = Math.max(1, (int) Math.ceil(Math.sqrt(modBlocks.size())));
            for (int blockIndex = 0; blockIndex < modBlocks.size(); blockIndex++) {
                int column = blockIndex % columns;
                int row = blockIndex / columns;
                BlockPos displayBase = blockDisplayOrigin.offset(column * BLOCK_GRID_SPACING, 0, row * BLOCK_GRID_SPACING);
                placeBlockDisplay(level, displayBase, modBlocks.get(blockIndex));
            }

            BlockPos exhibitionOrigin = spawnCenter.offset(0, 1, BUILDING_START_OFFSET_Z);
            ExhibitionFootprint footprint = generateSouthExhibition(level, exhibitionOrigin, debugCity.cityId());
            spawnDebugResidents(level, debugCity.cityId(), footprint.residentialCount());
            Simukraft.LOGGER.info("[DebugWorldGenerator] 南侧建筑展区生成完成: 建筑数={}, 宽度={}, 深度={}",
                    buildings.size(), footprint.width(), footprint.depth());
            level.setDefaultSpawnPos(spawnCenter, 0.0F);
        }

        private ExhibitionFootprint generateSouthExhibition(ServerLevel level, BlockPos origin, UUID cityId) {
            int exhibitionWidth = 0;
            int exhibitionDepth = 0;
            int residentialCount = 0;
            int currentTotalDepth = 0;
            int rowCount = 0;
            for (int rowStart = 0; rowStart < buildings.size(); rowStart += MAX_BUILDINGS_PER_SEGMENT_ROW) {
                rowCount++;
                int rowEnd = Math.min(rowStart + MAX_BUILDINGS_PER_SEGMENT_ROW, buildings.size());
                int rowWidth = 0;
                for (int index = rowStart; index < rowEnd; index++) {
                    rowWidth += buildings.get(index).template().size().getX() + BUILDING_SPACING;
                }
                rowWidth = Math.max(rowWidth - BUILDING_SPACING, 0);

                int rowStartX = -rowWidth / 2;
                int rowXOffset = 0;
                int rowMaxDepth = 0;

                for (int index = rowStart; index < rowEnd; index++) {
                    PlannedBuilding plannedBuilding = buildings.get(index);
                    String category = plannedBuilding.category();
                    BuildingDataManager.BuildingInfo info = plannedBuilding.info();
                    PlacedTemplate template = plannedBuilding.template();

                    // 以出生点南侧中轴为基准，整行居中铺开，避免单边越铺越偏。
                    BlockPos placePos = origin.offset(rowStartX + rowXOffset, 0, currentTotalDepth);
                    prepareBuildingPad(level, placePos, template.size());

                    String buildingFileName = info.getFileName() == null ? "unknown" : info.getFileName().replace(".sk", "");
                    String buildingName = info.getName() == null || info.getName().isBlank() ? buildingFileName : info.getName();

                    registerBusinessBlocks(level, placePos, template, buildingName, buildingFileName, cityId);
                    boolean placed = template.template().placeInWorld(
                            level,
                            placePos,
                            placePos,
                            new StructurePlaceSettings(),
                            RandomSource.create(),
                            WORLD_EDIT_FLAGS
                    );

                    if (!placed) {
                        clearPendingBusinessBlocks(level, placePos, template);
                        Simukraft.LOGGER.warn("[DebugWorldGenerator] 建筑放置失败 category={}, file={}", category, info.getFileName());
                        continue;
                    }

                    finalizeBusinessBlocks(level, placePos, template, buildingName, cityId);
                    residentialCount += template.markers().residentialControlBoxes().size();

                    rowXOffset += template.size().getX() + BUILDING_SPACING;
                    rowMaxDepth = Math.max(rowMaxDepth, template.size().getZ());
                }

                exhibitionWidth = Math.max(exhibitionWidth, rowWidth);
                currentTotalDepth += rowMaxDepth + BUILDING_SPACING;
            }

            exhibitionDepth = Math.max(exhibitionDepth, Math.max(currentTotalDepth - BUILDING_SPACING, 0));
            Simukraft.LOGGER.info("[DebugWorldGenerator] 南侧建筑展区生成完成: 建筑数={}, 行数={}, 总深度={}",
                    buildings.size(), rowCount, exhibitionDepth);
            return new ExhibitionFootprint(exhibitionWidth, exhibitionDepth, residentialCount);
        }
    }

    private static List<BuildingDataManager.BuildingInfo> getSortedBuildings(String category) {
        List<BuildingDataManager.BuildingInfo> buildings = new ArrayList<>(BuildingDataManager.getBuildingsByCategory(category));
        buildings.sort(Comparator.comparing(BuildingDataManager.BuildingInfo::getFileName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return buildings;
    }
}
