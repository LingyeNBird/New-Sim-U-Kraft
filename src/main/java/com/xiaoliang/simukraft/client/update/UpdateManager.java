package com.xiaoliang.simukraft.client.update;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@OnlyIn(Dist.CLIENT)
public class UpdateManager {

    private static UpdateManager INSTANCE;

    public enum UpdateState {
        IDLE,
        CHECKING,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        DOWNLOAD_COMPLETE,
        INSTALLING,
        INSTALL_COMPLETE,
        ERROR
    }

    private UpdateState state = UpdateState.IDLE;
    private UpdateInfo currentUpdate;
    private float downloadProgress = 0.0f;
    private long downloadedBytes = 0L;
    private long totalBytes = 0L;
    private long downloadSpeed = 0L; // bytes per second
    private long lastUpdateTime = 0L;
    private long lastDownloadedBytes = 0L;
    private String errorMessage = "";
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);

    private static final String MODS_FOLDER = "mods";

    private UpdateManager() {}

    public static UpdateManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UpdateManager();
        }
        return INSTANCE;
    }

    public void checkForUpdates(GiteeUpdateChecker checker) {
        if (state == UpdateState.CHECKING || state == UpdateState.DOWNLOADING || state == UpdateState.INSTALLING) {
            return;
        }

        state = UpdateState.CHECKING;
        checker.checkForUpdates().thenAccept(updateInfo -> {
            if (updateInfo != null && checker.isNewerVersionAvailable()) {
                currentUpdate = updateInfo;
                state = UpdateState.UPDATE_AVAILABLE;
                Simukraft.LOGGER.info("New version available: {}", updateInfo.tagName());
            } else {
                state = UpdateState.IDLE;
            }
        }).exceptionally(throwable -> {
            Simukraft.LOGGER.error("Failed to check for updates", throwable);
            state = UpdateState.IDLE;
            return null;
        });
    }

    public CompletableFuture<Boolean> downloadUpdate() {
        if (currentUpdate == null || !currentUpdate.hasDownloadUrl()) {
            return CompletableFuture.completedFuture(false);
        }

        if (!isDownloading.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(false);
        }

        state = UpdateState.DOWNLOADING;
        downloadProgress = 0.0f;
        downloadedBytes = 0L;
        downloadSpeed = 0L;
        lastUpdateTime = System.currentTimeMillis();
        lastDownloadedBytes = 0L;

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(currentUpdate.downloadUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", "Simukraft-UpdateManager/1.0");

                totalBytes = connection.getContentLengthLong();
                String fileName = extractFileName(currentUpdate.downloadUrl());

                Simukraft.LOGGER.info("Starting download: {} ({} bytes)", fileName, totalBytes);
                Simukraft.LOGGER.info("Download URL: {}", currentUpdate.downloadUrl());

                Path modsPath = getModsDirectory();
                Files.createDirectories(modsPath);
                Path downloadPath = modsPath.resolve(fileName + ".downloading");

                Simukraft.LOGGER.info("Download path: {}", downloadPath);

                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(downloadPath.toFile())) {

                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int bytesRead;
                    int logCounter = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        downloadedBytes = downloaded;

                        if (totalBytes > 0) {
                            downloadProgress = (float) downloaded / totalBytes;
                        }

                        // 计算下载速度（每500ms更新一次）
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime >= 500) {
                            long timeDiff = currentTime - lastUpdateTime;
                            long bytesDiff = downloaded - lastDownloadedBytes;
                            if (timeDiff > 0) {
                                downloadSpeed = (bytesDiff * 1000) / timeDiff; // bytes per second
                            }
                            lastUpdateTime = currentTime;
                            lastDownloadedBytes = downloaded;

                            // 每2秒输出一次日志
                            logCounter++;
                            if (logCounter % 4 == 0) {
                                Simukraft.LOGGER.info("Download progress: {}/{} bytes ({:.1f}%), Speed: {}/s",
                                    downloaded, totalBytes, downloadProgress * 100, formatSpeed(downloadSpeed));
                            }
                        }
                    }
                }

                connection.disconnect();
                Simukraft.LOGGER.info("Download completed: {} bytes", downloadedBytes);

                Path finalPath = modsPath.resolve(fileName);
                Files.move(downloadPath, finalPath);

                state = UpdateState.DOWNLOAD_COMPLETE;
                isDownloading.set(false);

                return true;

            } catch (Exception e) {
                Simukraft.LOGGER.error("Failed to download update", e);
                errorMessage = e.getMessage();
                state = UpdateState.ERROR;
                isDownloading.set(false);
                return false;
            }
        });
    }

    public boolean installUpdate() {
        if (state != UpdateState.DOWNLOAD_COMPLETE) {
            return false;
        }

        state = UpdateState.INSTALLING;

        try {
            Path modsPath = getModsDirectory();
            String currentModFileName = getCurrentModFileName();

            // 先处理新版本的文件
            String newFileName = extractFileName(currentUpdate.downloadUrl());
            Path newModPath = modsPath.resolve(newFileName);
            Path finalPath = modsPath.resolve(newFileName.replace(".jar", "-forge-1.20.1.jar"));

            if (Files.exists(newModPath)) {
                if (!finalPath.equals(newModPath)) {
                    Files.move(newModPath, finalPath);
                }
            }

            // 安装完成后立即删除旧版本
            if (currentModFileName != null) {
                Path oldModPath = modsPath.resolve(currentModFileName);
                if (Files.exists(oldModPath)) {
                    Files.delete(oldModPath);
                    Simukraft.LOGGER.info("Old mod file deleted: {}", oldModPath);
                }
                
                // 同时删除可能存在的 .old 备份文件
                Path backupPath = modsPath.resolve(currentModFileName + ".old");
                if (Files.exists(backupPath)) {
                    Files.delete(backupPath);
                    Simukraft.LOGGER.info("Old mod backup deleted: {}", backupPath);
                }
            }

            state = UpdateState.INSTALL_COMPLETE;
            return true;

        } catch (Exception e) {
            Simukraft.LOGGER.error("Failed to install update", e);
            errorMessage = e.getMessage();
            state = UpdateState.ERROR;
            return false;
        }
    }

    public void restartGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.stop();
        }
    }

    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "simukraft-update.jar";
        }

        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }

        return "simukraft-update.jar";
    }

    private Path getModsDirectory() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve(MODS_FOLDER);
    }

    private String getCurrentModFileName() {
        try {
            // 使用 ModList 直接获取当前运行的模组文件路径
            return ModList.get().getModContainerById(Simukraft.MOD_ID)
                    .map(modContainer -> modContainer.getModInfo().getOwningFile().getFile().getFileName())
                    .orElse(null);
        } catch (Exception e) {
            Simukraft.LOGGER.error("Failed to get current mod file name from ModList", e);
            return null;
        }
    }

    public UpdateState getState() {
        return state;
    }

    public void setState(UpdateState state) {
        this.state = state;
    }

    public UpdateInfo getCurrentUpdate() {
        return currentUpdate;
    }

    public void setCurrentUpdate(UpdateInfo update) {
        this.currentUpdate = update;
    }

    public float getDownloadProgress() {
        return downloadProgress;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getDownloadSpeed() {
        return downloadSpeed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void reset() {
        state = UpdateState.IDLE;
        currentUpdate = null;
        downloadProgress = 0.0f;
        downloadedBytes = 0L;
        totalBytes = 0L;
        downloadSpeed = 0L;
        errorMessage = "";
    }

    public boolean hasUpdateAvailable() {
        return state == UpdateState.UPDATE_AVAILABLE && currentUpdate != null;
    }

    public boolean isUpdateReady() {
        return state == UpdateState.INSTALL_COMPLETE;
    }

    /**
     * 格式化速度为可读字符串
     */
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.2f MB", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
}
