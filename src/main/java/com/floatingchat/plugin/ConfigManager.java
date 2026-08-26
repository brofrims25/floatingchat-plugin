package com.floatingchat.plugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.TextDisplay;

import java.util.List;

/**
 * Membaca dan menyediakan semua nilai dari config.yml dengan tipe yang sudah siap pakai.
 */
public class ConfigManager {

    public enum Position { ABOVE_HEAD, FRONT, BACK, SIDE }
    public enum Side { LEFT, RIGHT }
    public enum Alignment { LEFT, CENTER, RIGHT }

    private final FloatingChatPlugin plugin;

    private boolean enabled;
    private boolean keepNormalChat;

    private Position position;
    private Side side;
    private double heightOffset;
    private double frontBackDistance;
    private double sideDistance;
    private float textScale;
    private boolean alwaysFaceViewer;
    private int viewDistance;
    private boolean backgroundEnabled;
    private String backgroundColor;
    private boolean showPlayerName;
    private String nameColor;
    private String messageColor;

    private int durationSeconds;
    private boolean replacePrevious;

    private int maxCharactersPerLine;
    private int maxLines;
    private Alignment alignment;

    private boolean bedrockCompatibility;
    private boolean debug;

    public ConfigManager(FloatingChatPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        this.enabled = cfg.getBoolean("enabled", true);
        this.keepNormalChat = cfg.getBoolean("keep-normal-chat", true);

        this.position = parseEnum(Position.class, cfg.getString("display.position", "ABOVE_HEAD"), Position.ABOVE_HEAD);
        this.side = parseEnum(Side.class, cfg.getString("display.side", "RIGHT"), Side.RIGHT);
        this.heightOffset = cfg.getDouble("display.height-offset", 0.4);
        this.frontBackDistance = cfg.getDouble("display.front-back-distance", 0.8);
        this.sideDistance = cfg.getDouble("display.side-distance", 0.7);
        this.textScale = (float) cfg.getDouble("display.text-scale", 1.0);
        this.alwaysFaceViewer = cfg.getBoolean("display.always-face-viewer", true);
        this.viewDistance = cfg.getInt("display.view-distance", 48);
        this.backgroundEnabled = cfg.getBoolean("display.background.enabled", true);
        this.backgroundColor = cfg.getString("display.background.color", "40000000");
        this.showPlayerName = cfg.getBoolean("display.show-player-name", true);
        this.nameColor = cfg.getString("display.name-color", "&e");
        this.messageColor = cfg.getString("display.message-color", "&f");

        this.durationSeconds = cfg.getInt("duration.seconds", 6);
        this.replacePrevious = cfg.getBoolean("duration.replace-previous", true);

        this.maxCharactersPerLine = cfg.getInt("text-wrap.max-characters-per-line", 40);
        this.maxLines = cfg.getInt("text-wrap.max-lines", 4);
        this.alignment = parseEnum(Alignment.class, cfg.getString("text-wrap.alignment", "CENTER"), Alignment.CENTER);

        this.bedrockCompatibility = cfg.getBoolean("bedrock-compatibility.enabled", true);
        this.debug = cfg.getBoolean("misc.debug", false);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> clazz, String value, T fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(clazz, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Nilai konfigurasi tidak dikenal: '" + value + "', memakai default " + fallback);
            return fallback;
        }
    }

    public TextDisplay.TextAlignment toBukkitAlignment() {
        switch (alignment) {
            case LEFT: return TextDisplay.TextAlignment.LEFT;
            case RIGHT: return TextDisplay.TextAlignment.RIGHT;
            default: return TextDisplay.TextAlignment.CENTER;
        }
    }

    public String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public List<String> getCensorWords() {
        return plugin.getConfig().getStringList("censor.words");
    }

    public boolean isCensorEnabled() { return plugin.getConfig().getBoolean("censor.enabled", true); }
    public String getCensorReplacement() { return plugin.getConfig().getString("censor.replacement", "AUTO_STARS"); }
    public boolean isCensorIgnoreCase() { return plugin.getConfig().getBoolean("censor.ignore-case", true); }
    public boolean isCensorDetectBypassTricks() { return plugin.getConfig().getBoolean("censor.detect-bypass-tricks", true); }

    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isKeepNormalChat() { return keepNormalChat; }
    public Position getPosition() { return position; }
    public Side getSide() { return side; }
    public double getHeightOffset() { return heightOffset; }
    public double getFrontBackDistance() { return frontBackDistance; }
    public double getSideDistance() { return sideDistance; }
    public float getTextScale() { return textScale; }
    public boolean isAlwaysFaceViewer() { return alwaysFaceViewer; }
    public int getViewDistance() { return viewDistance; }
    public boolean isBackgroundEnabled() { return backgroundEnabled; }
    public String getBackgroundColor() { return backgroundColor; }
    public boolean isShowPlayerName() { return showPlayerName; }
    public String getNameColor() { return nameColor; }
    public String getMessageColor() { return messageColor; }
    public int getDurationSeconds() { return durationSeconds; }
    public boolean isReplacePrevious() { return replacePrevious; }
    public int getMaxCharactersPerLine() { return maxCharactersPerLine; }
    public int getMaxLines() { return maxLines; }
    public Alignment getAlignment() { return alignment; }
    public boolean isBedrockCompatibility() { return bedrockCompatibility; }
    public boolean isDebug() { return debug; }
}
