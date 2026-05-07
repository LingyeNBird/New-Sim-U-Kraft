package com.xiaoliang.simukraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class ClientToastHUDOverlay implements IGuiOverlay {
    public static final ClientToastHUDOverlay INSTANCE = new ClientToastHUDOverlay();
    private static final long TOAST_DURATION_MS = 3000L;
    private static final long CLEANUP_INTERVAL_MS = 5000L;
    private static final int FADE_IN_TICKS = 12;
    private static final int HOLD_TICKS = 36;
    private static final int FADE_OUT_TICKS = 14;
    private static final int TITLE_RGB = 0xF4E8C9;
    private static final int DESC_RGB = 0xD8C8A6;
    private static final int TYPE_W1_RGB = 0xEAD7AE;
    private static final int TYPE_W2_RGB = 0xD6E3B5;
    private static final int TYPE_G1_RGB = 0xE6C68F;
    private static final int CITY_RGB = 0xF0DFC1;
    private static final int BAR_WIDTH = 2;
    private static final int BAR_HEIGHT = 14;
    private static final int BAR_GAP = 12;
    private static final int BAR_TEXT_PADDING = 8;
    private static final int BAR_TRAVEL = 18;
    private static final float TITLE_SCALE = 2.2F;
    private static final float DESCRIPTION_SCALE = 1.15F;

    private static final Map<UUID, ToastInfo> playerToasts = new ConcurrentHashMap<>();
    private static volatile long lastCleanupTime = 0L;

    private static final class ToastInfo {
        private String type;
        private long startTime;
        private long duration;
        private com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade;

        private ToastInfo(String type, long duration, com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade) {
            this.type = type;
            this.duration = duration;
            this.upgrade = upgrade;
            this.startTime = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }

        private void update(String type, long duration, com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade) {
            this.type = type;
            this.duration = duration;
            this.upgrade = upgrade;
            this.startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render(@Nonnull ForgeGui gui, @Nonnull GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        pruneExpiredToasts(false);
        if (mc.player == null || mc.font == null) {
            return;
        }
        ToastInfo toastInfo = playerToasts.get(mc.player.getUUID());
        if (toastInfo == null || toastInfo.isExpired()) {
            if (toastInfo != null) {
                playerToasts.remove(mc.player.getUUID());
            }
            return;
        }

        int totalTicks = FADE_IN_TICKS + HOLD_TICKS + FADE_OUT_TICKS;
        float elapsedTicks = ((System.currentTimeMillis() - toastInfo.startTime) / 50.0f) + clamp01(partialTick);
        float alpha = getAlpha(elapsedTicks, totalTicks);
        if (alpha <= 0.01f) {
            return;
        }

        Font font = mc.font;
        Component title = getTitle(toastInfo.type, toastInfo.upgrade);
        Component description = getDescription(toastInfo.type, toastInfo.upgrade);
        int rgb = getTypeColor(toastInfo.type);

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int anchorX = width / 2;
        int anchorY = Math.max(56, height / 5);

        List<FormattedCharSequence> titleLines = requireFormattedLines(font.split(title, Math.max(120, width / 4)));
        List<FormattedCharSequence> descriptionLines = requireFormattedLines(font.split(description, Math.max(180, width / 3)));
        if (descriptionLines.size() > 2) {
            descriptionLines = descriptionLines.subList(0, 2);
        }

        int titleLineHeight = Math.max(1, Math.round(font.lineHeight * TITLE_SCALE));
        int descLineHeight = Math.max(1, Math.round(font.lineHeight * DESCRIPTION_SCALE));
        int gap = 10;
        int blockHeight = titleLines.size() * titleLineHeight + gap + descriptionLines.size() * descLineHeight;
        int startY = anchorY - blockHeight / 2;

        int titleColor = ((int) (alpha * 255.0f) << 24) | TITLE_RGB;
        int descriptionColor = ((int) (alpha * 235.0f) << 24) | DESC_RGB;
        int barColor = ((int) (alpha * 215.0f) << 24) | rgb;
        int barShadow = ((int) (alpha * 96.0f) << 24) | 0x4F3928;

        drawCenteredScaledLines(guiGraphics, font, titleLines, anchorX, startY, TITLE_SCALE, titleColor, true);
        drawCenteredScaledLines(guiGraphics, font, descriptionLines, anchorX, startY + titleLines.size() * titleLineHeight + gap,
                DESCRIPTION_SCALE, descriptionColor, true);

        float inPhase = clamp01(elapsedTicks / FADE_IN_TICKS);
        float outPhase = clamp01((elapsedTicks - FADE_IN_TICKS - HOLD_TICKS) / FADE_OUT_TICKS);
        float leftOffset;
        float rightOffset;
        if (elapsedTicks < FADE_IN_TICKS) {
            float eased = easeOutCubic(inPhase);
            leftOffset = -BAR_TRAVEL * (1.0f - eased);
            rightOffset = BAR_TRAVEL * (1.0f - eased);
        } else if (elapsedTicks < FADE_IN_TICKS + HOLD_TICKS) {
            leftOffset = 0.0f;
            rightOffset = 0.0f;
        } else {
            float eased = easeInCubic(outPhase);
            leftOffset = BAR_TRAVEL * eased;
            rightOffset = -BAR_TRAVEL * eased;
        }

        int titleWidth = Math.max(1, Math.round(getMaxLineWidth(font, titleLines) * TITLE_SCALE));
        int baseBarY = startY + Math.max(0, titleLineHeight / 2 - BAR_HEIGHT / 2);
        int leftX = anchorX - titleWidth / 2 - BAR_TEXT_PADDING - BAR_GAP - BAR_WIDTH;
        int rightX = anchorX + titleWidth / 2 + BAR_TEXT_PADDING + BAR_GAP;
        int leftY = Math.round(baseBarY + leftOffset);
        int rightY = Math.round(baseBarY + rightOffset);
        guiGraphics.fill(leftX + 1, leftY + 1, leftX + BAR_WIDTH + 1, leftY + BAR_HEIGHT + 1, barShadow);
        guiGraphics.fill(rightX + 1, rightY + 1, rightX + BAR_WIDTH + 1, rightY + BAR_HEIGHT + 1, barShadow);
        guiGraphics.fill(leftX, leftY, leftX + BAR_WIDTH, leftY + BAR_HEIGHT, barColor);
        guiGraphics.fill(rightX, rightY, rightX + BAR_WIDTH, rightY + BAR_HEIGHT, barColor);
    }

    public static void showToast(String type, int upgradeLevel, UUID playerId) {
        String toastType = normalizeType(type);
        if (toastType == null) {
            return;
        }
        com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade =
                com.xiaoliang.simukraft.world.CityUpgradeManager.getInstance().getUpgrade(upgradeLevel);
        Minecraft mc = Objects.requireNonNull(Minecraft.getInstance());
        mc.getSoundManager().play(Objects.requireNonNull(SimpleSoundInstance.forUI(
                Objects.requireNonNull(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 1.0F, 1.0F)));
        ToastInfo toastInfo = playerToasts.get(playerId);
        if (toastInfo != null) {
            toastInfo.update(toastType, TOAST_DURATION_MS, upgrade);
        } else {
            playerToasts.put(playerId, new ToastInfo(toastType, TOAST_DURATION_MS, upgrade));
        }
        pruneExpiredToasts(false);
    }

    public static void showToast(String type, UUID playerId) {
        showToast(type, 0, playerId);
    }

    public static void clearToast(UUID playerId) {
        playerToasts.remove(playerId);
    }

    public static void clearAllToasts() {
        playerToasts.clear();
        lastCleanupTime = 0L;
    }

    public static void showCityName(String cityName, UUID playerId) {
        if (cityName == null || cityName.isBlank()) {
            return;
        }
        ToastInfo toastInfo = playerToasts.get(playerId);
        com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade fakeUpgrade = null;
        if (toastInfo != null) {
            toastInfo.update("city_name:" + cityName.trim(), TOAST_DURATION_MS, fakeUpgrade);
        } else {
            playerToasts.put(playerId, new ToastInfo("city_name:" + cityName.trim(), TOAST_DURATION_MS, fakeUpgrade));
        }
        pruneExpiredToasts(false);
    }

    private static Component getTitle(String type, com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade) {
        if (type != null && type.startsWith("city_name:")) {
            return Component.literal(type.substring("city_name:".length()));
        }
        if (upgrade == null || upgrade.name() == null || upgrade.name().isEmpty()) {
            return Component.literal("城市升级");
        }
        return Component.literal(upgrade.name());
    }

    private static Component getDescription(String type, com.xiaoliang.simukraft.world.CityUpgradeManager.CityUpgrade upgrade) {
        if (type != null && type.startsWith("city_name:")) {
            return Component.literal("");
        }
        if (upgrade == null || upgrade.description() == null || upgrade.description().isEmpty()) {
            return Component.literal("");
        }
        return Component.literal(upgrade.description());
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        if (type.startsWith("city_name:")) {
            return type;
        }
        return switch (type.toLowerCase()) {
            case "w1", "w2", "g1" -> type.toLowerCase();
            default -> null;
        };
    }

    private static int getTypeColor(String type) {
        if (type.startsWith("city_name:")) {
            return CITY_RGB;
        }
        return switch (type) {
            case "w2" -> TYPE_W2_RGB;
            case "g1" -> TYPE_G1_RGB;
            default -> TYPE_W1_RGB;
        };
    }

    private static float getAlpha(float elapsedTicks, int totalTicks) {
        if (elapsedTicks < FADE_IN_TICKS) {
            return easeOutCubic(clamp01(elapsedTicks / FADE_IN_TICKS));
        }
        if (elapsedTicks < FADE_IN_TICKS + HOLD_TICKS) {
            return 1.0f;
        }
        return 1.0f - easeInCubic(clamp01((elapsedTicks - FADE_IN_TICKS - HOLD_TICKS) / FADE_OUT_TICKS));
    }

    private static void pruneExpiredToasts(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupTime = now;
        playerToasts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static int getMaxLineWidth(Font font, List<FormattedCharSequence> lines) {
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return Math.min(value, 1.0f);
    }

    private static float easeOutCubic(float t) {
        float x = clamp01(t);
        float oneMinus = 1.0f - x;
        return 1.0f - oneMinus * oneMinus * oneMinus;
    }

    private static float easeInCubic(float t) {
        float x = clamp01(t);
        return x * x * x;
    }

    private static void drawCenteredScaledLines(@Nonnull GuiGraphics guiGraphics, @Nonnull Font font, @Nonnull List<FormattedCharSequence> lines,
                                                int centerX, int startY, float scale, int color, boolean shadow) {
        int lineHeight = Math.max(1, Math.round(font.lineHeight * scale));
        int currentY = startY;
        for (FormattedCharSequence line : lines) {
            int lineWidth = font.width(line);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, currentY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(font, line, -lineWidth / 2, 0, color, shadow);
            guiGraphics.pose().popPose();
            currentY += lineHeight;
        }
    }

    @Nonnull
    private static List<FormattedCharSequence> requireFormattedLines(List<FormattedCharSequence> lines) {
        return Objects.requireNonNull(lines);
    }
}
