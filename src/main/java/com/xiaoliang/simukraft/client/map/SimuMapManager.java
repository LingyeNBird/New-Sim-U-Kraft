package com.xiaoliang.simukraft.client.map;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simukraft 地图管理器。
 * 管理所有区域 ({@link SimuMapRegion}) 的创建、扫描、渲染和生命周期。
 * 参考 FTB Chunks 的 MapManager，但完全独立、零外部模组依赖。
 *
 * <p>生命周期:</p>
 * <ol>
 *   <li>{@link #init()} - 客户端加入世界时调用，自动从磁盘恢复当前存档+维度的历史数据</li>
 *   <li>{@link #tick()} - 每客户端 tick 调用，执行增量扫描和渲染；检测维度切换时自动保存/加载数据</li>
 *   <li>{@link #shutdown()} - 客户端离开世界时调用，将当前数据写入磁盘</li>
 * </ol>
 *
 * <p>持久化策略：按 {@code <存档标识>/<维度>} 分目录存储，每个 region 对应一个 .smr 文件，
 * 由 {@link SimuMapStorage} 统一负责读写。</p>
 */
public class SimuMapManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static SimuMapManager instance;

    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SimuMap-Render");
        t.setDaemon(true);
        return t;
    });

    private final Map<Long, SimuMapRegion> regions = new ConcurrentHashMap<>();
    private final Set<Long> renderingKeys = ConcurrentHashMap.newKeySet();

    private int scanRadius = 12;
    private int chunksPerTick = 4;
    private long tickCount = 0;

    private int scanCursorDX = 0;
    private int scanCursorDZ = 0;
    private int scanSpiralLeg = 1;
    private int scanSpiralStep = 0;
    private int scanSpiralDirection = 0;

    private boolean initialized = false;
    private int loadGeneration = 0;

    /**
     * 当前已初始化时所在的维度 key，用于检测维度/世界切换。
     */
    @Nullable
    private ResourceKey<Level> currentDimension = null;

    /**
     * 当前存档的唯一标识字符串，由 {@link SimuMapStorage#getCurrentWorldId()} 提供。
     * 在 {@link #init()} 时设置，{@link #shutdown()} 时清空。
     */
    @Nullable
    private String currentWorldId = null;

    /**
     * 活跃消费者计数器。
     * 大于 0 时表示有地图界面处于打开状态，{@link #tick()} 才执行扫描和渲染。
     */
    private int activeConsumers = 0;

    private SimuMapManager() {
    }

    /**
     * 注册一个活跃消费者（地图界面打开时调用）。
     */
    public void acquireConsumer() {
        activeConsumers++;
    }

    /**
     * 注销一个活跃消费者（地图界面关闭时调用）。
     */
    public void releaseConsumer() {
        activeConsumers = Math.max(0, activeConsumers - 1);
    }

    /**
     * 是否存在活跃消费者。
     */
    public boolean hasActiveConsumer() {
        return activeConsumers > 0;
    }

    /**
     * 获取单例。
     */
    public static SimuMapManager getInstance() {
        if (instance == null) {
            instance = new SimuMapManager();
        }
        return instance;
    }

    /**
     * 检查地图系统是否已初始化。
     */
    public static boolean isAvailable() {
        return instance != null && instance.initialized;
    }

    /**
     * 初始化地图系统，并从磁盘恢复当前存档+维度的历史扫描数据。
     */
    public void init() {
        if (initialized) return;
        initialized = true;

        SimuBlockColors.getInstance().init();
        resetScanCursor();

        Minecraft mc = Minecraft.getInstance();
        currentWorldId = SimuMapStorage.getCurrentWorldId();
        Level level = mc.level;
        if (level != null) {
            currentDimension = level.dimension();
        } else {
            currentDimension = null;
        }

        if (currentDimension != null) {
            queueRegionLoad(currentWorldId, currentDimension);
            LOGGER.info("Simukraft: Map system initialization queued for world={} dim={}.",
                    currentWorldId, SimuMapStorage.dimensionToDir(currentDimension));
        } else {
            LOGGER.info("Simukraft: Map system initialized (dimension not yet known).");
        }
    }

    /**
     * 关闭地图系统：将当前数据写入磁盘，然后释放所有内存资源。
     */
    public void shutdown() {
        if (!initialized) return;
        initialized = false;

        persistRegionsAsync(currentWorldId, currentDimension, List.copyOf(regions.values()), "shutdown");

        currentDimension = null;
        currentWorldId = null;
        loadGeneration++;

        regions.clear();
        renderingKeys.clear();
        renderExecutor.shutdownNow();

        instance = null;
        LOGGER.info("Simukraft: Map rendering system shut down.");
    }

    /**
     * 获取或创建指定坐标的区域。
     */
    public SimuMapRegion getOrCreateRegion(int regionX, int regionZ) {
        long key = regionKey(regionX, regionZ);
        return regions.computeIfAbsent(key, k -> new SimuMapRegion(regionX, regionZ));
    }

    /**
     * 获取区域（可能为 null）。
     */
    @Nullable
    public SimuMapRegion getRegion(int regionX, int regionZ) {
        return regions.get(regionKey(regionX, regionZ));
    }

    /**
     * 获取指定范围内的所有区域。
     */
    public Collection<SimuMapRegion> getAllRegions() {
        return regions.values();
    }

    /**
     * 每 tick 调用：执行增量区块扫描和脏区域渲染。
     */
    public void tick() {
        if (!initialized) return;
        if (activeConsumers <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        // 检测维度/世界切换，发现变化时清空旧地图数据
        ResourceKey<Level> dim = level.dimension();
        ResourceKey<Level> previousDimension = currentDimension;
        if (!dim.equals(previousDimension)) {
            if (currentWorldId != null && previousDimension != null) {
                persistRegionsAsync(currentWorldId, previousDimension, List.copyOf(regions.values()), "dimension_change");
                LOGGER.info("Simukraft: Dimension changed from {} to {}, queued async save for {} regions.",
                        SimuMapStorage.dimensionToDir(previousDimension), SimuMapStorage.dimensionToDir(dim), regions.size());
                regions.clear();
                renderingKeys.clear();
                resetScanCursor();
            } else if (previousDimension == null) {
                LOGGER.info("Simukraft: First dimension acquired: {}.", SimuMapStorage.dimensionToDir(dim));
            }
            currentDimension = dim;
            if (currentWorldId != null) {
                queueRegionLoad(currentWorldId, currentDimension);
            }
        }

        tickCount++;

        if (tickCount % 2 == 0) {
            incrementalScan();
        }

        if (tickCount % 10 == 0) {
            renderDirtyRegions();
        }

        if (tickCount % 600 == 0) {
            releaseStaleRegions(60_000L);
        }
    }

    /**
     * 强制扫描指定范围内的所有区块。
     */
    public void forceScanArea(int centerChunkX, int centerChunkZ, int radius) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;

                if (!level.hasChunk(cx, cz)) continue;

                int regionX = cx >> 5;
                int regionZ = cz >> 5;
                SimuMapRegion region = getOrCreateRegion(regionX, regionZ);

                try {
                    SimuChunkScanner.scanChunk(cx, cz, region);
                } catch (Exception e) {
                    LOGGER.debug("Simukraft: Force scan failed for ({}, {}): {}", cx, cz, e.getMessage());
                }
            }
        }
    }

    /**
     * 强制重新渲染所有已加载区域。
     */
    public void forceRenderAll() {
        for (SimuMapRegion region : regions.values()) {
            SimuMapRegionData data = region.getData();
            if (data != null) {
                data.markDirty();
            }
        }
        renderDirtyRegions();
    }

    private void incrementalScan() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        int playerCX = player.chunkPosition().x;
        int playerCZ = player.chunkPosition().z;

        int scanned = 0;
        int maxAttempts = chunksPerTick * 4;
        int attempts = 0;

        while (scanned < chunksPerTick && attempts < maxAttempts) {
            int cx = playerCX + scanCursorDX;
            int cz = playerCZ + scanCursorDZ;

            advanceSpiralCursor();
            attempts++;

            if (Math.abs(scanCursorDX) > scanRadius || Math.abs(scanCursorDZ) > scanRadius) {
                resetScanCursor();
                break;
            }

            if (!level.hasChunk(cx, cz)) continue;

            int regionX = cx >> 5;
            int regionZ = cz >> 5;
            SimuMapRegion region = getOrCreateRegion(regionX, regionZ);

            try {
                if (SimuChunkScanner.scanChunk(cx, cz, region)) {
                    scanned++;
                }
            } catch (Exception e) {
                LOGGER.debug("Simukraft: Incremental scan failed for ({}, {})", cx, cz);
            }
        }
    }

    private void advanceSpiralCursor() {
        switch (scanSpiralDirection) {
            case 0 -> scanCursorDX++;
            case 1 -> scanCursorDZ++;
            case 2 -> scanCursorDX--;
            case 3 -> scanCursorDZ--;
        }

        scanSpiralStep++;
        if (scanSpiralStep >= scanSpiralLeg) {
            scanSpiralStep = 0;
            scanSpiralDirection = (scanSpiralDirection + 1) % 4;
            if (scanSpiralDirection == 0 || scanSpiralDirection == 2) {
                scanSpiralLeg++;
            }
        }
    }

    private void resetScanCursor() {
        scanCursorDX = 0;
        scanCursorDZ = 0;
        scanSpiralLeg = 1;
        scanSpiralStep = 0;
        scanSpiralDirection = 0;
    }

    private void renderDirtyRegions() {
        for (Map.Entry<Long, SimuMapRegion> entry : regions.entrySet()) {
            long key = entry.getKey();
            SimuMapRegion region = entry.getValue();
            SimuMapRegionData data = region.getData();
            if (data != null && data.isDirty() && renderingKeys.add(key)) {
                renderExecutor.execute(() -> {
                    try {
                        SimuMapRenderer.renderRegion(region);
                    } catch (Exception e) {
                        LOGGER.error("Simukraft: Failed to render region {}", region, e);
                    } finally {
                        renderingKeys.remove(key);
                    }
                });
            }
        }
    }

    private void releaseStaleRegions(long maxAge) {
        long now = System.currentTimeMillis();
        regions.entrySet().removeIf(entry -> {
            SimuMapRegion region = entry.getValue();
            if (now - region.getLastAccessTime() > maxAge) {
                if (region.distToPlayer() < 512 * 512 * 4) return false;
                region.release();
                return true;
            }
            return false;
        });
    }

    private void persistRegionsAsync(@Nullable String worldId, @Nullable ResourceKey<Level> dimension,
                                     List<SimuMapRegion> regionSnapshot, String reason) {
        if (regionSnapshot.isEmpty()) {
            return;
        }

        for (SimuMapRegion region : regionSnapshot) {
            region.releaseTexture();
        }

        if (worldId != null && dimension != null) {
            SimuMapStorage.saveAllAsync(worldId, dimension, regionSnapshot, reason);
            return;
        }

        for (SimuMapRegion region : regionSnapshot) {
            region.discardData();
        }
    }

    private void queueRegionLoad(String worldId, ResourceKey<Level> dimension) {
        int currentLoadGeneration = ++loadGeneration;
        SimuMapStorage.loadAllAsync(worldId, dimension, loadedRegions -> {
            if (!initialized || currentLoadGeneration != loadGeneration) {
                return;
            }
            if (currentDimension == null || !currentDimension.equals(dimension)) {
                return;
            }
            regions.putAll(loadedRegions);
            LOGGER.info("Simukraft: Async-loaded {} regions for world={} dim={}.",
                    loadedRegions.size(), worldId, SimuMapStorage.dimensionToDir(dimension));
        });
    }

    /**
     * 设置扫描半径（区块数）。
     */
    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(radius, 32));
    }

    /**
     * 获取扫描半径。
     */
    public int getScanRadius() {
        return scanRadius;
    }

    private static long regionKey(int regionX, int regionZ) {
        return ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);
    }
}
