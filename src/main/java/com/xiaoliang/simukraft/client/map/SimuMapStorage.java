package com.xiaoliang.simukraft.client.map;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * 地图数据持久化存储管理器。
 *
 * <p>存储目录结构（位于 .minecraft/simukraft_mapdata/）：
 * <pre>
 * simukraft_mapdata/
 *   &lt;存档标识&gt;/
 *     &lt;维度命名空间&gt;_&lt;维度路径&gt;/
 *       &lt;regionX&gt;_&lt;regionZ&gt;.smr
 * </pre>
 *
 * <p>文件格式（.smr = Simukraft Map Region）：
 * <ul>
 *   <li>4 字节 magic：{@code 0x534D5200}（"SMR\0"）</li>
 *   <li>2 字节 version：{@code 1}</li>
 *   <li>512*512 个 short：height 数组</li>
 *   <li>512*512 个 int：color 数组</li>
 *   <li>512*512 个 short：flags 数组</li>
 * </ul>
 *
 * <p>存档标识规则：
 * <ul>
 *   <li>单人游戏：使用存档文件夹名称</li>
 *   <li>多人游戏：使用 {@code mp_<IP>_<port>}，特殊字符替换为下划线</li>
 * </ul>
 */
public class SimuMapStorage {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAGIC = 0x534D5200;
    private static final short VERSION = 1;

    /** 根存储目录，位于 MC 游戏目录下 */
    private static final String ROOT_DIR = "simukraft_mapdata";

    private SimuMapStorage() {
    }

    /**
     * 获取当前存档的标识字符串。
     *
     * @return 单人游戏返回存档文件夹名，多人游戏返回 {@code mp_<host>_<port>}，无法识别时返回 {@code unknown}
     */
    public static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        var singleplayerServer = mc.getSingleplayerServer();
        if (singleplayerServer != null) {
            String levelId = singleplayerServer.getWorldData().getLevelName();
            return sanitize(levelId);
        }
        var currentServer = mc.getCurrentServer();
        if (currentServer != null) {
            String host = currentServer.ip;
            return "mp_" + sanitize(host);
        }
        return "unknown";
    }

    /**
     * 将维度 key 转换为合法的目录名称。
     *
     * @param dimension 维度资源键
     * @return 形如 {@code minecraft_overworld} 的字符串
     */
    public static String dimensionToDir(ResourceKey<Level> dimension) {
        String ns = dimension.location().getNamespace();
        String path = dimension.location().getPath();
        return sanitize(ns + "_" + path);
    }

    /**
     * 获取指定存档和维度的区域文件所在目录路径。
     *
     * @param worldId   存档标识
     * @param dimension 维度资源键
     * @return 目录路径
     */
    public static Path getRegionDir(String worldId, ResourceKey<Level> dimension) {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve(ROOT_DIR).resolve(worldId).resolve(dimensionToDir(dimension));
    }

    /**
     * 获取单个区域文件的路径。
     *
     * @param worldId   存档标识
     * @param dimension 维度资源键
     * @param regionX   区域X坐标
     * @param regionZ   区域Z坐标
     * @return 文件路径
     */
    public static Path getRegionFile(String worldId, ResourceKey<Level> dimension, int regionX, int regionZ) {
        return getRegionDir(worldId, dimension).resolve(regionX + "_" + regionZ + ".smr");
    }

    /**
     * 将单个区域的数据写入磁盘。
     * 仅在有实际数据时写入；若区域数据为空则跳过。
     *
     * @param worldId   存档标识
     * @param dimension 维度资源键
     * @param region    待保存的区域
     */
    public static void saveRegion(String worldId, ResourceKey<Level> dimension, SimuMapRegion region) {
        SimuMapRegionData data = region.getData();
        if (data == null || data.isEmpty()) return;

        Path file = getRegionFile(worldId, dimension, region.regionX, region.regionZ);
        try {
            Files.createDirectories(file.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
                out.writeInt(MAGIC);
                out.writeShort(VERSION);
                for (short h : data.height) {
                    out.writeShort(h);
                }
                for (int c : data.color) {
                    out.writeInt(c);
                }
                for (short f : data.flags) {
                    out.writeShort(f);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Simukraft: Failed to save map region ({}, {}) for world={} dim={}",
                    region.regionX, region.regionZ, worldId, dimensionToDir(dimension), e);
        }
    }

    /**
     * 将一批区域的数据批量写入磁盘。
     *
     * @param worldId   存档标识
     * @param dimension 维度资源键
     * @param regions   待保存的区域集合
     */
    public static void saveAll(String worldId, ResourceKey<Level> dimension,
                               Collection<SimuMapRegion> regions) {
        for (SimuMapRegion region : regions) {
            saveRegion(worldId, dimension, region);
        }
        LOGGER.debug("Simukraft: Saved {} regions for world={} dim={}",
                regions.size(), worldId, dimensionToDir(dimension));
    }

    /**
     * 从磁盘加载指定存档和维度下的所有已存区域数据，填充到传入的 regions Map 中。
     * 仅加载文件存在且格式合法的区域；格式错误的文件会被跳过并记录警告。
     *
     * @param worldId   存档标识
     * @param dimension 维度资源键
     * @param regions   目标 Map，key 为区域坐标编码，value 为 {@link SimuMapRegion}
     */
    public static void loadAll(String worldId, ResourceKey<Level> dimension,
                               Map<Long, SimuMapRegion> regions) {
        Path dir = getRegionDir(worldId, dimension);
        if (!Files.isDirectory(dir)) return;

        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".smr")).forEach(file -> {
                String name = file.getFileName().toString();
                name = name.substring(0, name.length() - 4);
                String[] parts = name.split("_", 2);
                if (parts.length != 2) return;
                try {
                    int rx = Integer.parseInt(parts[0]);
                    int rz = Integer.parseInt(parts[1]);
                    SimuMapRegionData data = readRegionFile(file);
                    if (data != null) {
                        SimuMapRegion region = new SimuMapRegion(rx, rz);
                        region.setData(data);
                        data.markDirty();
                        long key = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
                        regions.put(key, region);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Simukraft: Skipping malformed region file: {}", file.getFileName());
                }
            });
        } catch (IOException e) {
            LOGGER.error("Simukraft: Failed to list region files for world={} dim={}",
                    worldId, dimensionToDir(dimension), e);
        }

        LOGGER.debug("Simukraft: Loaded {} regions from world={} dim={}",
                regions.size(), worldId, dimensionToDir(dimension));
    }

    /**
     * 从单个 .smr 文件读取区域数据。
     *
     * @param file 文件路径
     * @return 成功时返回填充好的 {@link SimuMapRegionData}，格式非法时返回 {@code null}
     */
    private static SimuMapRegionData readRegionFile(Path file) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                LOGGER.warn("Simukraft: Invalid magic in {}", file.getFileName());
                return null;
            }
            short version = in.readShort();
            if (version != VERSION) {
                LOGGER.warn("Simukraft: Unsupported version {} in {}", version, file.getFileName());
                return null;
            }
            String name = file.getFileName().toString();
            name = name.substring(0, name.length() - 4);
            String[] parts = name.split("_", 2);
            int rx = Integer.parseInt(parts[0]);
            int rz = Integer.parseInt(parts[1]);

            SimuMapRegionData data = new SimuMapRegionData(rx, rz);
            for (int i = 0; i < SimuMapRegionData.AREA; i++) {
                data.height[i] = in.readShort();
            }
            for (int i = 0; i < SimuMapRegionData.AREA; i++) {
                data.color[i] = in.readInt();
            }
            for (int i = 0; i < SimuMapRegionData.AREA; i++) {
                data.flags[i] = in.readShort();
            }
            return data;
        } catch (IOException e) {
            LOGGER.error("Simukraft: Failed to read region file {}", file.getFileName(), e);
            return null;
        }
    }

    /**
     * 将字符串中不合法的文件系统字符替换为下划线。
     *
     * @param s 原始字符串
     * @return 清理后的字符串
     */
    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
