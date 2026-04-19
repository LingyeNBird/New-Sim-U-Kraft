package com.xiaoliang.simukraft.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 简化的日志工具类
 * 提供统一的日志输出接口，支持通过开关控制日志级别
 */
public class SimplifiedLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 日志级别控制开关
    private static boolean DEBUG_ENABLED = false;  // 调试日志开关
    private static boolean INFO_ENABLED = true;    // 信息日志开关
    private static final boolean WARN_ENABLED = true;    // 警告日志开关
    private static final boolean ERROR_ENABLED = true;   // 错误日志开关
    
    /**
     * 设置调试日志开关
     */
    public static void setDebugEnabled(boolean enabled) {
        DEBUG_ENABLED = enabled;
    }
    
    /**
     * 设置信息日志开关
     */
    public static void setInfoEnabled(boolean enabled) {
        INFO_ENABLED = enabled;
    }
    
    /**
     * 调试日志 - 仅在DEBUG_ENABLED为true时输出
     */
    public static void debug(String message) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[DEBUG] {}", message);
        }
    }
    
    /**
     * 调试日志 - 带格式参数
     */
    public static void debug(String format, Object... args) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[DEBUG] " + format, args);
        }
    }
    
    /**
     * 信息日志
     */
    public static void info(String message) {
        if (INFO_ENABLED) {
            LOGGER.info("[INFO] {}", message);
        }
    }
    
    /**
     * 信息日志 - 带格式参数
     */
    public static void info(String format, Object... args) {
        if (INFO_ENABLED) {
            LOGGER.info("[INFO] " + format, args);
        }
    }
    
    /**
     * 警告日志
     */
    public static void warn(String message) {
        if (WARN_ENABLED) {
            LOGGER.warn("[WARN] {}", message);
        }
    }
    
    /**
     * 警告日志 - 带格式参数
     */
    public static void warn(String format, Object... args) {
        if (WARN_ENABLED) {
            LOGGER.warn("[WARN] " + format, args);
        }
    }
    
    /**
     * 错误日志
     */
    public static void error(String message) {
        if (ERROR_ENABLED) {
            LOGGER.error("[ERROR] {}", message);
        }
    }
    
    /**
     * 错误日志 - 带异常
     */
    public static void error(String message, Throwable throwable) {
        if (ERROR_ENABLED) {
            LOGGER.error("[ERROR] " + message, throwable);
        }
    }
    
    /**
     * 错误日志 - 带格式参数
     */
    public static void error(String format, Object... args) {
        if (ERROR_ENABLED) {
            LOGGER.error("[ERROR] " + format, args);
        }
    }
    
    /**
     * 网络包日志 - 简化版，仅在DEBUG模式下输出
     */
    public static void packet(String packetName, String message) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[PACKET:{}] {}", packetName, message);
        }
    }
    
    /**
     * 网络包日志 - 带格式参数
     */
    public static void packet(String packetName, String format, Object... args) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[PACKET:{}] " + format, packetName, args);
        }
    }
    
    /**
     * NPC相关日志
     */
    public static void npc(String npcName, String message) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[NPC:{}] {}", npcName, message);
        }
    }
    
    /**
     * 建筑相关日志
     */
    public static void building(String buildingName, String message) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[BUILDING:{}] {}", buildingName, message);
        }
    }
    
    /**
     * 数据管理日志
     */
    public static void data(String dataType, String message) {
        if (DEBUG_ENABLED) {
            LOGGER.debug("[DATA:{}] {}", dataType, message);
        }
    }
}
