package com.xiaoliang.simukraft.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "simukraft")
public class BakeryDailyWorkManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static long lastLogTick = 0;
    private static final long LOG_INTERVAL_TICKS = 200; // 每10秒记录一次日志
    
    /**
     * 服务器每tick触发的事件，用于检测傍晚时分并处理每日工作
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // 获取服务器实例
        if (event.getServer() == null) return;
        
        // 遍历所有服务器世界
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getDayTime();
            long timeOfDay = gameTime % 24000;
            
            // 减少日志输出频率，只在傍晚时分附近且间隔一定时间才输出DEBUG日志
            if (timeOfDay >= 11900 && timeOfDay <= 12100) {
                if (gameTime - lastLogTick >= LOG_INTERVAL_TICKS) {
                    LOGGER.debug("BakeryDailyWorkManager: 检查中午销售任务，当前时间={}", timeOfDay);
                    lastLogTick = gameTime;
                }
            }
            
            // 面包店销售任务已合并到 CommercialWorkHandler
            // BakeryOwnerDailyWorkHandler.processEveningSales(level);
        }
    }
}