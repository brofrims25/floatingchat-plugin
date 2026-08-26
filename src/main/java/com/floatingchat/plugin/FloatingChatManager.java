package com.floatingchat.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Membuat, memposisikan (mengikuti pemain tiap tick), dan menghapus TextDisplay
 * yang berfungsi sebagai "floating chat" di dekat karakter pemain.
 *
 * TextDisplay adalah entity bawaan Minecraft (sejak 1.19.4) yang dirender
 * untuk semua pemain di sekitarnya secara native, termasuk pemain Bedrock
 * yang terhubung lewat Geyser (karena Geyser meneruskan entity display ini
 * sebagai entity teks biasa ke client Bedrock).
 */
public class FloatingChatManager {

    private final FloatingChatPlugin plugin;
    private final ConfigManager configManager;
    private final CensorManager censorManager;

    private final Map<UUID, TextDisplay> activeDisplays = new HashMap<>();
    private final Map<UUID, BukkitTask> followTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> expireTasks = new HashMap<>();

    public FloatingChatManager(FloatingChatPlugin plugin, ConfigManager configManager, CensorManager censorManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.censorManager = censorManager;
    }

    /**
     * Menampilkan pesan chat pemain sebagai floating text di dekat karakternya.
     */
    public void showFloatingChat(Player player, String rawMessage) {
        if (!configManager.isEnabled()) return;

        boolean bypassCensor = player.hasPermission("floatingchat.bypasscensor");
        String message = bypassCensor ? rawMessage : censorManager.censor(rawMessage);

        String wrapped = TextWrapUtil.wrap(message, configManager.getMaxCharactersPerLine(), configManager.getMaxLines());

        final String finalText = buildDisplayText(player, wrapped);

        UUID uuid = player.getUniqueId();

        // Jika sudah ada floating chat aktif untuk pemain ini
        TextDisplay existing = activeDisplays.get(uuid);
        if (existing != null && !existing.isDead()) {
            if (configManager.isReplacePrevious()) {
                existing.text(net.kyori.adventure.text.Component.text(""));
                existing.text(legacyToComponent(finalText));
                restartExpireTimer(uuid, existing);
                return;
            } else {
                // Tumpuk: gabungkan teks lama + baru (dibatasi max-lines lewat wrap ulang)
                String combined = plainOf(existing) + "\n" + finalText;
                existing.text(legacyToComponent(TextWrapUtil.wrap(combined, configManager.getMaxCharactersPerLine(), configManager.getMaxLines() * 2)));
                restartExpireTimer(uuid, existing);
                return;
            }
        }

        // Buat TextDisplay baru
        Location spawnLoc = calculateLocation(player);
        TextDisplay display = player.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
            td.text(legacyToComponent(finalText));
            td.setBillboard(configManager.isAlwaysFaceViewer() ? Display.Billboard.CENTER : Display.Billboard.FIXED);
            td.setAlignment(configManager.toBukkitAlignment());
            td.setSeeThrough(false);
            td.setShadowed(false);
            td.setDefaultBackground(false);
            if (configManager.isBackgroundEnabled()) {
                td.setBackgroundColor(parseArgbColor(configManager.getBackgroundColor()));
            } else {
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }
            td.setLineWidth(500);
            td.setViewRange(Math.max(1, configManager.getViewDistance()) / 48f);
            td.setPersistent(false);
            td.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.Quaternionf(0, 0, 0, 1),
                    new org.joml.Vector3f(configManager.getTextScale(), configManager.getTextScale(), configManager.getTextScale()),
                    new org.joml.Quaternionf(0, 0, 0, 1)
            ));
        });

        activeDisplays.put(uuid, display);
        startFollowTask(player, display);
        restartExpireTimer(uuid, display);
    }

    /**
     * Menyusun teks akhir yang ditampilkan, termasuk menambahkan nama pemain
     * (jika diaktifkan di config) hanya pada baris pertama.
     */
    private String buildDisplayText(Player player, String wrapped) {
        if (!configManager.isShowPlayerName()) {
            return configManager.colorize(configManager.getMessageColor()) + wrapped;
        }

        String namePart = configManager.colorize(configManager.getNameColor()) + player.getName() + configManager.colorize("&f") + ": ";
        String[] parts = wrapped.split("\n", 2);
        if (parts.length == 2) {
            return namePart + configManager.colorize(configManager.getMessageColor()) + parts[0] + "\n" + configManager.colorize(configManager.getMessageColor()) + parts[1];
        }
        return namePart + configManager.colorize(configManager.getMessageColor()) + wrapped;
    }

    private String plainOf(TextDisplay display) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(display.text());
    }

    private net.kyori.adventure.text.Component legacyToComponent(String legacyText) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }

    private Color parseArgbColor(String hex) {
        try {
            long argb = Long.parseLong(hex, 16);
            int a = (int) ((argb >> 24) & 0xFF);
            int r = (int) ((argb >> 16) & 0xFF);
            int g = (int) ((argb >> 8) & 0xFF);
            int b = (int) (argb & 0xFF);
            return Color.fromARGB(a, r, g, b);
        } catch (NumberFormatException ex) {
            return Color.fromARGB(64, 0, 0, 0);
        }
    }

    /**
     * Menghitung posisi tampil berdasarkan konfigurasi (ABOVE_HEAD, FRONT, BACK, SIDE).
     */
    private Location calculateLocation(Player player) {
        Location base = player.getEyeLocation();
        Vector direction = base.getDirection().setY(0).normalize();

        switch (configManager.getPosition()) {
            case FRONT: {
                Vector offset = direction.clone().multiply(configManager.getFrontBackDistance());
                return base.clone().add(offset).add(0, configManager.getHeightOffset(), 0);
            }
            case BACK: {
                Vector offset = direction.clone().multiply(-configManager.getFrontBackDistance());
                return base.clone().add(offset).add(0, configManager.getHeightOffset(), 0);
            }
            case SIDE: {
                Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                if (configManager.getSide() == ConfigManager.Side.LEFT) {
                    right.multiply(-1);
                }
                Vector offset = right.multiply(configManager.getSideDistance());
                return base.clone().add(offset).add(0, configManager.getHeightOffset(), 0);
            }
            case ABOVE_HEAD:
            default: {
                return base.clone().add(0, configManager.getHeightOffset(), 0);
            }
        }
    }

    private void startFollowTask(Player player, TextDisplay display) {
        UUID uuid = player.getUniqueId();
        cancelTask(followTasks, uuid);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || display.isDead()) {
                    cancel();
                    followTasks.remove(uuid);
                    return;
                }
                Location loc = calculateLocation(player);
                display.teleport(loc);
            }
        }.runTaskTimer(plugin, 1L, 1L);

        followTasks.put(uuid, task);
    }

    private void restartExpireTimer(UUID uuid, TextDisplay display) {
        cancelTask(expireTasks, uuid);

        long ticks = Math.max(1, configManager.getDurationSeconds()) * 20L;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                removeDisplay(uuid);
            }
        }.runTaskLater(plugin, ticks);

        expireTasks.put(uuid, task);
    }

    public void removeDisplay(UUID uuid) {
        TextDisplay display = activeDisplays.remove(uuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        cancelTask(followTasks, uuid);
        cancelTask(expireTasks, uuid);
    }

    public void removeAllDisplays() {
        for (UUID uuid : new HashMap<>(activeDisplays).keySet()) {
            removeDisplay(uuid);
        }
    }

    private void cancelTask(Map<UUID, BukkitTask> map, UUID uuid) {
        BukkitTask task = map.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
