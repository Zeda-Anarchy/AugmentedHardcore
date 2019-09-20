package com.backtobedrock.LiteDeathBan;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LiteDeathBanCRUD {
    
    private final Logger log = Bukkit.getLogger();
    private final LiteDeathBan plugin;
    
    private File file = null;
    private FileConfiguration configuration = null;
    private OfflinePlayer player = null;
    private int lives;
    private int totalDeathBans;
    private String lastBan;
    private LocalDateTime lastRevive;
    
    public LiteDeathBanCRUD(OfflinePlayer player, LiteDeathBan plugin) {
        this.plugin = plugin;
        this.player = player;
        this.lives = this.getConfig().getInt("lives", 1);
        this.totalDeathBans = this.getConfig().getInt("totalDeathBans", 0);
        this.lastBan = this.getConfig().getString("lastBan", "never");
        this.lastRevive = LocalDateTime.parse(this.getConfig().getString("lastRevive", this.plugin.getLDBConfig().isReviveOptionOnFirstJoin() ? LocalDateTime.MIN.toString() : LocalDateTime.now().toString()));
    }
    
    public FileConfiguration getConfig() {
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(getFile());
            return configuration;
        }
        return configuration;
    }
    
    public void saveConfig() {
        try {
            configuration.save(this.getFile());
        } catch (IOException e) {
            this.log.log(Level.SEVERE, "Cannot save to {0}", file.getName());
        }
    }
    
    public void setNewStart() {
        FileConfiguration conf = this.getConfig();
        conf.set("uuid", player.getUniqueId().toString());
        conf.set("playername", player.getName());
        conf.set("lives", this.lives);
        conf.set("totalDeathBans", this.totalDeathBans);
        conf.set("lastBan", this.lastBan);
        conf.set("lastRevive", this.plugin.getLDBConfig().isReviveOptionOnFirstJoin() ? LocalDateTime.MIN.toString() : LocalDateTime.now().toString());
        this.saveConfig();
    }
    
    public void setLives(int amount, boolean save) {
        FileConfiguration conf = this.getConfig();
        conf.set("uuid", player.getUniqueId().toString());
        conf.set("playername", player.getName());
        conf.set("lives", amount);
        this.lives = amount;
        if (this.plugin.getLDBConfig().isShowLivesInTabMenu() && this.player.isOnline()) {
            this.player.getPlayer().setPlayerListFooter(this.plugin.getMessages().getOnLivesLeftInTabMenu(this.player.getName(), this.lives));
        }
        if (save) {
            this.saveConfig();
        }
    }
    
    public int getLives() {
        return this.lives;
    }
    
    public void setTotalDeathBans(int amount, boolean save) {
        FileConfiguration conf = this.getConfig();
        conf.set("uuid", player.getUniqueId().toString());
        conf.set("playername", player.getName());
        conf.set("totalDeathBans", amount);
        this.totalDeathBans = amount;
        if (save) {
            this.saveConfig();
        }
    }
    
    public int getTotalDeathBans() {
        return this.totalDeathBans;
    }
    
    public void setLastBanDate(LocalDateTime date, boolean save) {
        FileConfiguration conf = this.getConfig();
        conf.set("uuid", player.getUniqueId().toString());
        conf.set("playername", player.getName());
        conf.set("lastBanDate", this.plugin.getLDBConfig().getSaveDateFormat().format(date));
        this.lastBan = this.plugin.getLDBConfig().getSaveDateFormat().format(date);
        if (save) {
            this.saveConfig();
        }
    }
    
    public String getLastBanDate() {
        return this.lastBan;
    }
    
    public void setLastRevive(LocalDateTime date, boolean save) {
        FileConfiguration conf = this.getConfig();
        conf.set("uuid", player.getUniqueId().toString());
        conf.set("playername", player.getName());
        conf.set("lastRevive", date.toString());
        this.lastRevive = date;
        if (save) {
            this.saveConfig();
        }
    }
    
    public LocalDateTime getLastRevive() {
        return this.lastRevive;
    }
    
    private File getFile() {
        if (file == null) {
            this.file = new File(this.plugin.getDataFolder() + "/userdata/" + player.getUniqueId().toString() + ".yml");
            if (!this.file.exists()) {
                try {
                    if (this.file.createNewFile()) {
                        this.log.log(Level.INFO, "[LiteDeathBan] File for player {0} has been created", player.getName());
                    }
                } catch (IOException e) {
                    this.log.log(Level.SEVERE, "[LiteDeathBan] Cannot create data for {0}", player.getName());
                }
            }
            return file;
        }
        return file;
    }
    
    public static boolean doesPlayerDataExists(String id, LiteDeathBan plugin) {
        File file = new File(plugin.getDataFolder() + id + ".yml");
        return file.exists();
    }
    
    public void reloadConfig() {
        YamlConfiguration.loadConfiguration(file);
    }
}
