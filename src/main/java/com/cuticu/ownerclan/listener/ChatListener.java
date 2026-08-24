package com.cuticu.ownerclan.listener;

import com.cuticu.ownerclan.OwnerClanPlugin;
import com.cuticu.ownerclan.model.Clan;
import com.cuticu.ownerclan.util.ColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Rewrites how chat messages are displayed. Priority order for the prefix shown
 * before a player's name:
 *   1. Owner tag (from config.yml), if this player is the configured owner.
 *   2. A personal tag set with /tag set (rainbow or colored).
 *   3. Their clan's tag, colored with the clan's color.
 *   4. Nothing.
 */
public class ChatListener implements Listener {

    private final OwnerClanPlugin plugin;

    public ChatListener(OwnerClanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Component prefix;
        Component nameComponent = Component.text(player.getName());

        if (plugin.isOwner(player.getName())) {
            String text = plugin.getConfig().getString("owner-tag.text", "[OWNER]");
            String color = plugin.getConfig().getString("owner-tag.color", "rainbow");
            prefix = ColorUtil.render(text, color).append(Component.text(" "));
            nameComponent = ColorUtil.isRainbow(color)
                    ? ColorUtil.rainbow(player.getName())
                    : nameComponent.color(ColorUtil.parseColor(color));

        } else if (plugin.getTagManager().hasTag(uuid)) {
            String text = plugin.getTagManager().getTagText(uuid);
            String color = plugin.getTagManager().getTagColor(uuid);
            prefix = ColorUtil.render(text, color).append(Component.text(" "));
            nameComponent = nameComponent.color(NamedTextColor.WHITE);

        } else {
            Clan clan = plugin.getClanManager().getByMember(uuid);
            if (clan != null) {
                prefix = ColorUtil.render(clan.getTagText(), clan.getTagColor()).append(Component.text(" "));
                nameComponent = nameComponent.color(ColorUtil.parseColor(clan.getTagColor()));
            } else {
                prefix = Component.empty();
                nameComponent = nameComponent.color(NamedTextColor.GRAY);
            }
        }

        Component finalPrefix = prefix;
        Component finalName = nameComponent;

        event.renderer((source, sourceDisplayName, message, viewer) ->
                finalPrefix
                        .append(finalName)
                        .append(Component.text(": ", NamedTextColor.WHITE))
                        .append(message));
    }
}
