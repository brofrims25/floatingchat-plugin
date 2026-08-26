package com.floatingchat.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class FloatingChatPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private CensorManager censorManager;
    private FloatingChatManager floatingChatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.censorManager = new CensorManager(configManager);
        this.floatingChatManager = new FloatingChatManager(this, configManager, censorManager);

        // Perintah /chat dan /ch (alias) untuk memicu floating chat (bukan chat global)
        getCommand("chat").setExecutor(new ChatCommand(configManager, floatingChatManager));

        getLogger().info("FloatingChat aktif! Ketik /chat <pesan> atau /ch <pesan> untuk floating chat.");
    }

    @Override
    public void onDisable() {
        if (floatingChatManager != null) {
            floatingChatManager.removeAllDisplays();
        }
        getLogger().info("FloatingChat dinonaktifkan, semua teks melayang dibersihkan.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("floatingchat.admin")) {
                sender.sendMessage("§cKamu tidak punya izin untuk melakukan ini.");
                return true;
            }
            reloadConfig();
            configManager.reload();
            censorManager.reload();
            sender.sendMessage("§a[FloatingChat] Konfigurasi berhasil dimuat ulang.");
            return true;
        }

        sender.sendMessage("§e[FloatingChat] Gunakan: /floatingchat reload");
        return true;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CensorManager getCensorManager() {
        return censorManager;
    }

    public FloatingChatManager getFloatingChatManager() {
        return floatingChatManager;
    }
}
