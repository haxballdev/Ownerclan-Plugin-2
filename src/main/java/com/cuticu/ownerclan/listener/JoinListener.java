package com.cuticu.ownerclan.listener;

import com.cuticu.ownerclan.OwnerClanPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Automatically grants operator status (=> access to every vanilla and plugin
 * command) to the account configured as "owner-name" in config.yml, every time
 * that account joins. Nobody else is affected.
 */
public class JoinListener implements Listener {

    private final OwnerClanPlugin plugin;

    public JoinListener(OwnerClanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isOwner(player.getName()) && !player.isOp()) {
            player.setOp(true);
            player.sendMessage("§dAcceso total de owner concedido.");
            plugin.getWebhookManager().sendLog("\uD83D\uDD11 " + player.getName() + " entro al servidor y recibio acceso total de owner.");
        }
    }
}
