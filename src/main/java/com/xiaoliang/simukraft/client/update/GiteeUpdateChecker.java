package com.xiaoliang.simukraft.client.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class GiteeUpdateChecker {

    private static final String GATEE_API_BASE = "https://gitee.com/api/v5";
    private static final Gson GSON = new GsonBuilder().create();
    // 支持格式: 1.0.4, 1.0.4b2, 1.0.4b2-fix1, v1.0.0, 1.0.0-alpha, 等
    private static final Pattern VERSION_PATTERN = Pattern.compile("v?(\\d+)\\.(\\d+)\\.(\\d+)([a-zA-Z]*\\d*)?(?:-([a-zA-Z]+\\d*))?");

    private final String owner;
    private final String repo;
    private final String currentVersion;

    private UpdateInfo latestUpdate;
    private boolean checkInProgress = false;
    private long lastCheckTime = 0;
    private static final long CHECK_COOLDOWN_MS = 5 * 60 * 1000;

    public GiteeUpdateChecker(String owner, String repo, String currentVersion) {
        this.owner = owner;
        this.repo = repo;
        this.currentVersion = currentVersion;
    }

    public CompletableFuture<UpdateInfo> checkForUpdates() {
        CompletableFuture<UpdateInfo> future = new CompletableFuture<>();

        if (checkInProgress) {
            return CompletableFuture.completedFuture(latestUpdate);
        }

        if (System.currentTimeMillis() - lastCheckTime < CHECK_COOLDOWN_MS && latestUpdate != null) {
            return CompletableFuture.completedFuture(latestUpdate);
        }

        checkInProgress = true;
        lastCheckTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try {
                UpdateInfo info = fetchLatestRelease();
                this.latestUpdate = info;
                future.complete(info);
            } catch (Exception e) {
                com.xiaoliang.simukraft.Simukraft.LOGGER.error("Failed to check for updates from Gitee", e);
                future.completeExceptionally(e);
            } finally {
                checkInProgress = false;
            }
        });

        return future;
    }

    private UpdateInfo fetchLatestRelease() throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/releases/latest", GATEE_API_BASE, owner, repo);

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Simukraft-UpdateChecker/1.0");

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                return parseReleaseResponse(response);
            }
        } finally {
            connection.disconnect();
        }
    }

    private UpdateInfo parseReleaseResponse(String response) {
        JsonObject json = GSON.fromJson(response, JsonObject.class);

        String tagName = json.has("tag_name") ? json.get("tag_name").getAsString().trim() : "";
        String releaseName = json.has("name") ? json.get("name").getAsString().trim() : tagName;
        String body = json.has("body") ? json.get("body").getAsString() : "";
        String htmlUrl = json.has("html_url") ? json.get("html_url").getAsString() : "";
        boolean prerelease = json.has("prerelease") && json.get("prerelease").getAsBoolean();
        String publishedAt = json.has("published_at") ? json.get("published_at").getAsString() : "";

        String downloadUrl = "";
        JsonArray assets = json.has("assets") ? json.get("assets").getAsJsonArray() : null;

        com.xiaoliang.simukraft.Simukraft.LOGGER.info("Parsing release response. Assets count: {}", assets != null ? assets.size() : 0);

        if (assets != null && assets.size() > 0) {
            for (int i = 0; i < assets.size(); i++) {
                JsonObject asset = assets.get(i).getAsJsonObject();
                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                String url = asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : "";
                com.xiaoliang.simukraft.Simukraft.LOGGER.info("Asset {}: name={}, url={}", i, name, url);
                if (name.endsWith(".jar") || name.endsWith(".zip")) {
                    downloadUrl = url;
                    break;
                }
            }
        }

        String assetsUrl = "";
        if (assets != null && assets.size() > 0) {
            JsonObject firstAsset = assets.get(0).getAsJsonObject();
            assetsUrl = firstAsset.has("browser_download_url") ? firstAsset.get("browser_download_url").getAsString() : "";
        }

        if (downloadUrl.isEmpty() && !assetsUrl.isEmpty()) {
            downloadUrl = assetsUrl;
        }

        com.xiaoliang.simukraft.Simukraft.LOGGER.info("Final download URL: {}", downloadUrl);

        return new UpdateInfo(
            tagName,
            releaseName,
            body,
            htmlUrl,
            downloadUrl,
            prerelease,
            publishedAt
        );
    }

    public boolean isNewerVersionAvailable() {
        if (latestUpdate == null) {
            com.xiaoliang.simukraft.Simukraft.LOGGER.info("[isNewerVersionAvailable] latestUpdate is null, return false");
            return false;
        }

        String latestTag = latestUpdate.tagName();
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[isNewerVersionAvailable] currentVersion='{}', latestTag='{}'", currentVersion, latestTag);

        int result = compareVersions(currentVersion, latestTag);
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[isNewerVersionAvailable] compareVersions result={}, return {}", result, result < 0);

        // 如果 compareVersions 返回负数，说明 latest 比 current 新
        return result < 0;
    }

    public int compareVersions(String current, String latest) {
        VersionInfo currentInfo = parseVersion(current);
        VersionInfo latestInfo = parseVersion(latest);

        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] current='{}' -> parts=[{}, {}, {}], sub='{}', fix='{}'",
            current, currentInfo.parts[0], currentInfo.parts[1], currentInfo.parts[2], currentInfo.subVersion, currentInfo.fixVersion);
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] latest='{}' -> parts=[{}, {}, {}], sub='{}', fix='{}'",
            latest, latestInfo.parts[0], latestInfo.parts[1], latestInfo.parts[2], latestInfo.subVersion, latestInfo.fixVersion);

        // 比较主版本号
        for (int i = 0; i < 3; i++) {
            if (currentInfo.parts[i] < latestInfo.parts[i]) {
                com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] parts[{}]: {} < {}, return -1", i, currentInfo.parts[i], latestInfo.parts[i]);
                return -1;
            } else if (currentInfo.parts[i] > latestInfo.parts[i]) {
                com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] parts[{}]: {} > {}, return 1", i, currentInfo.parts[i], latestInfo.parts[i]);
                return 1;
            }
        }

        // 比较小版本后缀 (如 b2)
        int subVersionCompare = compareSubVersion(currentInfo.subVersion, latestInfo.subVersion);
        if (subVersionCompare != 0) {
            com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] subVersionCompare={}, return {}", subVersionCompare, subVersionCompare);
            return subVersionCompare;
        }

        // 比较修复后缀 (如 fix1)
        int fixCompare = compareFixVersion(currentInfo.fixVersion, latestInfo.fixVersion);
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[VersionCompare] fixCompare={}, return {}", fixCompare, fixCompare);
        return fixCompare;
    }

    /**
     * 版本信息内部类
     */
    private static class VersionInfo {
        int[] parts = {0, 0, 0};  // 主版本号 [major, minor, patch]
        String subVersion = "";    // 小版本后缀 (如 b2, rc1)
        String fixVersion = "";    // 修复后缀 (如 fix1, hotfix2)
    }

    private VersionInfo parseVersion(String version) {
        VersionInfo info = new VersionInfo();

        // 清理版本号：去除前后空格，统一处理v前缀
        String cleanVersion = version != null ? version.trim() : "";
        
        Matcher matcher = VERSION_PATTERN.matcher(cleanVersion);
        boolean matches = matcher.matches();
        com.xiaoliang.simukraft.Simukraft.LOGGER.info("[ParseVersion] raw='{}', clean='{}', matcher.matches()={}", version, cleanVersion, matches);
        
        if (matches) {
            try {
                info.parts[0] = Integer.parseInt(matcher.group(1));
                info.parts[1] = Integer.parseInt(matcher.group(2));
                info.parts[2] = Integer.parseInt(matcher.group(3));
                info.subVersion = matcher.group(4) != null ? matcher.group(4) : "";
                info.fixVersion = matcher.group(5) != null ? matcher.group(5) : "";
                com.xiaoliang.simukraft.Simukraft.LOGGER.info("[ParseVersion] Parsed: parts=[{}, {}, {}], sub='{}', fix='{}'",
                    info.parts[0], info.parts[1], info.parts[2], info.subVersion, info.fixVersion);
            } catch (NumberFormatException e) {
                com.xiaoliang.simukraft.Simukraft.LOGGER.warn("Failed to parse version: " + cleanVersion, e);
            }
        } else {
            com.xiaoliang.simukraft.Simukraft.LOGGER.warn("[ParseVersion] No match found for version: {}", cleanVersion);
        }

        return info;
    }

    /**
     * 比较小版本后缀
     * 规则: 空后缀 > 有后缀，b(beta) < rc(release candidate) < 正式版
     * 例如: 1.0.4 < 1.0.4b1 < 1.0.4b2 < 1.0.4rc1 < 1.0.4
     */
    private int compareSubVersion(String current, String latest) {
        int currentPriority = getSubVersionPriority(current);
        int latestPriority = getSubVersionPriority(latest);

        if (currentPriority != latestPriority) {
            return Integer.compare(currentPriority, latestPriority);
        }

        // 同类型比较数字
        int currentNum = extractNumber(current);
        int latestNum = extractNumber(latest);

        return Integer.compare(currentNum, latestNum);
    }

    private int getSubVersionPriority(String subVersion) {
        if (subVersion.isEmpty()) return 100;  // 正式版优先级最高
        if (subVersion.startsWith("rc")) return 90;  // 候选版
        if (subVersion.startsWith("b")) return 80;   // beta版
        if (subVersion.startsWith("a")) return 70;   // alpha版
        return 50;  // 其他
    }

    private int extractNumber(String str) {
        if (str == null || str.isEmpty()) return 0;
        StringBuilder num = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            }
        }
        try {
            return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 比较修复版本后缀
     * 规则: 有修复 > 无修复，数字越大越新
     * 例如: 1.0.4b2 < 1.0.4b2-fix1 < 1.0.4b2-fix2
     */
    private int compareFixVersion(String current, String latest) {
        int currentNum = extractNumber(current);
        int latestNum = extractNumber(latest);
        return Integer.compare(currentNum, latestNum);
    }

    public UpdateInfo getLatestUpdate() {
        return latestUpdate;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void clearCache() {
        this.latestUpdate = null;
        this.lastCheckTime = 0;
    }
}
