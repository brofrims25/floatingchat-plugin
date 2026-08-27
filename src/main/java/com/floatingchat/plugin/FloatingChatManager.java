package com.floatingchat.plugin;

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
 * Membuat, memposisikan, dan menghapus TextDisplay yang berfungsi sebagai
 * "floating chat" di dekat karakter pemain.
 *
 * Mendukung 2 mode (diatur lewat config.yml -> follow-mode.enabled):
 *  - FOLLOW MODE (true)  : teks terus mengikuti pemain ke mana pun dia bergerak.
 *  - STATIC MODE (false) : teks "ditinggal" diam di koordinat tempat dia pertama
 *                          muncul, tidak ikut pindah walau pemainnya bergerak.
 *
 * Juga mendukung pesan jarak jauh dari admin (showRemoteFloatingChat), di mana
 * lokasi kemunculan (locationPlayer) bisa berbeda dari nama yang ditampilkan
 * (nameOwner) -- dipakai untuk perintah /chat <nama>/all yang khusus admin.
 */
public class FloatingChatManager {

    private final FloatingChatPlugin plugin;
    private final ConfigManager configManager;
    private final CensorManager censorManager;

    // --- FOLLOW MODE: satu display aktif per pemain (key = UUID pemain yang jadi acuan lokasi) ---
    private final Map<UUID, TextDisplay> followDisplays = new HashMap<>();
    private final Map<UUID, BukkitTask> followTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> followExpireTasks = new HashMap<>();

    // --- STATIC MODE: bisa banyak display sekaligus (key = UUID entity display itu sendiri) ---
    private final Map<UUID, TextDisplay> staticDisplays = new HashMap<>();
    private final Map<UUID, BukkitTask> staticExpireTasks = new HashMap<>();

    public FloatingChatManager(FloatingChatPlugin plugin, ConfigManager configManager, CensorManager censorManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.censorManager = censorManager;
    }

    /**
     * Dipakai untuk pemakaian normal: pemain mengetik /chat atau /ch untuk pesannya sendiri.
     */
    public void showFloatingChat(Player speaker, String rawMessage) {
        boolean bypassCensor = speaker.hasPermission("floatingchat.bypasscensor");
        showFloatingChatInternal(speaker, speaker, rawMessage, bypassCensor);
    }

    /**
     * Dipakai untuk perintah admin jarak jauh: /chat <nama> <pesan> atau /chat all <pesan>.
     *
     * @param locationPlayer pemain yang jadi acuan lokasi kemunculan (target)
     * @param nameOwner      pemain/pengirim yang namanya ditampilkan sebagai label (biasanya admin pengirim)
     * @param rawMessage     isi pesan
     * @param bypassCensor   true jika pesan tidak perlu disensor
     */
    public void showRemoteFloatingChat(Player locationPlayer, Player nameOwner, String rawMessage, boolean bypassCensor) {
        showFloatingChatInternal(locationPlayer, nameOwner, rawMessage, bypassCensor);
    }

    private void showFloatingChatInternal(Player locationPlayer, Player nameOwner, String rawMessage, boolean bypassCensor) {
        if (!configManager.isEnabled()) return;

        String message = bypassCensor ? rawMessage : censorManager.censor(rawMessage);
        String wrapped = TextWrapUtil.wrap(message, configManager.getMaxCharactersPerLine(), configManager.getMaxLines());
        final String finalText = buildDisplayText(nameOwner, wrapped);

        if (configManager.isFollowMode()) {
            showFollowingChat(locationPlayer, finalText);
        } else {
            showStaticChat(locationPlayer, finalText);
        }
    }

    // ===================================================================
    // FOLLOW MODE
    // ===================================================================

    private void showFollowingChat(Player locationPlayer, String finalText) {
        UUID uuid = locationPlayer.getUniqueId();

        TextDisplay existing = followDisplays.get(uuid);
        if (existing != null && !existing.isDead()) {
            if (configManager.isReplacePrevious()) {
                existing.text(legacyToComponent(finalText));
            } else {
                String combined = plainOf(existing) + "\n" + finalText;
                existing.text(legacyToComponent(TextWrapUtil.wrap(combined, configManager.getMaxCharactersPerLine(), configManager.getMaxLines() * 2)));
            }
            restartFollowExpireTimer(uuid);
            return;
        }

        Location spawnLoc = calculateLocation(locationPlayer);
        TextDisplay display = spawnDisplay(spawnLoc, finalText);

        followDisplays.put(uuid, display);
        startFollowTask(locationPlayer, display);
        restartFollowExpireTimer(uuid);
    }

    private void startFollowTask(Player locationPlayer, TextDisplay display) {
        UUID uuid = locationPlayer.getUniqueId();
        cancelTask(followTasks, uuid);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!locationPlayer.isOnline() || display.isDead()) {
                    cancel();
                    followTasks.remove(uuid);
                    return;
                }
                display.teleport(calculateLocation(locationPlayer));
            }
        }.runTaskTimer(plugin, 1L, 1L);

        followTasks.put(uuid, task);
    }

    private void restartFollowExpireTimer(UUID playerUuid) {
        cancelTask(followExpireTasks, playerUuid);

        long ticks = Math.max(1, configManager.getDurationSeconds()) * 20L;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                removeFollowDisplay(playerUuid);
            }
        }.runTaskLater(plugin, ticks);

        followExpireTasks.put(playerUuid, task);
    }

    private void removeFollowDisplay(UUID playerUuid) {
        TextDisplay display = followDisplays.remove(playerUuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        cancelTask(followTasks, playerUuid);
        cancelTask(followExpireTasks, playerUuid);
    }

    // ===================================================================
    // STATIC MODE (ditinggal diam di koordinat awal)
    // ===================================================================

    private void showStaticChat(Player locationPlayer, String finalText) {
        Location spawnLoc = calculateLocation(locationPlayer);
        TextDisplay display = spawnDisplay(spawnLoc, finalText);

        UUID entityUuid = display.getUniqueId();
        staticDisplays.put(entityUuid, display);
        restartStaticExpireTimer(entityUuid);
    }

    private void restartStaticExpireTimer(UUID entityUuid) {
        long ticks = Math.max(1, configManager.getDurationSeconds()) * 20L;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                removeStaticDisplay(entityUuid);
            }
        }.runTaskLater(plugin, ticks);

        staticExpireTasks.put(entityUuid, task);
    }

    private void removeStaticDisplay(UUID entityUuid) {
        TextDisplay display = staticDisplays.remove(entityUuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        cancelTask(staticExpireTasks, entityUuid);
    }

    // ===================================================================
    // PEMBUATAN ENTITY & UTILITAS BERSAMA
    // ===================================================================

    private TextDisplay spawnDisplay(Location spawnLoc, String finalText) {
        return spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
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
    }

    /**
     * Menyusun teks akhir yang ditampilkan, termasuk menambahkan nama pemain
     * (jika diaktifkan di config) hanya pada baris pertama.
     */
    private String buildDisplayText(Player nameOwner, String wrapped) {
        if (!configManager.isShowPlayerName()) {
            return configManager.colorize(configManager.getMessageColor()) + wrapped;
        }

        String namePart = configManager.colorize(configManager.getNameColor()) + nameOwner.getName() + configManager.colorize("&f") + ": ";
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
     * Menghitung posisi tampil berdasarkan konfigurasi (ABOVE_HEAD, FRONT, BACK, SIDE)
     * relatif terhadap pemain yang diberikan.
     */
    private Location calculateLocation(Player locationPlayer) {
        Location base = locationPlayer.getEyeLocation();
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

    private void cancelTask(Map<UUID, BukkitTask> map, UUID uuid) {
        BukkitTask task = map.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Membersihkan semua teks melayang (follow maupun static) -- dipanggil saat plugin dimatikan.
     */
    public void removeAllDisplays() {
        for (UUID uuid : new HashMap<>(followDisplays).keySet()) {
            removeFollowDisplay(uuid);
        }
        for (UUID uuid : new HashMap<>(staticDisplays).keySet()) {
            removeStaticDisplay(uuid);
        }
    }
}
