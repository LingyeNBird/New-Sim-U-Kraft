package com.xiaoliang.simukraft.notification;

import java.util.List;
import java.util.UUID;

/**
 * 统一的通知服务接口
 * 
 * 这是一个核心接口，用于所有内部系统发送消息通知。
 * 外部聊天室模组可以实现这个接口来提供自己的通知存储和分发机制。
 * 
 * 使用场景：
 * - NPC发送通知给玩家
 * - 城市事件通知
 * - 官员邀请
 * - 系统提示
 * - 游戏事件通知等
 */
public interface IMessageNotificationService {

    /**
     * 发送一条通知消息
     * 
     * @param notification 通知对象
     * @return 是否发送成功
     */
    boolean sendNotification(MessageNotification notification);

    /**
     * 获取玩家的所有通知
     * 
     * @param playerId 玩家UUID
     * @return 通知列表
     */
    List<MessageNotification> getNotifications(UUID playerId);

    /**
     * 获取玩家特定分类的通知
     * 
     * @param playerId 玩家UUID
     * @param category 消息分类
     * @return 通知列表
     */
    List<MessageNotification> getNotificationsByCategory(UUID playerId, MessageCategory category);

    /**
     * 获取玩家未读通知数
     * 
     * @param playerId 玩家UUID
     * @return 未读通知数
     */
    int getUnreadCount(UUID playerId);

    /**
     * 获取玩家未读通知数（按分类）
     * 
     * @param playerId 玩家UUID
     * @param category 消息分类
     * @return 该分类的未读通知数
     */
    int getUnreadCountByCategory(UUID playerId, MessageCategory category);

    /**
     * 标记通知为已读
     * 
     * @param notificationId 通知ID
     * @return 是否标记成功
     */
    boolean markAsRead(UUID notificationId);

    /**
     * 标记玩家的所有通知为已读
     * 
     * @param playerId 玩家UUID
     * @return 标记成功的通知数
     */
    int markAllAsRead(UUID playerId);

    /**
     * 删除一条通知
     * 
     * @param notificationId 通知ID
     * @return 是否删除成功
     */
    boolean deleteNotification(UUID notificationId);

    /**
     * 删除玩家的所有通知
     * 
     * @param playerId 玩家UUID
     * @return 删除的通知数
     */
    int deleteAllNotifications(UUID playerId);

    /**
     * 删除玩家的指定分类通知
     * 
     * @param playerId 玩家UUID
     * @param category 消息分类
     * @return 删除的通知数
     */
    int deleteNotificationsByCategory(UUID playerId, MessageCategory category);

    /**
     * 获取特定通知
     * 
     * @param notificationId 通知ID
     * @return 通知对象，如果不存在返回null
     */
    MessageNotification getNotification(UUID notificationId);

    /**
     * 清空玩家在特定时间之前的所有通知
     * 
     * @param playerId 玩家UUID
     * @param beforeTimestamp 时间戳（毫秒）
     * @return 清空的通知数
     */
    int clearNotificationsBefore(UUID playerId, long beforeTimestamp);
}

