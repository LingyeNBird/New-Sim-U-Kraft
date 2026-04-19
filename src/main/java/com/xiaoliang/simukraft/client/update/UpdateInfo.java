package com.xiaoliang.simukraft.client.update;

/**
 * 更新信息数据类
 */
public record UpdateInfo(
    String tagName,
    String releaseName,
    String body,
    String htmlUrl,
    String downloadUrl,
    boolean prerelease,
    String publishedAt
) {
    public String getVersionNumber() {
        String version = tagName;
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }

    public String getFormattedDate() {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return "";
        }
        try {
            return publishedAt.substring(0, 10);
        } catch (Exception e) {
            return publishedAt;
        }
    }

    public boolean hasDownloadUrl() {
        return downloadUrl != null && !downloadUrl.isEmpty();
    }
}
