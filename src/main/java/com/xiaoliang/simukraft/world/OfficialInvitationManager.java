package com.xiaoliang.simukraft.world;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 官员邀请管理器
 * 管理通过开拓助手发送的官员邀请，包含5分钟有效期
 */
public class OfficialInvitationManager {
    
    // 邀请有效期：5分钟（毫秒）
    private static final long INVITATION_EXPIRY_MS = 5 * 60 * 1000;
    
    // 单例实例
    private static OfficialInvitationManager instance;
    
    // 邀请缓存：key = 邀请ID, value = 邀请信息
    private final Map<UUID, OfficialInvitation> invitations = new ConcurrentHashMap<>();
    
    private OfficialInvitationManager() {
    }
    
    public static synchronized OfficialInvitationManager getInstance() {
        if (instance == null) {
            instance = new OfficialInvitationManager();
        }
        return instance;
    }
    
    /**
     * 创建新的官员邀请
     * @param cityId 城市ID
     * @param cityName 城市名称
     * @param mayorId 市长ID
     * @param mayorName 市长名称
     * @param targetPlayerId 目标玩家ID
     * @param targetPlayerName 目标玩家名称
     * @return 邀请ID
     */
    public UUID createInvitation(UUID cityId, String cityName, UUID mayorId, String mayorName, 
                                  UUID targetPlayerId, String targetPlayerName) {
        // 先清理该玩家之前的邀请
        removeInvitationsForPlayer(targetPlayerId);
        
        UUID invitationId = UUID.randomUUID();
        long expiryTime = System.currentTimeMillis() + INVITATION_EXPIRY_MS;
        
        OfficialInvitation invitation = new OfficialInvitation(
            invitationId, cityId, cityName, mayorId, mayorName, 
            targetPlayerId, targetPlayerName, expiryTime
        );
        
        invitations.put(invitationId, invitation);
        System.out.println("[OfficialInvitationManager] 创建官员邀请: " + invitationId + 
                          " 城市: " + cityName + " 目标玩家: " + targetPlayerName);
        
        return invitationId;
    }
    
    /**
     * 获取邀请信息
     * @param invitationId 邀请ID
     * @return 邀请信息，如果不存在或已过期则返回null
     */
    public OfficialInvitation getInvitation(UUID invitationId) {
        cleanupExpiredInvitations();
        
        OfficialInvitation invitation = invitations.get(invitationId);
        if (invitation == null) {
            return null;
        }
        
        if (invitation.isExpired()) {
            invitations.remove(invitationId);
            return null;
        }
        
        return invitation;
    }
    
    /**
     * 接受邀请
     * @param invitationId 邀请ID
     * @param playerId 接受邀请的玩家ID（用于验证）
     * @return 是否成功接受
     */
    public boolean acceptInvitation(UUID invitationId, UUID playerId) {
        OfficialInvitation invitation = getInvitation(invitationId);
        if (invitation == null) {
            System.out.println("[OfficialInvitationManager] 邀请不存在或已过期: " + invitationId);
            return false;
        }
        
        if (!invitation.getTargetPlayerId().equals(playerId)) {
            System.out.println("[OfficialInvitationManager] 玩家无权接受此邀请: " + playerId);
            return false;
        }
        
        invitations.remove(invitationId);
        System.out.println("[OfficialInvitationManager] 邀请已接受: " + invitationId);
        return true;
    }
    
    /**
     * 拒绝邀请
     * @param invitationId 邀请ID
     * @param playerId 拒绝邀请的玩家ID（用于验证）
     * @return 是否成功拒绝
     */
    public boolean rejectInvitation(UUID invitationId, UUID playerId) {
        OfficialInvitation invitation = getInvitation(invitationId);
        if (invitation == null) {
            return false;
        }
        
        if (!invitation.getTargetPlayerId().equals(playerId)) {
            return false;
        }
        
        invitations.remove(invitationId);
        System.out.println("[OfficialInvitationManager] 邀请已拒绝: " + invitationId);
        return true;
    }
    
    /**
     * 移除指定邀请
     * @param invitationId 邀请ID
     */
    public void removeInvitation(UUID invitationId) {
        invitations.remove(invitationId);
        System.out.println("[OfficialInvitationManager] 邀请已移除: " + invitationId);
    }

    /**
     * 移除指定玩家的所有邀请
     * @param playerId 玩家ID
     */
    public void removeInvitationsForPlayer(UUID playerId) {
        invitations.entrySet().removeIf(entry -> 
            entry.getValue().getTargetPlayerId().equals(playerId)
        );
    }
    
    /**
     * 清理过期邀请
     */
    private void cleanupExpiredInvitations() {
        invitations.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                System.out.println("[OfficialInvitationManager] 清理过期邀请: " + entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * 官员邀请信息类
     */
    public static class OfficialInvitation {
        private final UUID invitationId;
        private final UUID cityId;
        private final String cityName;
        private final UUID mayorId;
        private final String mayorName;
        private final UUID targetPlayerId;
        private final String targetPlayerName;
        private final long expiryTime;
        
        public OfficialInvitation(UUID invitationId, UUID cityId, String cityName, 
                                   UUID mayorId, String mayorName, 
                                   UUID targetPlayerId, String targetPlayerName, 
                                   long expiryTime) {
            this.invitationId = invitationId;
            this.cityId = cityId;
            this.cityName = cityName;
            this.mayorId = mayorId;
            this.mayorName = mayorName;
            this.targetPlayerId = targetPlayerId;
            this.targetPlayerName = targetPlayerName;
            this.expiryTime = expiryTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public long getRemainingTimeMs() {
            return Math.max(0, expiryTime - System.currentTimeMillis());
        }
        
        // Getters
        public UUID getInvitationId() { return invitationId; }
        public UUID getCityId() { return cityId; }
        public String getCityName() { return cityName; }
        public UUID getMayorId() { return mayorId; }
        public String getMayorName() { return mayorName; }
        public UUID getTargetPlayerId() { return targetPlayerId; }
        public String getTargetPlayerName() { return targetPlayerName; }
        public long getExpiryTime() { return expiryTime; }
    }
}
