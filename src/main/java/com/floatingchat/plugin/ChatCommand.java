package com.floatingchat.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Menangani perintah /chat dan /ch (alias).
 * Pesan yang dikirim lewat perintah ini HANYA tampil sebagai floating chat
 * di dekat karakter pengirim (untuk pemain yang berada dekat secara fisik),
 * dan TIDAK dikirim ke chat global.
 *
 * Jika admin-monitor diaktifkan di config.yml, pesan (versi asli/belum disensor)
 * akan ikut dikirim ke chat box semua pemain yang memiliki izin
 * "floatingchat.admin", supaya admin bisa memantau tanpa harus dekat secara fisik.
 * Pemain biasa tanpa izin tersebut tidak akan menerima salinan ini di chat box.
 */
public class ChatCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final FloatingChatManager floatingChatManager;

    public ChatCommand(ConfigManager configManager, FloatingChatManager floatingChatManager) {
        this.configManager = configManager;
        this.floatingChatManager = floatingChatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPerintah ini hanya bisa dipakai in-game oleh pemain.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("floatingchat.use")) {
            player.sendMessage("§cKamu tidak punya izin untuk menggunakan floating chat.");
            return true;
        }

        if (!configManager.isEnabled()) {
            player.sendMessage("§cFloating chat sedang dinonaktifkan di server ini.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eGunakan: /" + label + " <pesan>");
            return true;
        }

        String message = String.join(" ", args);

        // Tampilkan floating chat (censor otomatis diterapkan di dalam FloatingChatManager)
        floatingChatManager.showFloatingChat(player, message);

        // Kirim salinan pesan asli (belum disensor) ke semua admin untuk pemantauan
        if (configManager.isAdminMonitorEnabled()) {
            String formatted = configManager.colorize(
                    configManager.getAdminMonitorFormat()
                            .replace("{player}", player.getName())
                            .replace("{message}", message)
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("floatingchat.admin")) {
                    online.sendMessage(formatted);
                }
            }
            Bukkit.getConsoleSender().sendMessage(formatted);
        }

        return true;
    }
}
