package com.xiaoliang.simukraft.farmland;

/**
 * 作物布局类型枚举
 * FULL - 全填充布局（小麦、胡萝卜等）
 * CHECKERBOARD - 棋盘布局（西瓜、南瓜等）
 * RIGHT_CLICK_HARVEST - 右键采摘布局（浆果、番茄等，采摘后不破坏作物）
 */
public enum CropLayoutType {
    FULL,
    CHECKERBOARD,
    RIGHT_CLICK_HARVEST
}
