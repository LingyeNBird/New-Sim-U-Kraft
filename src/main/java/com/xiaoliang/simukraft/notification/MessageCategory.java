package com.xiaoliang.simukraft.notification;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * 消息分类枚举
 * 用于对消息进行分类和过滤，同时决定 CityChat 中的通知频道划分。
 */
public enum MessageCategory {
    SYSTEM("notify.category.system"),
    CITIZEN("notify.category.citizen"),
    CONSTRUCTION("notify.category.construction"),
    INDUSTRIAL("notify.category.industrial"),
    COMMERCE("notify.category.commerce"),
    FARMING("notify.category.farming"),
    OFFICIAL("notify.category.official"),
    FINANCE("notify.category.finance"),
    OTHER("notify.category.other");

    private final String translationKey;

    MessageCategory(String translationKey) {
        this.translationKey = Objects.requireNonNull(translationKey);
    }

    /**
     * 返回枚举常量名（大写），用于 CityChat 频道 ID 拼接和序列化
     */
    public String getKey() {
        return name();
    }

    /**
     * 返回翻译键
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * 返回本地化显示名，如 "[市民]"、"[建筑]"
     */
    public String getDisplayName() {
        return Objects.requireNonNull(
                Component.translatable(Objects.requireNonNull(translationKey)).getString()
        );
    }

    /**
     * 从 key 字符串重建枚举，用于反序列化。
     * 兼容旧版本的 key（NPC/CITY/BUSINESS 等）。
     */
    public static MessageCategory fromKey(String key) {
        try {
            return valueOf(Objects.requireNonNull(key));
        } catch (IllegalArgumentException e) {
            // 旧值兼容映射
            return switch (key) {
                case "NPC", "CITY" -> CITIZEN;
                case "BUSINESS" -> COMMERCE;
                case "PLAYER_MESSAGE", "GROUP_MESSAGE" -> OTHER;
                default -> OTHER;
            };
        }
    }
}
