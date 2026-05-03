package com.xiaoliang.simukraft.notification;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

/**
 * 消息通知服务管理器
 * 
 * 这是一个适配器管理器，用于在主项目中管理通知服务实现。
 * 
 * 使用场景：
 * - 主项目启动时注册默认或自定义的通知服务实现
 * - 运行时获取当前的通知服务实现
 * - 支持热插拔不同的通知服务实现
 * 
 * 默认情况下，提供一个无操作的实现（NullNotificationService）。
 * 外部模组（如聊天室模组）可以注册自己的实现。
 */
public class NotificationServiceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static IMessageNotificationService instance = new NullNotificationService();
    private static final Object LOCK = new Object();
    private static volatile MinecraftServer server;

    private NotificationServiceManager() {
    }

    /**
     * 注册通知服务实现
     * 
     * @param service 通知服务实现
     */
    public static void registerService(IMessageNotificationService service) {
        synchronized (LOCK) {
            if (service != null) {
                instance = service;
                LOGGER.info("[SK-NOTIFY] SERVICE-REGISTER | service={}", service.getClass().getSimpleName());
            }
        }
    }

    /**
     * 获取当前的通知服务实现
     * 
     * @return 通知服务实例
     */
    public static IMessageNotificationService getService() {
        synchronized (LOCK) {
            return instance;
        }
    }

    /**
     * 重置为默认的空实现
     */
    public static void reset() {
        synchronized (LOCK) {
            instance = new NullNotificationService();
            LOGGER.info("[SK-NOTIFY] SERVICE-RESET | reverted to NullNotificationService");
        }
    }

    /**
     * 绑定当前服务器实例，供默认通知兜底使用。
     */
    public static void bindServer(MinecraftServer currentServer) {
        server = currentServer;
    }

    /**
     * 清除服务器绑定。
     */
    public static void clearServer() {
        server = null;
    }

    /**
     * 检查是否已注册非默认的服务
     * 
     * @return true 如果已注册非默认服务
     */
    public static boolean isCustomServiceRegistered() {
        synchronized (LOCK) {
            return !(instance instanceof NullNotificationService);
        }
    }

    /**
     * 默认的无操作通知服务实现
     * 当没有实际的服务实现时使用
     */
    private static class NullNotificationService implements IMessageNotificationService {
        @Override
        public boolean sendNotification(MessageNotification notification) {
            if (notification == null || notification.getRecipientId() == null) {
                LOGGER.warn("[SK-NOTIFY] FALLBACK-DROP | reason=null_notification_or_recipient");
                return false;
            }

            MinecraftServer currentServer = server;
            if (currentServer == null) {
                LOGGER.warn("[SK-NOTIFY] FALLBACK-DROP | id={} | reason=server_null | recipient={}",
                        notification.getId(), notification.getRecipientId());
                return false;
            }

            ServerPlayer target = currentServer.getPlayerList().getPlayer(
                    Objects.requireNonNull(notification.getRecipientId())
            );
            if (target == null) {
                LOGGER.warn("[SK-NOTIFY] FALLBACK-DROP | id={} | reason=player_offline | recipient={}",
                        notification.getId(), notification.getRecipientId());
                return false;
            }

            // 优先使用 Component 字段（客户端自行翻译），降级为 String 字段
            net.minecraft.network.chat.Component categoryComp = notification.getCategoryDisplayComponent();
            net.minecraft.network.chat.Component contentComp = notification.getContentComponent();
            net.minecraft.network.chat.Component titleComp = notification.getTitleComponent();

            // 构建前缀：优先用 categoryDisplayComponent，其次用 titleComponent，最后降级为字符串
            net.minecraft.network.chat.MutableComponent prefixComp;
            if (categoryComp != null) {
                prefixComp = categoryComp.copy();
            } else if (notification.getCategoryDisplayName() != null) {
                prefixComp = Component.literal(Objects.requireNonNull(notification.getCategoryDisplayName()));
            } else if (titleComp != null) {
                prefixComp = Component.literal("[")
                        .append(Objects.requireNonNull(titleComp))
                        .append(Objects.requireNonNull(Component.literal("]")));
            } else {
                String titleStr = notification.getTitle();
                if (titleStr == null) {
                    prefixComp = Component.literal("[")
                            .append(Objects.requireNonNull(Component.translatable("notify.title.notification")))
                            .append(Objects.requireNonNull(Component.literal("]")));
                } else {
                    prefixComp = Component.literal("[" + titleStr + "]");
                }
            }

            // 构建内容：优先用 contentComponent，降级为字符串
            net.minecraft.network.chat.Component bodyComp = contentComp != null
                    ? contentComp
                    : Component.literal(notification.getContent() == null ? "" : Objects.requireNonNull(notification.getContent()));

            // 构建带可点击按钮的消息
            net.minecraft.network.chat.MutableComponent message = prefixComp.copy()
                    .append(Objects.requireNonNull(Component.literal(" ")))
                    .append(Objects.requireNonNull(bodyComp));
            
            // 添加 action 按钮
            int actionCount = notification.getActionCount();
            if (actionCount > 0) {
                message.append(Objects.requireNonNull(Component.literal(" ")));
                for (int i = 0; i < actionCount; i++) {
                    String label = notification.getMetadata("action." + i + ".label");
                    String command = notification.getMetadata("action." + i + ".command");
                    String colorStr = notification.getMetadata("action." + i + ".color");
                    
                    if (label != null && command != null) {
                        int buttonColor = 0xFFFFFF;
                        if (colorStr != null) {
                            try {
                                buttonColor = Integer.parseInt(colorStr, 16);
                            } catch (NumberFormatException ignored) {}
                        }
                        final int finalColor = buttonColor;
                        
                        // 创建可点击的文本组件
                        net.minecraft.network.chat.MutableComponent button = Component.literal(label)
                            .withStyle(style -> style
                                .withColor(finalColor)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, 
                                    "/" + command))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    Objects.requireNonNull(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT),
                                    Objects.requireNonNull(Component.translatable("notify.hover.click_execute", command))))
                            );
                        
                        message.append(Objects.requireNonNull(button));
                        if (i < actionCount - 1) {
                            message.append(Objects.requireNonNull(Component.literal(" ")));
                        }
                    }
                }
            }
            
            target.sendSystemMessage(Objects.requireNonNull(message));

            String logContent = notification.getContent() == null ? "" : notification.getContent();
            LOGGER.info("[SK-NOTIFY] FALLBACK-OK | id={} | recipient={} | recipientName={} | category={} | text={}",
                    notification.getId(), notification.getRecipientId(),
                    target.getName().getString(), notification.getCategory(),
                    logContent.length() > 60 ? logContent.substring(0, 60) + "..." : logContent);
            return true;
        }

        @Override
        public java.util.List<MessageNotification> getNotifications(UUID playerId) {
            return new java.util.ArrayList<>();
        }

        @Override
        public java.util.List<MessageNotification> getNotificationsByCategory(UUID playerId, MessageCategory category) {
            return new java.util.ArrayList<>();
        }

        @Override
        public int getUnreadCount(UUID playerId) {
            return 0;
        }

        @Override
        public int getUnreadCountByCategory(UUID playerId, MessageCategory category) {
            return 0;
        }

        @Override
        public boolean markAsRead(UUID notificationId) {
            return false;
        }

        @Override
        public int markAllAsRead(UUID playerId) {
            return 0;
        }

        @Override
        public boolean deleteNotification(UUID notificationId) {
            return false;
        }

        @Override
        public int deleteAllNotifications(UUID playerId) {
            return 0;
        }

        @Override
        public int deleteNotificationsByCategory(UUID playerId, MessageCategory category) {
            return 0;
        }

        @Override
        public MessageNotification getNotification(UUID notificationId) {
            return null;
        }

        @Override
        public int clearNotificationsBefore(UUID playerId, long beforeTimestamp) {
            return 0;
        }
    }
}
