package com.cuticu.ownerclan.listener;

import com.cuticu.ownerclan.OwnerClanPlugin;
import com.cuticu.ownerclan.model.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final OwnerClanPlugin plugin;

    public DeathListener(OwnerClanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Clan clan = plugin.getClanManager().getByMember(killer.getUniqueId());
        if (clan == null) return;

        clan.addKill();
        plugin.getClanManager().save();
    }
}
