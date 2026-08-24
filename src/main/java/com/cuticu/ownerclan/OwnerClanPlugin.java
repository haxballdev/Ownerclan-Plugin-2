package com.cuticu.ownerclan;

import com.cuticu.ownerclan.command.ClanCommand;
import com.cuticu.ownerclan.command.TagCommand;
import com.cuticu.ownerclan.listener.ChatListener;
import com.cuticu.ownerclan.listener.DeathListener;
import com.cuticu.ownerclan.listener.JoinListener;
import com.cuticu.ownerclan.manager.ClanManager;
import com.cuticu.ownerclan.manager.TagManager;
import com.cuticu.ownerclan.manager.WebhookManager;
import com.cuticu.ownerclan.model.Clan;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OwnerClanPlugin extends JavaPlugin {

    private TagManager tagManager;
    private ClanManager clanManager;
    private WebhookManager webhookManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tagManager = new TagManager(this);
        this.clanManager = new ClanManager(this);
        this.webhookManager = new WebhookManager(this);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        TagCommand tagCommand = new TagCommand(this);
        getCommand("tag").setExecutor(tagCommand);
        getCommand("tag").setTabCompleter(tagCommand);

        ClanCommand clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        scheduleLeaderboard();

        getLogger().info("OwnerClanPlugin activado. Owner: " + getConfig().getString("owner-name"));
    }

    @Override
    public void onDisable() {
        if (tagManager != null) tagManager.save();
        if (clanManager != null) clanManager.save();
    }

    private void scheduleLeaderboard() {
        int minutes = getConfig().getInt("leaderboard-interval-minutes", 30);
        if (minutes <= 0) return;

        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            List<Clan> top = clanManager.topByKills(getConfig().getInt("leaderboard-size", 5));
            if (top.isEmpty()) return;

            StringBuilder sb = new StringBuilder("**\uD83C\uDFC6 Ranking de clanes por kills**\\n");
            int pos = 1;
            for (Clan clan : top) {
                sb.append(pos).append(". ").append(clan.getName())
                        .append(" - ").append(clan.getKills()).append(" kills\\n");
                pos++;
            }
            webhookManager.sendNews(sb.toString());
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public String getOwnerName() {
        return getConfig().getString("owner-name", "Cuticu098");
    }

    public boolean isOwner(String playerName) {
        return playerName != null && playerName.equalsIgnoreCase(getOwnerName());
    }
}
