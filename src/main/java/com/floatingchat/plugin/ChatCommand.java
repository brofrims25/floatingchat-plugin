package com.floatingchat.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Menangani perintah /chat dan /ch (alias).
 *
 * Pemakaian normal (semua pemain dengan izin floatingchat.use):
 *   /chat <pesan>           -> floating chat muncul di dekat karakter pengirim sendiri.
 *
 * Pemakaian khusus admin (izin floatingchat.admin), bisa dari jarak jauh:
 *   /chat <nama_pemain> <pesan>  -> floating chat muncul di dekat pemain bernama itu.
 *   /chat all <pesan>            -> floating chat muncul di dekat SEMUA pemain sekaligus.
 *
 * Semua pemakaian di atas TIDAK pernah masuk ke chat global. Admin (izin
 * floatingchat.admin) akan menerima salinan pesan di chat box mereka untuk
 * pemantauan (lihat config.yml -> admin-monitor).
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

        if (!configManager.isEnabled()) {
            player.sendMessage("§cFloating chat sedang dinonaktifkan di server ini.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eGunakan: /" + label + " <pesan>");
            if (player.hasPermission("floatingchat.admin")) {
                player.sendMessage("§eKhusus admin: /" + label + " <nama_pemain> <pesan>  |  /" + label + " all <pesan>");
            }
            return true;
        }

        // ==== Perintah khusus admin: /chat <nama_pemain>/all <pesan> ====
        if (player.hasPermission("floatingchat.admin") && args.length >= 2) {
            String firstArg = args[0];

            if (firstArg.equalsIgnoreCase("all")) {
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleAdminBroadcastAll(player, message);
                return true;
            }

            Player target = Bukkit.getPlayerExact(firstArg);
            if (target != null) {
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleAdminRemote(player, target, message);
                return true;
            }
            // Kalau nama tidak ditemukan, lanjut dianggap sebagai pesan biasa (jatuh ke bawah).
        }

        // ==== Pemakaian normal: /chat <pesan> ====
        if (!player.hasPermission("floatingchat.use")) {
            player.sendMessage("§cKamu tidak punya izin untuk menggunakan floating chat.");
            return true;
        }

        String message = String.join(" ", args);
        floatingChatManager.showFloatingChat(player, message);
        sendAdminMonitor(player.getName(), message);

        return true;
    }

    /**
     * Admin mengirim pesan yang muncul di dekat SATU pemain tertentu, bisa dari jarak jauh.
     */
    private void handleAdminRemote(Player admin, Player target, String message) {
        boolean bypassCensor = admin.hasPermission("floatingchat.bypasscensor");
        floatingChatManager.showRemoteFloatingChat(target, admin, message, bypassCensor);
        sendAdminMonitor(admin.getName(), "-> " + target.getName() + ": " + message);
    }

    /**
     * Admin mengirim pesan yang muncul di dekat SEMUA pemain online sekaligus.
     */
    private void handleAdminBroadcastAll(Player admin, String message) {
        boolean bypassCensor = admin.hasPermission("floatingchat.bypasscensor");
        for (Player target : Bukkit.getOnlinePlayers()) {
            floatingChatManager.showRemoteFloatingChat(target, admin, message, bypassCensor);
        }
        sendAdminMonitor(admin.getName(), "-> ALL: " + message);
    }

    /**
     * Mengirim salinan pesan ke chat box semua pemain dengan izin floatingchat.admin,
     * memakai format redup/abu-abu di config.yml (admin-monitor.format) supaya tidak
     * terlalu mencolok dibanding chat global asli.
     */
    private void sendAdminMonitor(String senderLabel, String message) {
        if (!configManager.isAdminMonitorEnabled()) return;

        String formatted = configManager.colorize(
                configManager.getAdminMonitorFormat()
                        .replace("{player}", senderLabel)
                        .replace("{message}", message)
        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("floatingchat.admin")) {
                online.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }
}
