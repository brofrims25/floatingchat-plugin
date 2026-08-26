package com.floatingchat.plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final FloatingChatPlugin plugin;
    private final ConfigManager configManager;
    private final FloatingChatManager floatingChatManager;

    public ChatListener(FloatingChatPlugin plugin, ConfigManager configManager, FloatingChatManager floatingChatManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.floatingChatManager = floatingChatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        if (!configManager.isEnabled()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("floatingchat.use")) return;

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (!configManager.isKeepNormalChat()) {
            event.setCancelled(true);
        }

        // Jalankan di main thread karena AsyncChatEvent bisa dipanggil di luar main thread,
        // sedangkan spawn/teleport entity wajib dilakukan di main thread server.
        plugin.getServer().getScheduler().runTask(plugin, () ->
                floatingChatManager.showFloatingChat(player, plainMessage));
    }
}
