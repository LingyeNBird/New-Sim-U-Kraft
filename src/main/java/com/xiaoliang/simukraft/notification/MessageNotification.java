package com.xiaoliang.simukraft.notification;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一的消息通知对象
 * 用于系统内所有的消息通知，包括：
 * - NPC通知
 * - 城市消息
 * - 官员邀请
 * - 系统提示
 * - 其他玩家消息等
 */
public class MessageNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private long timestamp;
    private String sender;  // 发送者名称（可能是NPC、系统或玩家）
    private String senderType;  // SYSTEM, NPC, PLAYER, OFFICIAL_INVITATION等
    private String title;  // 消息标题
    private String content;  // 消息内容
    private UUID recipientId;  // 接收者UUID（玩家）
    private boolean isRead;  // 是否已读
    private MessageCategory category;  // 消息分类

    // 城市与分类显示信息（供 CityChat 等外部模组直接使用，避免二次查询）
    private String cityName;  // 城市显示名称
    private String categoryDisplayName;  // 分类显示名，如 "[城市]"、"[系统]"

    // 可选的关联数据
    private UUID relatedEntityId;  // 相关实体ID（如城市ID、NPC ID等）
    private String relatedEntityType;  // 相关实体类型（CITY, NPC, OFFICIAL_POSITION等）
    private Map<String, String> metadata;  // 额外的元数据

    // 客户端可翻译的 Component（transient，不参与序列化）
    // 当设置后，NullNotificationService 会优先使用这些 Component 发送给玩家，
    // 让客户端根据玩家自己的语言设置来解析翻译，而不是在服务端解析为 en_us。
    private transient net.minecraft.network.chat.Component contentComponent;
    private transient net.minecraft.network.chat.Component titleComponent;
    private transient net.minecraft.network.chat.Component categoryDisplayComponent;

    public MessageNotification() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.metadata = new HashMap<>();
    }

    public MessageNotification(String sender, String senderType, String title, String content, 
                              UUID recipientId, MessageCategory category) {
        this();
        this.sender = sender;
        this.senderType = senderType;
        this.title = title;
        this.content = content;
        this.recipientId = recipientId;
        this.category = category;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public MessageCategory getCategory() {
        return category;
    }

    public void setCategory(MessageCategory category) {
        this.category = category;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(UUID relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void putMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public String getMetadata(String key) {
        return this.metadata.get(key);
    }

    // ── Component（可翻译）字段 ──

    public net.minecraft.network.chat.Component getContentComponent() {
        return contentComponent;
    }

    public void setContentComponent(net.minecraft.network.chat.Component contentComponent) {
        this.contentComponent = contentComponent;
    }

    public net.minecraft.network.chat.Component getTitleComponent() {
        return titleComponent;
    }

    public void setTitleComponent(net.minecraft.network.chat.Component titleComponent) {
        this.titleComponent = titleComponent;
    }

    public net.minecraft.network.chat.Component getCategoryDisplayComponent() {
        return categoryDisplayComponent;
    }

    public void setCategoryDisplayComponent(net.minecraft.network.chat.Component categoryDisplayComponent) {
        this.categoryDisplayComponent = categoryDisplayComponent;
    }

    // ── Action 按钮支持 ──

    /**
     * 添加一个可点击的 action 按钮。CC 等外部模组会将其渲染为可点击按钮。
     *
     * @param label   按钮文本，如 "[接受]"
     * @param command 点击执行的命令（不带 /），如 "skofficial accept xxx"
     * @param color   按钮颜色，如 0x55FF55
     */
    public void addAction(String label, String command, int color) {
        int count = getActionCount();
        metadata.put("action." + count + ".label", label);
        metadata.put("action." + count + ".command", command);
        metadata.put("action." + count + ".color", Integer.toHexString(color));
        metadata.put("action.count", String.valueOf(count + 1));
    }

    /**
     * 获取 action 数量。
     */
    public int getActionCount() {
        String countStr = metadata.get("action.count");
        if (countStr == null) return 0;
        try { return Integer.parseInt(countStr); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override
    public String toString() {
        return "MessageNotification{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", sender='" + sender + '\'' +
                ", senderType='" + senderType + '\'' +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", cityName='" + cityName + '\'' +
                ", categoryDisplayName='" + categoryDisplayName + '\'' +
                ", recipientId=" + recipientId +
                ", isRead=" + isRead +
                '}';
    }
}


