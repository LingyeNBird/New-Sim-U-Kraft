package com.xiaoliang.simukraft.client.update;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 更新处理器
 * 处理游戏启动时的版本检查和主界面更新按钮
 */
@OnlyIn(Dist.CLIENT)
public class UpdateHandler {

    private static UpdateHandler INSTANCE;

    private final GiteeUpdateChecker updateChecker;
    private boolean hasCheckedOnStartup = false;
    private boolean updateAvailable = false;

    // Gitee 仓库配置
    private static final String GITEE_OWNER = "Kafeiqwq";             // Gitee用户名
    private static final String GITEE_REPO = "new-sim-version-hub";   // 仓库名

    private UpdateHandler() {
        this.updateChecker = new GiteeUpdateChecker(GITEE_OWNER, GITEE_REPO, Simukraft.getVersion());
    }

    public static UpdateHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UpdateHandler();
        }
        return INSTANCE;
    }

    /**
     * 游戏启动时检查更新
     */
    public void checkForUpdatesOnStartup() {
        if (hasCheckedOnStartup) {
            return;
        }

        hasCheckedOnStartup = true;

        // 异步检查更新
        updateChecker.checkForUpdates().thenAccept(updateInfo -> {
            Minecraft.getInstance().execute(() -> {
                if (updateInfo != null && updateChecker.isNewerVersionAvailable()) {
                    updateAvailable = true;
                    UpdateManager.getInstance().setCurrentUpdate(updateInfo);
                    UpdateManager.getInstance().setState(UpdateManager.UpdateState.UPDATE_AVAILABLE);
                    Simukraft.LOGGER.info("New version available: {}", updateInfo.tagName());
                }
            });
        }).exceptionally(throwable -> {
            Simukraft.LOGGER.error("Update check failed", throwable);
            return null;
        });
    }

    /**
     * 手动检查更新
     */
    public void checkForUpdates() {
        updateChecker.checkForUpdates().thenAccept(updateInfo -> {
            Minecraft.getInstance().execute(() -> {
                if (updateInfo != null && updateChecker.isNewerVersionAvailable()) {
                    updateAvailable = true;
                    UpdateManager.getInstance().setCurrentUpdate(updateInfo);
                    UpdateManager.getInstance().setState(UpdateManager.UpdateState.UPDATE_AVAILABLE);
                } else {
                    updateAvailable = false;
                    UpdateManager.getInstance().reset();
                }
            });
        });
    }

    /**
     * 屏幕初始化事件 - 用于在主菜单添加更新按钮
     */
    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            // 启动时检查更新
            if (!hasCheckedOnStartup) {
                checkForUpdatesOnStartup();
            }
        }
    }

    public GiteeUpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void clearUpdateFlag() {
        updateAvailable = false;
    }

    /**
     * 设置Gitee仓库信息
     */
    public static void setRepository(String owner, String repo) {
        if (INSTANCE != null) {
            // 重新创建checker
            INSTANCE = new UpdateHandler() {
                {
                    // 使用新的仓库信息
                }
            };
        }
    }
}
