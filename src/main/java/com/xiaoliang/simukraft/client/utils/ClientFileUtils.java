package com.xiaoliang.simukraft.client.utils;

import com.xiaoliang.simukraft.utils.FileUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientFileUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 读取指定位置的 job_type（客户端版本）
     */
    public static String readIndustrialJobTypeClient(Minecraft minecraft, BlockPos pos) {
        try {
            if (minecraft.getSingleplayerServer() != null) {
                // 单人游戏，使用服务器端方法
                return FileUtils.readIndustrialJobType(minecraft.getSingleplayerServer(), pos);
            }
            // 多人游戏，无法直接读取文件，返回null
            return null;
        } catch (Exception e) {
            LOGGER.error("客户端读取工业建筑 job_type 失败", e);
        }
        return null;
    }

    /**
     * 读取指定商业建筑位置的 job_type（客户端版本）
     */
    public static String readCommercialJobTypeClient(Minecraft minecraft, BlockPos pos) {
        try {
            if (minecraft.getSingleplayerServer() != null) {
                // 单人游戏，使用服务器端方法
                return FileUtils.readCommercialJobType(minecraft.getSingleplayerServer(), pos);
            }
            // 多人游戏，无法直接读取文件，返回null
            return null;
        } catch (Exception e) {
            LOGGER.error("客户端读取商业建筑 job_type 失败", e);
        }
        return null;
    }

    /**
     * 读取指定商业建筑位置的建筑文件名（客户端版本）
     */
    public static String readCommercialBuildingFileNameClient(Minecraft minecraft, BlockPos pos) {
        try {
            if (minecraft.getSingleplayerServer() != null) {
                // 单人游戏，使用服务器端缓存方法
                return FileUtils.readCommercialBuildingFileNameCached(minecraft.getSingleplayerServer(), pos);
            }
            // 多人游戏，无法直接读取文件，返回null
            return null;
        } catch (Exception e) {
            LOGGER.error("客户端读取商业建筑文件名失败", e);
        }
        return null;
    }
}