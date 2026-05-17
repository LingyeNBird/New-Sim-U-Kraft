package com.xiaoliang.simukraft.building;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.client.preview.SchematicNBTLoader;
import com.xiaoliang.simukraft.config.ServerConfig;
import com.xiaoliang.simukraft.entity.CustomEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("null")
public class ConstructionTask {
    private static final long MATERIAL_SLEEP_TICKS = 60L;
    private static final int INIT_BLOCKS_PER_TICK = 1500;
    private static final long INIT_TIME_BUDGET_NANOS = 4_000_000L;
    private static final Property<Direction> FACING_PROPERTY = requireProperty(BlockStateProperties.FACING);
    private static final Property<Direction> HORIZONTAL_FACING_PROPERTY = requireProperty(BlockStateProperties.HORIZONTAL_FACING);
    private static final Property<Direction> HOPPER_FACING_PROPERTY = requireProperty(BlockStateProperties.FACING_HOPPER);
    private static final Property<Integer> ROTATION_16_PROPERTY = requireProperty(BlockStateProperties.ROTATION_16);
    private static final Property<Direction.Axis> AXIS_PROPERTY = requireProperty(BlockStateProperties.AXIS);
    private static final Property<Direction.Axis> HORIZONTAL_AXIS_PROPERTY = requireProperty(BlockStateProperties.HORIZONTAL_AXIS);
    private static final Property<?> STAIRS_SHAPE_PROPERTY = requireProperty(BlockStateProperties.STAIRS_SHAPE);
    private static final Property<BedPart> BED_PART_PROPERTY = requireProperty(BedBlock.PART);
    private static final Property<Direction> BED_FACING_PROPERTY = requireProperty(BedBlock.FACING);
    private static final Property<DoubleBlockHalf> DOOR_HALF_PROPERTY = requireProperty(DoorBlock.HALF);

    @Nullable
    private CustomEntity builder;
    @Nonnull
    private final String buildingName;
    @Nonnull
    private final String category;
    @Nonnull
    private final BlockPos startPos;
    @Nonnull
    private final BlockPos buildBoxPos;
    @Nonnull
    private final Direction facing;
    @Nonnull
    private final String displayName;
    private final double cost;
    private int currentBlockIndex = 0;
    private boolean isCompleted = false;
    @Nonnull
    private final List<BlockInfo> blocksToPlace = new ArrayList<>();
    @Nonnull
    private final Map<BlockPos, Integer> blockIndexLookup = new HashMap<>();
    @Nonnull
    private final List<BlockPos> controlBoxPositions = new ArrayList<>();
    @Nonnull
    private final List<LayerRange> layerRanges = new ArrayList<>();
    private int currentLayerRangeIndex = 0;
    @Nullable
    private ServerLevel runtimeLevel = null;
    @Nonnull
    private Set<ChunkPos> requiredWorkflowChunks = Set.of();
    @Nonnull
    private final Set<ChunkPos> workflowForcedChunks = new LinkedHashSet<>();
    // 修复：添加区块加载等待计数器，解决退出重进后箱子找不到的问题
    private int chunkLoadWaitTicks = 0;
    private boolean hasNotifiedChunkLoading = false;
    @Nonnull
    private final BuilderMaterialCache materialCache;
    private boolean waitingForMaterials = false;
    private long nextMaterialCheckTick = Long.MIN_VALUE;
    @Nonnull
    private InitializationState initializationState = InitializationState.NOT_STARTED;
    @Nullable
    private CompletableFuture<CompoundTag> pendingNbtFuture;
    @Nullable
    private ListTag pendingBlocksList;
    @Nullable
    private ListTag pendingPalette;
    @Nullable
    private NavigableMap<Integer, List<List<BlockInfo>>> pendingLayerBuckets;
    @Nullable
    private Set<ChunkPos> pendingRequiredWorkflowChunks;
    @Nullable
    private List<BlockPos> pendingControlBoxPositions;
    @Nullable
    private Map<String, Integer> pendingMaterialRequirements;
    private int pendingBlockCursor = 0;
    private int pendingCurrentBlockIndex = 0;
    private int estimatedTotalBlocks = 0;
    @Nonnull
    private final Map<String, Integer> materialRequirements = new LinkedHashMap<>();
    private boolean controlBoxesRegistered = false;
    private boolean cityTerritoryValidated = false;

    public ConstructionTask(@Nonnull CustomEntity builder, @Nonnull String buildingName, @Nonnull String category, @Nonnull BlockPos startPos,
                           @Nonnull BlockPos buildBoxPos, @Nonnull Direction facing, @Nonnull String displayName, double cost) {
        this.builder = builder;
        this.runtimeLevel = builder.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        this.buildingName = Objects.requireNonNull(buildingName);
        this.category = Objects.requireNonNull(category);
        this.startPos = Objects.requireNonNull(startPos);
        this.buildBoxPos = Objects.requireNonNull(buildBoxPos);
        this.facing = Objects.requireNonNull(facing);
        this.displayName = Objects.requireNonNull(displayName);
        this.cost = cost;
        this.materialCache = new BuilderMaterialCache(this.buildBoxPos);
        initializeBoundsOnly();
    }

    /**
     * 用于从持久化数据恢复建造任务的构造函数
     * 不关联具体的NPC实体，用于局域网开放模式下NPC休息后恢复任务
     */
    public ConstructionTask(@Nonnull String buildingName, @Nonnull String category, @Nonnull BlockPos startPos,
                           @Nonnull BlockPos buildBoxPos, @Nonnull String facingStr, @Nonnull String displayName, double cost,
                           @Nullable ServerLevel level) {
        this.builder = null;  // 恢复时不关联NPC
        this.runtimeLevel = level;
        this.buildingName = Objects.requireNonNull(buildingName);
        this.category = Objects.requireNonNull(category);
        this.startPos = Objects.requireNonNull(startPos);
        this.buildBoxPos = Objects.requireNonNull(buildBoxPos);
        Direction tempFacing = Direction.byName(facingStr);
        this.facing = tempFacing != null ? tempFacing : Direction.NORTH;
        this.displayName = Objects.requireNonNull(displayName);
        this.cost = cost;
        this.materialCache = new BuilderMaterialCache(this.buildBoxPos);
        initializeBoundsOnly();
    }

    private void initializeBoundsOnly() {
        this.estimatedTotalBlocks = Integer.MAX_VALUE;
        this.requiredWorkflowChunks = Set.copyOf(baseWorkflowChunks());
    }

    @Nonnull
    private String getSchematicFilePath() {
        return "simukraftbuilding/" + category + "/" + buildingName + ".nbt";
    }

    @Nonnull
    private List<BlockInfo> loadAndParseBlocks() {
        List<BlockInfo> blocks = new ArrayList<>();

        // 使用新的NBT加载方式
        String filePath = getSchematicFilePath();
        List<SchematicNBTLoader.SchematicBlock> schematicBlocks = SchematicNBTLoader.loadSchematicBlocks(filePath);

        for (SchematicNBTLoader.SchematicBlock schematicBlock : schematicBlocks) {
            BlockPos pos = Objects.requireNonNull(schematicBlock.pos());
            BlockState state = Objects.requireNonNull(schematicBlock.blockState());

            // 根据朝向旋转坐标
            BlockPos rotatedPos = rotatePosition(pos);
            // 根据朝向旋转方块状态
            BlockState rotatedState = Objects.requireNonNull(rotateBlockState(state));

            // 计算最终位置
            BlockPos finalPos = new BlockPos(
                startPos.getX() + rotatedPos.getX(),
                startPos.getY() + rotatedPos.getY(),
                startPos.getZ() + rotatedPos.getZ()
            );

            blocks.add(new BlockInfo(finalPos, rotatedState, pos));
        }

        // 按层排序，每层优先级：普通方块 > 需要支撑/重力方块 > 液体相关方块
        blocks.sort(new LayeredBlockComparator());

        return blocks;
    }

    /**
     * 层建造比较器
     * 按Y坐标分层，每层内按优先级排序：完整方块 > 不完整方块 > 需要支撑的方块 > 重力方块 > 液体相关方块
     */
    private static class LayeredBlockComparator implements Comparator<BlockInfo> {
        @Override
        public int compare(BlockInfo a, BlockInfo b) {
            // 首先按Y坐标排序（从低到高，先建底层）
            int yCompare = Integer.compare(a.pos().getY(), b.pos().getY());
            if (yCompare != 0) {
                return yCompare;
            }

            // 同一层内按优先级排序
            int priorityA = getBlockPriority(a.state());
            int priorityB = getBlockPriority(b.state());
            return Integer.compare(priorityA, priorityB);
        }

        /**
         * 获取方块优先级
         * 0 = 完整方块（最先建造）
         * 1 = 不完整方块（台阶、楼梯等）
         * 2 = 需要支撑的方块（门、活板门、按钮、拉杆、火把、灯笼等）
         * 3 = 重力方块（沙子、沙砾等，最后建造）
         * 4 = 液体相关方块（统一最后放置，避免流体更新影响前序建造）
         * 5 = 空气方块（拆除任务放到层尾，避免层切换时先扫大量空气）
         */
        private static int getBlockPriority(BlockState state) {
            Block block = state.getBlock();

            if (state.isAir()) {
                return 5;
            }

            if (!state.getFluidState().isEmpty()) {
                return 4;
            }

            // 重力方块优先级最低（最后建造）
            if (block instanceof FallingBlock) {
                return 3;
            }

            // 需要支撑的方块
            if (requiresSupport(block)) {
                return 2;
            }

            // 不完整方块
            if (isIncompleteBlock(block)) {
                return 1;
            }

            // 完整方块优先级最高（最先建造）
            return 0;
        }

        /**
         * 检查方块是否需要支撑（会掉落或需要依附在其他方块上）
         */
        private static boolean requiresSupport(Block block) {
            return block instanceof DoorBlock ||
                   block instanceof TrapDoorBlock ||
                   block instanceof ButtonBlock ||
                   block instanceof LeverBlock ||
                   block instanceof TorchBlock ||
                   block instanceof LanternBlock ||
                   block instanceof ChainBlock ||
                   block instanceof IronBarsBlock;
        }

        /**
         * 检查方块是否为不完整方块
         */
        private static boolean isIncompleteBlock(Block block) {
            return block instanceof SlabBlock ||
                   block instanceof StairBlock ||
                   block instanceof FenceBlock ||
                   block instanceof WallBlock;
        }
    }
    
    private BlockPos rotatePosition(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        // 根据朝向进行旋转
        switch (facing) {
            case NORTH:
                // 默认朝向，不旋转
                break;
            case EAST:
                // 顺时针90度
                int newX1 = -z;
                int newZ1 = x;
                x = newX1;
                z = newZ1;
                break;
            case SOUTH:
                // 180度
                x = -x;
                z = -z;
                break;
            case WEST:
                // 逆时针90度（顺时针270度）
                int newX2 = z;
                int newZ2 = -x;
                x = newX2;
                z = newZ2;
                break;
            default:
                break;
        }
        
        return new BlockPos(x, y, z);
    }
    
    private BlockState rotateBlockState(BlockState state) {
        if (state.isAir()) {
            return state;
        }

        int rotations = 0;
        switch (facing) {
            case NORTH:
                rotations = 0;
                break;
            case EAST:
                rotations = 1;
                break;
            case SOUTH:
                rotations = 2;
                break;
            case WEST:
                rotations = 3;
                break;
            default:
                rotations = 0;
                break;
        }

        if (rotations == 0) {
            return state;
        }

        BlockState rotatedState = state;
        
        for (int i = 0; i < rotations; i++) {
            rotatedState = rotateBlockStateOnce(rotatedState);
        }
        
        return rotatedState;
    }

    @Nonnull
    private static <T extends Comparable<T>> T getRequiredValue(@Nonnull BlockState state, @Nonnull Property<T> property) {
        return Objects.requireNonNull(state.getValue(Objects.requireNonNull(property)));
    }

    @Nonnull
    private static <T extends Comparable<T>> BlockState setRequiredValue(@Nonnull BlockState state, @Nonnull Property<T> property, @Nonnull T value) {
        return Objects.requireNonNull(state.setValue(Objects.requireNonNull(property), Objects.requireNonNull(value)));
    }

    @Nonnull
    private static <T extends Comparable<T>> Property<T> requireProperty(Property<T> property) {
        return Objects.requireNonNull(property);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private static BlockState setDirectionEnumValue(@Nonnull BlockState state, @Nonnull Property<?> property, @Nonnull Direction value) {
        return setRequiredValue(state, (Property<Direction>) property, value);
    }

    @Nonnull
    @SuppressWarnings("deprecation")
    private BlockState rotateBlockStateOnce(BlockState state) {
        if (state.hasProperty(FACING_PROPERTY)) {
            Direction facing = getRequiredValue(state, FACING_PROPERTY);
            Direction newFacing = rotateDirection(facing);
            return setRequiredValue(state, FACING_PROPERTY, newFacing);
        }

        if (state.hasProperty(HORIZONTAL_FACING_PROPERTY)) {
            Direction facing = getRequiredValue(state, HORIZONTAL_FACING_PROPERTY);
            Direction newFacing = rotateHorizontalDirection(facing);
            return setRequiredValue(state, HORIZONTAL_FACING_PROPERTY, newFacing);
        }

        if (state.hasProperty(HOPPER_FACING_PROPERTY)) {
            Direction facing = getRequiredValue(state, HOPPER_FACING_PROPERTY);
            Direction newFacing = rotateDirection(facing);
            return setRequiredValue(state, HOPPER_FACING_PROPERTY, newFacing);
        }

        if (state.hasProperty(ROTATION_16_PROPERTY)) {
            int rotation = getRequiredValue(state, ROTATION_16_PROPERTY);
            int newRotation = (rotation + 4) % 16;
            return setRequiredValue(state, ROTATION_16_PROPERTY, newRotation);
        }

        if (state.hasProperty(AXIS_PROPERTY)) {
            net.minecraft.core.Direction.Axis axis = getRequiredValue(state, AXIS_PROPERTY);
            net.minecraft.core.Direction.Axis newAxis = rotateAxis(axis);
            return setRequiredValue(state, AXIS_PROPERTY, newAxis);
        }

        if (state.hasProperty(HORIZONTAL_AXIS_PROPERTY)) {
            net.minecraft.core.Direction.Axis axis = getRequiredValue(state, HORIZONTAL_AXIS_PROPERTY);
            net.minecraft.core.Direction.Axis newAxis = rotateHorizontalAxis(axis);
            return setRequiredValue(state, HORIZONTAL_AXIS_PROPERTY, newAxis);
        }

        if (state.hasProperty(STAIRS_SHAPE_PROPERTY)) {
            return state.rotate(Rotation.CLOCKWISE_90);
        }

        for (Property<?> property : state.getProperties()) {
            if (property instanceof EnumProperty<?>) {
                Object value = state.getValue(property);
                if (value instanceof Direction direction) {
                    Direction newDirection = rotateDirection(direction);
                    return setDirectionEnumValue(state, property, newDirection);
                }
            }
        }
        
        return state.rotate(Rotation.CLOCKWISE_90);
    }
    
    @Nonnull
    private Direction rotateDirection(@Nonnull Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }
    
    @Nonnull
    private Direction rotateHorizontalDirection(@Nonnull Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> direction;
        };
    }
    
    @Nonnull
    private net.minecraft.core.Direction.Axis rotateAxis(@Nonnull net.minecraft.core.Direction.Axis axis) {
        return switch (axis) {
            case X -> net.minecraft.core.Direction.Axis.Z;
            case Z -> net.minecraft.core.Direction.Axis.X;
            case Y -> net.minecraft.core.Direction.Axis.Y;
        };
    }
    
    @Nonnull
    private net.minecraft.core.Direction.Axis rotateHorizontalAxis(@Nonnull net.minecraft.core.Direction.Axis axis) {
        return switch (axis) {
            case X -> net.minecraft.core.Direction.Axis.Z;
            case Z -> net.minecraft.core.Direction.Axis.X;
            case Y -> net.minecraft.core.Direction.Axis.Y;
        };
    }

    public boolean hasNextBlock() {
        if (isInitialized()) {
            return currentBlockIndex < blocksToPlace.size();
        }
        return !isInitializationFailed();
    }

    public boolean isInitialized() {
        return initializationState == InitializationState.READY;
    }

    public boolean isInitializing() {
        return initializationState == InitializationState.LOADING_NBT || initializationState == InitializationState.PARSING_BLOCKS;
    }

    public boolean isInitializationFailed() {
        return initializationState == InitializationState.FAILED;
    }

    public boolean tickInitialization(@Nullable ServerLevel serverLevel) {
        if (isInitialized()) {
            if (serverLevel != null) {
                ensureCurrentWorkflowChunksForced(serverLevel);
            }
            return true;
        }

        if (isInitializationFailed()) {
            return false;
        }

        if (serverLevel != null) {
            this.runtimeLevel = serverLevel;
        }

        try {
            if (initializationState == InitializationState.NOT_STARTED) {
                beginInitialization();
            }
            if (initializationState == InitializationState.LOADING_NBT) {
                finishAsyncNbtLoadIfReady();
            }
            if (initializationState == InitializationState.PARSING_BLOCKS) {
                parseInitializationBatch();
            }
            if (isInitialized() && serverLevel != null) {
                ensureCurrentWorkflowChunksForced(serverLevel);
            }
        } catch (Exception e) {
            initializationState = InitializationState.FAILED;
            pendingNbtFuture = null;
            pendingBlocksList = null;
            pendingPalette = null;
            pendingLayerBuckets = null;
            pendingRequiredWorkflowChunks = null;
            pendingControlBoxPositions = null;
            pendingMaterialRequirements = null;
            Simukraft.LOGGER.error("[ConstructionTask] Failed to initialize construction task {}/{}", category, buildingName, e);
        }

        return isInitialized();
    }

    private void beginInitialization() {
        initializationState = InitializationState.LOADING_NBT;
        String filePath = getSchematicFilePath();
        pendingNbtFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return SchematicNBTLoader.loadSchematicNBT(filePath);
            } catch (java.io.IOException e) {
                throw new CompletionException(e);
            }
        });
        Simukraft.LOGGER.info("[ConstructionTask] Started async NBT load for {}", displayName);
    }

    private void finishAsyncNbtLoadIfReady() {
        if (pendingNbtFuture == null || !pendingNbtFuture.isDone()) {
            return;
        }

        CompletableFuture<CompoundTag> completedFuture = pendingNbtFuture;
        pendingNbtFuture = null;
        CompoundTag nbt = completedFuture.join();
        CompoundTag root = nbt.contains("Schematic", 10) ? nbt.getCompound("Schematic") : nbt;
        if (!root.contains("blocks", 9) || !root.contains("palette", 9)) {
            throw new IllegalStateException("Invalid schematic format");
        }

        pendingBlocksList = root.getList("blocks", 10);
        pendingPalette = root.getList("palette", 10);
        estimatedTotalBlocks = pendingBlocksList.size();
        SchematicNBTLoader.SchematicSize size = SchematicNBTLoader.readSchematicSize(root);
        Set<ChunkPos> initialChunks = baseWorkflowChunks();
        if (size != null) {
            addBoundingBoxChunks(initialChunks, size);
        }
        requiredWorkflowChunks = Set.copyOf(initialChunks);
        pendingLayerBuckets = new TreeMap<>();
        pendingRequiredWorkflowChunks = baseWorkflowChunks();
        pendingControlBoxPositions = new ArrayList<>();
        pendingMaterialRequirements = new LinkedHashMap<>();
        pendingBlockCursor = 0;
        initializationState = InitializationState.PARSING_BLOCKS;
        Simukraft.LOGGER.info("[ConstructionTask] Started staged initialization for {} ({} blocks)", displayName, estimatedTotalBlocks);
    }

    private void parseInitializationBatch() {
        if (pendingBlocksList == null || pendingPalette == null || pendingLayerBuckets == null
                || pendingRequiredWorkflowChunks == null || pendingControlBoxPositions == null || pendingMaterialRequirements == null) {
            initializationState = InitializationState.FAILED;
            return;
        }

        long deadline = System.nanoTime() + INIT_TIME_BUDGET_NANOS;
        int processed = 0;
        while (pendingBlockCursor < pendingBlocksList.size()
                && processed < INIT_BLOCKS_PER_TICK
                && System.nanoTime() < deadline) {
            CompoundTag blockTag = pendingBlocksList.getCompound(pendingBlockCursor++);
            SchematicNBTLoader.SchematicBlock schematicBlock = SchematicNBTLoader.parseSchematicBlock(blockTag, pendingPalette);
            if (schematicBlock != null) {
                BlockInfo blockInfo = toBlockInfo(schematicBlock);
                addToLayerBuckets(blockInfo, pendingLayerBuckets);
                collectParsedBlockMetadata(blockInfo, pendingRequiredWorkflowChunks, pendingControlBoxPositions, pendingMaterialRequirements);
            }
            processed++;
        }

        if (pendingBlockCursor >= pendingBlocksList.size()) {
            finishInitialization(pendingLayerBuckets, pendingRequiredWorkflowChunks, pendingControlBoxPositions, pendingMaterialRequirements);
        }
    }

    @Nonnull
    private BlockInfo toBlockInfo(@Nonnull SchematicNBTLoader.SchematicBlock schematicBlock) {
        BlockPos pos = Objects.requireNonNull(schematicBlock.pos());
        BlockState state = Objects.requireNonNull(schematicBlock.blockState());
        BlockPos rotatedPos = rotatePosition(pos);
        BlockState rotatedState = Objects.requireNonNull(rotateBlockState(state));
        BlockPos finalPos = new BlockPos(
            startPos.getX() + rotatedPos.getX(),
            startPos.getY() + rotatedPos.getY(),
            startPos.getZ() + rotatedPos.getZ()
        );
        return new BlockInfo(finalPos, rotatedState, pos);
    }

    private void addToLayerBuckets(@Nonnull BlockInfo blockInfo, @Nonnull NavigableMap<Integer, List<List<BlockInfo>>> layerBuckets) {
        List<List<BlockInfo>> priorityBuckets = layerBuckets.computeIfAbsent(blockInfo.pos().getY(), y -> createPriorityBuckets());
        priorityBuckets.get(LayeredBlockComparator.getBlockPriority(blockInfo.state())).add(blockInfo);
    }

    private void collectParsedBlockMetadata(@Nonnull BlockInfo blockInfo, @Nonnull Set<ChunkPos> chunks,
                                            @Nonnull List<BlockPos> controlBoxes, @Nonnull Map<String, Integer> materials) {
        chunks.add(new ChunkPos(blockInfo.pos()));
        if (isControlBoxBlock(blockInfo.state())) {
            controlBoxes.add(blockInfo.pos());
        }

        BlockState state = blockInfo.state();
        if (!state.isAir() && com.xiaoliang.simukraft.utils.MaterialManager.requiresMaterial(state)) {
            String blockId = com.xiaoliang.simukraft.utils.MaterialManager.getBlockId(state.getBlock());
            materials.merge(blockId, 1, Integer::sum);
        }
    }

    @Nonnull
    private static List<List<BlockInfo>> createPriorityBuckets() {
        List<List<BlockInfo>> buckets = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            buckets.add(new ArrayList<>());
        }
        return buckets;
    }

    private void finishInitialization(@Nonnull NavigableMap<Integer, List<List<BlockInfo>>> layerBuckets,
                                      @Nonnull Set<ChunkPos> chunks,
                                      @Nonnull List<BlockPos> controlBoxes,
                                      @Nonnull Map<String, Integer> materials) {
        blocksToPlace.clear();
        for (List<List<BlockInfo>> priorityBuckets : layerBuckets.values()) {
            for (List<BlockInfo> bucket : priorityBuckets) {
                blocksToPlace.addAll(bucket);
            }
        }

        blockIndexLookup.clear();
        blockIndexLookup.putAll(buildBlockIndexLookup(blocksToPlace));
        controlBoxPositions.clear();
        controlBoxPositions.addAll(controlBoxes);
        layerRanges.clear();
        layerRanges.addAll(buildLayerRanges(blocksToPlace));
        requiredWorkflowChunks = Set.copyOf(chunks);
        materialRequirements.clear();
        materialRequirements.putAll(materials);

        pendingBlocksList = null;
        pendingPalette = null;
        pendingLayerBuckets = null;
        pendingRequiredWorkflowChunks = null;
        pendingControlBoxPositions = null;
        pendingMaterialRequirements = null;
        initializationState = InitializationState.READY;
        setCurrentBlockIndex(pendingCurrentBlockIndex);
        Simukraft.LOGGER.info("[ConstructionTask] Finished staged initialization for {} ({} blocks)", displayName, blocksToPlace.size());
    }

    /**
     * 恢复持久化任务后重新绑定运行时建筑师。
     */
    public void attachBuilder(@Nullable CustomEntity builder) {
        this.builder = builder;
        if (builder != null && builder.level() instanceof ServerLevel serverLevel) {
            this.runtimeLevel = serverLevel;
            ensureCurrentWorkflowChunksForced(serverLevel);
        }
    }

    public void detachBuilder() {
        releaseForcedChunks();
        this.builder = null;
    }

    @Nullable
    private ServerLevel getRuntimeLevel() {
        if (builder != null && builder.level() instanceof ServerLevel serverLevel) {
            runtimeLevel = serverLevel;
            return serverLevel;
        }
        return runtimeLevel;
    }

    @Nonnull
    private Set<ChunkPos> baseWorkflowChunks() {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(new ChunkPos(startPos));
        chunks.add(new ChunkPos(buildBoxPos));
        for (Direction direction : Direction.values()) {
            chunks.add(new ChunkPos(buildBoxPos.relative(direction)));
        }
        return chunks;
    }

    private void addBoundingBoxChunks(@Nonnull Set<ChunkPos> chunks, @Nonnull SchematicNBTLoader.SchematicSize size) {
        BlockPos[] corners = new BlockPos[] {
            new BlockPos(0, 0, 0),
            new BlockPos(size.x() - 1, 0, 0),
            new BlockPos(0, 0, size.z() - 1),
            new BlockPos(size.x() - 1, 0, size.z() - 1)
        };
        for (BlockPos corner : corners) {
            chunks.add(new ChunkPos(startPos.offset(rotatePosition(corner))));
        }
    }

    private void ensureCurrentWorkflowChunksForced(@Nonnull ServerLevel serverLevel) {
        ensureWorkflowChunkForced(serverLevel, new ChunkPos(buildBoxPos));
        ensureWorkflowChunkForced(serverLevel, new ChunkPos(startPos));
        if (isInitialized() && currentBlockIndex >= 0 && currentBlockIndex < blocksToPlace.size()) {
            ensureWorkflowChunkForced(serverLevel, new ChunkPos(blocksToPlace.get(currentBlockIndex).pos()));
        }
    }

    private void ensureWorkflowChunkForced(@Nonnull ServerLevel serverLevel, @Nonnull ChunkPos chunkPos) {
        long chunkKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        boolean alreadyForcedByOtherOwner = serverLevel.getForcedChunks().contains(chunkKey) && !workflowForcedChunks.contains(chunkPos);
        if (!workflowForcedChunks.contains(chunkPos) && !serverLevel.getForcedChunks().contains(chunkKey)) {
            serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true);
            workflowForcedChunks.add(chunkPos);
        }
        if (workflowForcedChunks.contains(chunkPos) || alreadyForcedByOtherOwner || serverLevel.hasChunk(chunkPos.x, chunkPos.z)) {
            serverLevel.getChunk(chunkPos.x, chunkPos.z);
        }
    }

    private long lastWarningTime = 0;

    private boolean consumeFromNearbyChests(BlockState state) {
        ServerLevel serverLevel = getRuntimeLevel();
        if (serverLevel == null) return false;

        // 使用新的材料管理器检查是否需要材料
        if (!com.xiaoliang.simukraft.utils.MaterialManager.requiresMaterial(state)) {
            return true; // 非材料方块直接允许放置
        }

        // 性能优化：只在区块未就绪时检查，避免每 tick 重复检查
        if (!areChunksReady(serverLevel)) {
            return false; // 区块未就绪，等待下次tick
        }

        if (shouldDelayMaterialCheck(serverLevel)) {
            return false;
        }

        if (materialCache.tryConsume(serverLevel, state)) {
            clearMaterialSleep();
            return true;
        }

        enterMaterialSleep(serverLevel);

        // 没有找到材料，检查冷却时间后发送提示
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarningTime >= ServerConfig.getBuilderWarningCooldownMs()) {
            lastWarningTime = currentTime;
            // 使用材料管理器获取详细的材料需求信息（Component 版本，支持客户端翻译）
            Component materialInfo = com.xiaoliang.simukraft.utils.MaterialManager.getMaterialRequirementComponent(state);
            Component message = Component.translatable("message.simukraft.construction.need_materials", displayName, materialInfo, 1);
            // 使用原版消息系统广播给所有玩家
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);
        }
        return false;
    }

    private boolean shouldDelayMaterialCheck(@Nonnull ServerLevel serverLevel) {
        if (!waitingForMaterials) {
            return false;
        }

        if (serverLevel.getGameTime() < nextMaterialCheckTick) {
            return true;
        }

        clearMaterialSleep();
        materialCache.markDirty();
        return false;
    }

    private void enterMaterialSleep(@Nonnull ServerLevel serverLevel) {
        waitingForMaterials = true;
        long wakeTick = serverLevel.getGameTime() + MATERIAL_SLEEP_TICKS;
        if (nextMaterialCheckTick == Long.MIN_VALUE) {
            nextMaterialCheckTick = wakeTick;
            return;
        }
        nextMaterialCheckTick = Math.min(nextMaterialCheckTick, wakeTick);
    }

    private void clearMaterialSleep() {
        waitingForMaterials = false;
        nextMaterialCheckTick = Long.MIN_VALUE;
    }

    public void requestMaterialRefresh(long delayTicks) {
        ServerLevel serverLevel = getRuntimeLevel();
        materialCache.markDirty();
        if (serverLevel == null) {
            clearMaterialSleep();
            return;
        }

        long safeDelayTicks = Math.max(0L, delayTicks);
        if (safeDelayTicks == 0L) {
            clearMaterialSleep();
            return;
        }

        waitingForMaterials = true;
        long wakeTick = serverLevel.getGameTime() + safeDelayTicks;
        if (nextMaterialCheckTick == Long.MIN_VALUE) {
            nextMaterialCheckTick = wakeTick;
            return;
        }
        nextMaterialCheckTick = Math.min(nextMaterialCheckTick, wakeTick);
    }

    public boolean isWaitingForMaterials() {
        return waitingForMaterials;
    }

    public boolean handlesContainerInteraction(@Nonnull ServerLevel serverLevel, @Nonnull BlockPos containerPos) {
        return materialCache.tracksContainer(serverLevel, containerPos);
    }

    public BlockInfo getNextBlock() {
        return getNextBlock(null);
    }

    /**
     * 修复：在消耗材料之前检查方块是否已经存在
     * @param serverLevel 服务器世界，用于检查方块状态。如果为null，则跳过已存在方块检查
     */
    public BlockInfo getNextBlock(ServerLevel serverLevel) {
        syncCurrentLayerRangeIndex();
        while (hasNextBlock()) {
            LayerRange currentLayerRange = getCurrentLayerRange();
            if (currentLayerRange != null && currentBlockIndex > currentLayerRange.endIndex()) {
                currentLayerRangeIndex++;
                continue;
            }

            BlockInfo next = blocksToPlace.get(currentBlockIndex);

            if (serverLevel != null && shouldSkipWithoutPlacement(serverLevel, next)) {
                currentBlockIndex++;
                continue;
            }

            // menglannnn: 允许放置空气方块（用于拆除/替换已有方块）
            // 空气方块直接返回，不需要消耗材料
            if (next.state().isAir()) {
                currentBlockIndex++;
                return next;
            }

            // 检查是否需要消耗材料
            if (!ServerConfig.isBuilderRequireMaterials()) {
                // 不需要材料，直接返回
                currentBlockIndex++;
                return next;
            }

            // 处理双格方块（床和门）
            BlockState state = next.state();
            if (isDoubleBlock(state)) {
                // 获取双格方块的另一半位置
                BlockPos otherHalfPos = getOtherHalfPos(state, next.pos());
                if (otherHalfPos != null) {
                    // 查找另一半在列表中的索引
                    int otherHalfIndex = findBlockIndex(otherHalfPos, state);
                    if (otherHalfIndex != -1 && otherHalfIndex > currentBlockIndex) {
                        // 这是双格方块的第一个部分，消耗材料并跳过第二个部分
                        if (consumeFromNearbyChests(state)) {
                            currentBlockIndex++;
                            // 同时跳过另一半（不消耗额外材料）
                            if (otherHalfIndex == currentBlockIndex) {
                                currentBlockIndex++;
                            }
                            return next;
                        } else {
                            return null; // 材料不足，暂停建造
                        }
                    } else {
                        // 这是双格方块的第二个部分，已经消耗过材料了，直接跳过
                        currentBlockIndex++;
                        continue;
                    }
                }
            }

            // 普通方块的处理
            if (consumeFromNearbyChests(next.state())) {
                currentBlockIndex++;
                return next;
            } else {
                return null; // 材料不足，暂停建造
            }
        }
        return null;
    }

    private boolean shouldSkipWithoutPlacement(@Nonnull ServerLevel serverLevel, @Nonnull BlockInfo next) {
        BlockState targetState = next.state();
        BlockPos targetPos = next.pos();

        if (targetState.isAir()) {
            return serverLevel.getBlockState(targetPos).isAir();
        }

        BlockState currentState = serverLevel.getBlockState(targetPos);
        if (targetState.getBlock() == currentState.getBlock()) {
            return true;
        }

        if (!isDoubleBlock(targetState)) {
            return false;
        }

        BlockPos otherHalfPos = getOtherHalfPos(targetState, targetPos);
        if (otherHalfPos == null) {
            return false;
        }

        int otherHalfIndex = findBlockIndex(otherHalfPos, targetState);
        return otherHalfIndex != -1 && otherHalfIndex < currentBlockIndex;
    }

    /**
     * 检查是否是双格方块（床或门）
     */
    private boolean isDoubleBlock(BlockState state) {
        Block block = state.getBlock();
        // 检查是否是床
        if (block instanceof BedBlock) {
            return true;
        }
        // 检查是否是门
        if (block instanceof DoorBlock) {
            return true;
        }
        return false;
    }

    /**
     * 获取双格方块的另一半位置
     */
    @Nullable
    private BlockPos getOtherHalfPos(@Nonnull BlockState state, @Nonnull BlockPos pos) {
        Block block = state.getBlock();

        // 处理床
        if (block instanceof BedBlock) {
            if (state.hasProperty(BED_PART_PROPERTY)) {
                BedPart part = getRequiredValue(state, BED_PART_PROPERTY);
                // 根据朝向获取另一半位置
                if (state.hasProperty(BED_FACING_PROPERTY)) {
                    Direction facing = getRequiredValue(state, BED_FACING_PROPERTY);
                    if (part == BedPart.FOOT) {
                        // 脚在头部后面，所以头部在脚的前面
                        return pos.relative(facing);
                    } else {
                        // 头部在脚的前面，所以脚在头部的后面
                        return pos.relative(facing.getOpposite());
                    }
                }
            }
        }

        // 处理门
        if (block instanceof DoorBlock) {
            if (state.hasProperty(DOOR_HALF_PROPERTY)) {
                DoubleBlockHalf half = getRequiredValue(state, DOOR_HALF_PROPERTY);
                if (half == DoubleBlockHalf.LOWER) {
                    // 下半部分，上半部分在上面
                    return pos.above();
                } else {
                    // 上半部分，下半部分在下面
                    return pos.below();
                }
            }
        }

        return null;
    }

    /**
     * 在blocksToPlace列表中查找指定位置和状态的方块索引
     */
    private int findBlockIndex(BlockPos pos, BlockState state) {
        Integer index = blockIndexLookup.get(pos);
        if (index == null) {
            return -1;
        }
        BlockInfo info = blocksToPlace.get(index);
        return info.state().getBlock() == state.getBlock() ? index : -1;
    }

    @Nonnull
    private static Map<BlockPos, Integer> buildBlockIndexLookup(@Nonnull List<BlockInfo> blocks) {
        Map<BlockPos, Integer> indexLookup = new HashMap<>(Math.max(16, blocks.size()));
        for (int i = 0; i < blocks.size(); i++) {
            indexLookup.put(blocks.get(i).pos(), i);
        }
        return indexLookup;
    }

    @Nonnull
    private static List<LayerRange> buildLayerRanges(@Nonnull List<BlockInfo> blocks) {
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<LayerRange> ranges = new ArrayList<>();
        int layerStartIndex = 0;
        int currentY = blocks.get(0).pos().getY();
        for (int i = 1; i < blocks.size(); i++) {
            int y = blocks.get(i).pos().getY();
            if (y != currentY) {
                ranges.add(new LayerRange(currentY, layerStartIndex, i - 1));
                currentY = y;
                layerStartIndex = i;
            }
        }
        ranges.add(new LayerRange(currentY, layerStartIndex, blocks.size() - 1));
        return ranges;
    }

    private void syncCurrentLayerRangeIndex() {
        while (currentLayerRangeIndex < layerRanges.size()) {
            LayerRange currentRange = layerRanges.get(currentLayerRangeIndex);
            if (currentBlockIndex < currentRange.startIndex()) {
                return;
            }
            if (currentBlockIndex <= currentRange.endIndex()) {
                return;
            }
            currentLayerRangeIndex++;
        }
    }

    @Nullable
    private LayerRange getCurrentLayerRange() {
        syncCurrentLayerRangeIndex();
        if (currentLayerRangeIndex < 0 || currentLayerRangeIndex >= layerRanges.size()) {
            return null;
        }
        return layerRanges.get(currentLayerRangeIndex);
    }

    private static boolean isControlBoxBlock(@Nonnull BlockState state) {
        var blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) return false;
        String path = blockId.getPath();
        return path.contains("control_box") || path.contains("farmland_box");
    }

    public void markCompleted() {
        this.isCompleted = true;
        cancelPendingInitialization();
        // 修复：释放所有强制加载的区块
        releaseForcedChunks();

        // 注意：经验值现在在每放置一个方块时获得，不再在建造完成时获得
    }

    public void cancel() {
        this.isCompleted = true;
        cancelPendingInitialization();
        this.currentBlockIndex = blocksToPlace.size();
        // 修复：释放所有强制加载的区块
        releaseForcedChunks();

        if (this.builder != null) {
            // 只清除任务引用，不调用resetToIdle避免循环
            builder.setConstructionTask(null);
        }
    }

    /**
     * 性能优化：检查区块是否就绪，避免每 tick 重复检查
     * 只在区块未就绪时执行完整检查逻辑
     */
    private boolean areChunksReady(ServerLevel serverLevel) {
        ensureCurrentWorkflowChunksForced(serverLevel);

        // 如果已经等待过区块加载，直接返回状态
        if (chunkLoadWaitTicks > 0) {
            return checkAndWaitChunks(serverLevel);
        }

        ChunkPos currentChunk = getCurrentTargetChunk();
        if (currentChunk != null && !serverLevel.hasChunk(currentChunk.x, currentChunk.z)) {
            return checkAndWaitChunks(serverLevel);
        }
        return true; // 所有区块都已加载
    }

    private void cancelPendingInitialization() {
        if (pendingNbtFuture != null) {
            pendingNbtFuture.cancel(true);
            pendingNbtFuture = null;
        }
        pendingBlocksList = null;
        pendingPalette = null;
        pendingLayerBuckets = null;
        pendingRequiredWorkflowChunks = null;
        pendingControlBoxPositions = null;
        pendingMaterialRequirements = null;
    }
    
    /**
     * 检查并等待区块加载
     */
    private boolean checkAndWaitChunks(ServerLevel serverLevel) {
        int maxWaitTicks = ServerConfig.getBuilderChunkLoadWaitTicks();
        boolean allChunksLoaded = true;
        
        ChunkPos chunkPos = getCurrentTargetChunk();
        if (chunkPos != null && !serverLevel.hasChunk(chunkPos.x, chunkPos.z)) {
            allChunksLoaded = false;
            ensureWorkflowChunkForced(serverLevel, chunkPos);
        }

        if (!allChunksLoaded) {
            chunkLoadWaitTicks++;

            if (!hasNotifiedChunkLoading && chunkLoadWaitTicks == 1) {
                hasNotifiedChunkLoading = true;
                Component message = Component.translatable("message.simukraft.construction.chunk_loading", displayName)
                    .withStyle(style -> style.withColor(0xFFFF00));
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(Objects.requireNonNull(message), false);
            }

            if (chunkLoadWaitTicks >= maxWaitTicks) {
                chunkLoadWaitTicks = 0;
            } else {
                return false;
            }
        } else {
            chunkLoadWaitTicks = 0;
        }
        return true;
    }

    @Nullable
    private ChunkPos getCurrentTargetChunk() {
        if (!isInitialized()) {
            return new ChunkPos(buildBoxPos);
        }
        if (currentBlockIndex >= 0 && currentBlockIndex < blocksToPlace.size()) {
            return new ChunkPos(blocksToPlace.get(currentBlockIndex).pos());
        }
        return new ChunkPos(buildBoxPos);
    }

    /**
     * 修复：释放所有强制加载的区块
     */
    private void releaseForcedChunks() {
        ServerLevel serverLevel = getRuntimeLevel();
        if (serverLevel == null) {
            return;
        }
        for (ChunkPos chunkPos : workflowForcedChunks) {
            long chunkKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);
            if (serverLevel.getForcedChunks().contains(chunkKey)) {
                serverLevel.setChunkForced(chunkPos.x, chunkPos.z, false);
            }
        }
        workflowForcedChunks.clear();
    }

    public boolean isCompleted() {
        return isCompleted || (isInitialized() && currentBlockIndex >= blocksToPlace.size());
    }

    /**
     * 标记当前方块为已完成（用于跳过黑名单方块等情况）
     */
    public void markCurrentBlockComplete() {
        currentBlockIndex++;
    }

    public CustomEntity getBuilder() {
        return builder;
    }

    public String getBuildingName() {
        return displayName;
    }

    @Nonnull
    public List<BlockInfo> getBlocksToPlace() {
        return Collections.unmodifiableList(blocksToPlace);
    }
    
    public String getInternalBuildingName() {
        return buildingName;
    }
    
    public String getCategory() {
        return category;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public BlockPos getBuildBoxPos() {
        return buildBoxPos;
    }

    public Direction getFacing() {
        return facing;
    }

    public double getCost() {
        return cost;
    }

    public Set<ChunkPos> getRequiredWorkflowChunks() {
        return requiredWorkflowChunks;
    }

    public int getProgress() {
        int totalBlocks = getTotalBlocks();
        if (totalBlocks <= 0 || totalBlocks == Integer.MAX_VALUE) return 0;
        int progress = (int) ((double) currentBlockIndex / totalBlocks * 100);
        return Math.max(0, Math.min(100, progress));
    }

    public int getCurrentBlockIndex() {
        return currentBlockIndex;
    }

    public void setCurrentBlockIndex(int index) {
        if (!isInitialized()) {
            this.currentBlockIndex = Math.max(0, index);
            this.pendingCurrentBlockIndex = this.currentBlockIndex;
            return;
        }
        int totalBlocks = getTotalBlocks();
        this.currentBlockIndex = Math.max(0, Math.min(index, totalBlocks));
        this.currentLayerRangeIndex = 0;
        syncCurrentLayerRangeIndex();
    }

    /**
     * 获取总方块数量
     */
    public int getTotalBlocks() {
        return isInitialized() ? blocksToPlace.size() : estimatedTotalBlocks;
    }

    public String getTotalBlocksDisplay() {
        int totalBlocks = getTotalBlocks();
        return isInitialized() || totalBlocks != Integer.MAX_VALUE ? String.valueOf(totalBlocks) : "loading";
    }

    @Nonnull
    public List<BlockPos> getControlBoxPositions() {
        return controlBoxPositions;
    }

    public boolean hasRegisteredControlBoxes() {
        return controlBoxesRegistered;
    }

    public void markControlBoxesRegistered() {
        this.controlBoxesRegistered = true;
    }

    public boolean isCityTerritoryValidated() {
        return cityTerritoryValidated;
    }

    public void markCityTerritoryValidated() {
        this.cityTerritoryValidated = true;
    }

    /**
     * 获取显示名称（建筑的中文名称）
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取建造所需的所有材料清单
     * menglannnn: 用于清单物品显示材料需求
     * @return 材料ID到数量的映射
     */
    @Nonnull
    public Map<String, Integer> getRequiredMaterials() {
        return new LinkedHashMap<>(materialRequirements);
    }

    private enum InitializationState {
        NOT_STARTED,
        LOADING_NBT,
        PARSING_BLOCKS,
        READY,
        FAILED
    }

    private record LayerRange(int y, int startIndex, int endIndex) {
    }

    public record BlockInfo(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockPos originalNbtPos) {
        public BlockInfo(@Nonnull BlockPos pos, @Nonnull BlockState state) {
            this(pos, state, null);
        }
    }
}
