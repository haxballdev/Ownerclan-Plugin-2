package com.cuticu.ownerclan.command;

import com.cuticu.ownerclan.OwnerClanPlugin;
import com.cuticu.ownerclan.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /tag set <player> <color> <texto...>
 * /tag remove <player>
 * /tag list
 *
 * Only the owner or someone with ownerclan.tag.set can tag other players.
 */
public class TagCommand implements CommandExecutor, TabCompleter {

    private static final List<String> COLORS = Arrays.asList(
            "white", "black", "red", "gold", "yellow", "green", "aqua", "blue",
            "light_purple", "dark_purple", "gray", "rainbow");

    private final OwnerClanPlugin plugin;

    public TagCommand(OwnerClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean allowed = plugin.isOwner(sender.getName()) || sender.hasPermission("ownerclan.tag.set");
        if (!allowed) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUso: /tag set <jugador> <color|rainbow> <texto> §7| /tag remove <jugador> §7| /tag list");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> sender.sendMessage("§eUso: /tag set <jugador> <color|rainbow> <texto> §7| /tag remove <jugador> §7| /tag list");
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§eUso: /tag set <jugador> <color|rainbow> <texto...>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cEse jugador nunca ha entrado al servidor.");
            return;
        }
        String color = args[2];
        if (!ColorUtil.isValidColorArg(color)) {
            sender.sendMessage("§cColor invalido. Usa un color con nombre, un hex (#RRGGBB) o 'rainbow'.");
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        plugin.getTagManager().setTag(target.getUniqueId(), text, color);
        sender.sendMessage(Component.text("§aTag actualizado: ").append(ColorUtil.render(text, color))
                .append(Component.text(" §apara " + target.getName())));

        plugin.getWebhookManager().sendLog("\uD83C\uDFF7\uFE0F " + sender.getName() + " puso el tag \"" + text
                + "\" (" + color + ") a " + target.getName());
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUso: /tag remove <jugador>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getTagManager().removeTag(target.getUniqueId());
        sender.sendMessage("§aTag eliminado de " + target.getName() + ".");
        plugin.getWebhookManager().sendLog("\uD83C\uDFF7\uFE0F " + sender.getName() + " quito el tag de " + target.getName());
    }

    private void handleList(CommandSender sender) {
        if (plugin.getTagManager().all().isEmpty()) {
            sender.sendMessage("§eNadie tiene un tag personalizado todavia.");
            return;
        }
        sender.sendMessage("§6Tags activos:");
        plugin.getTagManager().all().forEach((uuid, data) -> {
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
            sender.sendMessage(Component.text("§7- " + p.getName() + ": ").append(ColorUtil.render(data[0], data[1])));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("set", "remove", "list"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(COLORS, args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
