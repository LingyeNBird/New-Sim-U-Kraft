package com.xiaoliang.simukraft.world;

import com.xiaoliang.simukraft.employment.bridge.EmploymentLegacyBridge;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 物流仓库雇佣数据存储 - 使用V2统一雇佣存储系统
 */
public class LogisticsHiredData {
    private static final LogisticsHiredData INSTANCE = new LogisticsHiredData();

    private LogisticsHiredData() {}

    public static LogisticsHiredData getInstance() {
        return INSTANCE;
    }

    // ══════════════════════════════════════
    //  服务端盒子雇佣管理
    // ══════════════════════════════════════

    public static void setServerBoxHired(MinecraftServer server, BlockPos pos, UUID npcUuid) {
        Map<BlockPos, UUID> data = loadServerBoxHired(server);
        data.put(pos, npcUuid);
        saveServerBoxHired(server, data);
    }

    public static void removeServerBoxHired(MinecraftServer server, BlockPos pos) {
        Map<BlockPos, UUID> data = loadServerBoxHired(server);
        data.remove(pos);
        saveServerBoxHired(server, data);
    }

    public static boolean hasServerBoxHired(MinecraftServer server, BlockPos pos) {
        return loadServerBoxHired(server).containsKey(pos);
    }

    public static UUID getServerBoxHiredNpc(MinecraftServer server, BlockPos pos) {
        return loadServerBoxHired(server).get(pos);
    }

    public static Map<BlockPos, UUID> getServerBoxHiredNpcs(MinecraftServer server) {
        return new HashMap<>(loadServerBoxHired(server));
    }

    public static BlockPos findByNpcUuid(MinecraftServer server, UUID npcUuid) {
        for (Map.Entry<BlockPos, UUID> entry : loadServerBoxHired(server).entrySet()) {
            if (entry.getValue().equals(npcUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ══════════════════════════════════════
    //  数据持久化 (V2统一存储)
    // ══════════════════════════════════════

    private static void saveServerBoxHired(MinecraftServer server, Map<BlockPos, UUID> data) {
        EmploymentLegacyBridge.saveAssignments(server, WorkBlockType.LOGISTICS_SERVER_BOX, JobType.WAREHOUSE_MANAGER, data);
    }

    private static Map<BlockPos, UUID> loadServerBoxHired(MinecraftServer server) {
        return EmploymentLegacyBridge.loadAssignments(server, WorkBlockType.LOGISTICS_SERVER_BOX, JobType.WAREHOUSE_MANAGER);
    }
}
