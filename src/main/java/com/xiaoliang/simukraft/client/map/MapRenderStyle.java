package com.xiaoliang.simukraft.client.map;

/**
 * 地图渲染样式枚举。
 * 决定城市地图使用哪种底层地图纹理数据源。
 */
public enum MapRenderStyle {

    /**
     * 自有渲染系统（默认）。
     * 使用 {@link SimuMapManager} 独立扫描和渲染，不依赖任何外部模组。
     */
    SIMUKRAFT,

    /**
     * Xaero's World Map 渲染风格。
     * 利用 Xaero 已有的高质量地图纹理；当 Xaero 不可用时自动降级为 {@link #SIMUKRAFT}。
     */
    XAERO,

    /**
     * FTB Chunks 渲染风格。
     * 利用 FTB Chunks 已有的地图纹理；当 FTB Chunks 不可用时自动降级为 {@link #SIMUKRAFT}。
     */
    FTB;

    /**
     * 根据名称安全解析样式，无效值返回 {@link #SIMUKRAFT}。
     */
    public static MapRenderStyle fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SIMUKRAFT;
        }
    }
}
