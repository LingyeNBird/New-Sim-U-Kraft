package com.xiaoliang.simukraft.utils;

import com.mojang.logging.LogUtils;
import com.xiaoliang.simukraft.notification.IMessageNotificationService;
import com.xiaoliang.simukraft.notification.MessageCategory;
import com.xiaoliang.simukraft.notification.MessageNotification;
import com.xiaoliang.simukraft.notification.NotificationServiceManager;
import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

/**
 * 城市消息工具类
 * 所有游戏内通知的唯一入口，通过 {@link NotificationServiceManager} 分发。
 * <p>
 * 当 CityChat 等外部模组注册了自定义服务时，消息由外部模组处理（存储、频道投递、HUD 弹窗）。
 * 未注册时，内置的 NullNotificationService 会将消息降级为原版 sendSystemMessage。
 * <p>
 * 推荐使用接受 {@link Component} 参数的重载方法，以便客户端根据玩家语言设置正确翻译。
 */
public class CityMessageUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ────────────────── 公开 API ──────────────────

    /**
     * 发送消息到城市群组（所有在线城市成员），使用指定分类。
     * 推荐使用 Component 版本以支持客户端翻译。
     */
    public static void sendToCityGroup(MinecraftServer server, UUID cityId,
                                        Component message, MessageCategory category) {
        if (server == null || cityId == null || message == null) return;

        String caller = getCaller();
        String msgStr = message.getString();
        LOGGER.info("[SK-NOTIFY] ENTER | method=sendToCityGroup | caller={} | cityId={} | category={} | msg={}",
                caller, cityId, category, truncate(msgStr));

        CityData cityData = CityData.get(server.overworld());
        IMessageNotificationService service = NotificationServiceManager.getService();
        int sent = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            UUID playerCityId = cityData.getPlayerCityId(playerName);
            if (!cityId.equals(playerCityId)) continue;

            MessageNotification notification = buildNotification(
                    server, cityId,
                    Component.translatable("notify.title.pioneer_helper"),
                    "SYSTEM",
                    Component.translatable("notify.title.notification"),
                    message,
                    player.getUUID(), category);

            LOGGER.info("[SK-NOTIFY] SEND | id={} | recipient={} | recipientName={} | category={} | cityName={} | service={}",
                    notification.getId(), player.getUUID(), playerName, category,
                    notification.getCityName(), service.getClass().getSimpleName());

            service.sendNotification(notification);
            sent++;
        }

        LOGGER.info("[SK-NOTIFY] DONE | method=sendToCityGroup | cityId={} | totalSent={}", cityId, sent);
    }

    /**
     * 发送纯字符串消息到城市群组（向后兼容）
     */
    public static void sendToCityGroup(MinecraftServer server, UUID cityId,
                                        String message, MessageCategory category) {
        sendToCityGroup(server, cityId, Component.literal(Objects.requireNonNull(message)), category);
    }

    /**
     * 发送消息到城市群组，默认使用 CITIZEN 分类
     */
    public static void sendToCityGroup(MinecraftServer server, UUID cityId, String message) {
        sendToCityGroup(server, cityId, message, MessageCategory.CITIZEN);
    }

    /**
     * 发送消息到城市群组，默认使用 CITIZEN 分类（Component 版本）
     */
    public static void sendToCityGroup(MinecraftServer server, UUID cityId, Component message) {
        sendToCityGroup(server, cityId, message, MessageCategory.CITIZEN);
    }

    /**
     * 通过通知服务发送消息给城市市长。
     * 推荐使用 Component 版本以支持客户端翻译。
     */
    public static void sendToMayorViaService(MinecraftServer server, UUID cityId,
                                              Component title, Component content,
                                              MessageCategory category) {
        if (server == null || cityId == null) return;

        String caller = getCaller();
        CityData cityData = CityData.get(server.overworld());
        CityData.CityInfo cityInfo = cityData.getCity(cityId);
        if (cityInfo == null || cityInfo.getMayorId() == null) {
            LOGGER.warn("[SK-NOTIFY] DROP | method=sendToMayorViaService | caller={} | cityId={} | reason=cityInfo_or_mayor_null", caller, cityId);
            return;
        }

        IMessageNotificationService service = NotificationServiceManager.getService();
        MessageNotification notification = buildNotification(
                server, cityId,
                Component.translatable("notify.title.pioneer_helper"),
                "SYSTEM",
                title, content,
                cityInfo.getMayorId(), category);

        LOGGER.info("[SK-NOTIFY] SEND | id={} | method=sendToMayorViaService | caller={} | recipient={} | category={} | cityName={} | title={} | service={}",
                notification.getId(), caller, cityInfo.getMayorId(), category,
                notification.getCityName(), title.getString(), service.getClass().getSimpleName());

        service.sendNotification(notification);
    }

    /**
     * 通过通知服务发送消息给城市市长（纯字符串版本，向后兼容）
     */
    public static void sendToMayorViaService(MinecraftServer server, UUID cityId,
                                              String title, String content,
                                              MessageCategory category) {
        sendToMayorViaService(
                server,
                cityId,
                Component.literal(Objects.requireNonNull(title)),
                Component.literal(Objects.requireNonNull(content)),
                category
        );
    }

    // ────────────────── 内部方法 ──────────────────

    /**
     * 构建通知对象，同时设置 String 字段（CityChat 兼容）和 Component 字段（客户端翻译）
     */
    private static MessageNotification buildNotification(
            MinecraftServer server, UUID cityId,
            Component sender, String senderType,
            Component title, Component content,
            UUID recipientId, MessageCategory category) {

        // String 字段用于 CityChat 等外部模组和序列化
        MessageNotification n = new MessageNotification(
                sender.getString(), senderType, title.getString(), content.getString(), recipientId, category);
        n.setRelatedEntityId(cityId);
        n.setRelatedEntityType("CITY");

        // Component 字段用于 NullNotificationService 客户端翻译
        n.setContentComponent(content);
        n.setTitleComponent(title);

        // 将 Component JSON 存入 metadata，供 CityChat 客户端反序列化后按玩家语言翻译
        try {
            n.putMetadata("contentJson", net.minecraft.network.chat.Component.Serializer.toJson(content));
            n.putMetadata("titleJson", net.minecraft.network.chat.Component.Serializer.toJson(title));
            n.putMetadata("senderJson", net.minecraft.network.chat.Component.Serializer.toJson(sender));
        } catch (Exception ignored) {
        }

        // 填充城市名称
        if (cityId != null) {
            CityData cityData = CityData.get(server.overworld());
            CityData.CityInfo cityInfo = cityData.getCity(cityId);
            if (cityInfo != null) {
                n.setCityName(Objects.requireNonNull(cityInfo.getCityName()));
            }
        }

        // 填充分类显示名（同时设置 String 和 Component）
        if (category != null) {
            n.setCategoryDisplayName(category.getDisplayName());
            n.setCategoryDisplayComponent(
                    Component.translatable(Objects.requireNonNull(category.getTranslationKey()))
            );
        }

        return n;
    }

    /**
     * 获取调用方类名和方法名（跳过 CityMessageUtils 自身的栈帧）
     */
    private static String getCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // 0=getStackTrace, 1=getCaller, 2=public method, 3+=实际调用方
        for (int i = 2; i < stack.length; i++) {
            if (!stack[i].getClassName().equals(CityMessageUtils.class.getName())) {
                StackTraceElement e = stack[i];
                String className = e.getClassName();
                int dot = className.lastIndexOf('.');
                return (dot >= 0 ? className.substring(dot + 1) : className) + "." + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        return "unknown";
    }

    /**
     * 截断消息内容用于日志输出，最多 60 字符
     */
    private static String truncate(String msg) {
        if (msg == null) return "null";
        return msg.length() <= 60 ? msg : msg.substring(0, 60) + "...";
    }
}
