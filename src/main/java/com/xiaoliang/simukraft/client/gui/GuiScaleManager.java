package com.xiaoliang.simukraft.client.gui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * GUI缩放管理类
 * 统一管理界面缩放，提供简洁的API
 */
public class GuiScaleManager {

    private static float originalScale = -1;

    /**
     * 从options.txt读取原始缩放值
     */
    public static int readOriginalScale() {
        try {
            File optionsFile = new File(Minecraft.getInstance().gameDirectory, "options.txt");
            if (optionsFile.exists()) {
                String content = new String(Files.readAllBytes(optionsFile.toPath()));
                for (String line : content.split("\n")) {
                    if (line.startsWith("guiScale:")) {
                        return Integer.parseInt(line.substring("guiScale:".length()).trim());
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            // 读取失败返回默认值
        }
        return 2;
    }

    /**
     * 应用3x缩放，保存原始值
     */
    public static void apply3x() {
        if (originalScale < 0) {
            originalScale = readOriginalScale();
        }
        setScale(3);
    }

    /**
     * 恢复原始缩放
     */
    public static void restore() {
        if (originalScale > 0) {
            setScale(Math.round(originalScale));
            originalScale = -1;
        }
    }

    /**
     * 设置指定缩放值
     */
    public static void setScale(int scale) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        mc.options.guiScale().set(scale);
        window.setGuiScale(scale);
    }
}
