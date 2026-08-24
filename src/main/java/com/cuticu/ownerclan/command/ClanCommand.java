package com.cuticu.ownerclan.command;

import com.cuticu.ownerclan.OwnerClanPlugin;
import com.cuticu.ownerclan.model.Clan;
import com.cuticu.ownerclan.model.ClanRole;
import com.cuticu.ownerclan.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "disband", "invite", "accept", "deny", "leave", "kick",
            "tag", "promote", "demote", "perm", "info", "list", "top");
    private static final List<String> PERM_ACTIONS = Arrays.asList("invite", "kick", "tag", "promote", "demote");

    private final OwnerClanPlugin plugin;

    public ClanCommand(OwnerClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /clan list and /clan top and /clan info <name> work without being a player.
        if (sub.equals("list")) {
            handleList(sender);
            return true;
        }
        if (sub.equals("top")) {
            handleTop(sender, args);
            return true;
        }
        if (sub.equals("info") && args.length > 1) {
            handleInfo(sender, plugin.getClanManager().getByName(args[1]));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo lo puede usar un jugador (excepto list/top/info <clan>).");
            return true;
        }

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "disband" -> handleDisband(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "tag" -> handleTag(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "perm" -> handlePerm(player, args);
            case "info" -> handleInfo(player, plugin.getClanManager().getByMember(player.getUniqueId()));
            default -> sendHelp(player);
        }
        return true;
    }

    // ---------------- subcommands ----------------

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eUso: /clan create <nombre> <color> [texto del tag]");
            return;
        }
        if (plugin.getClanManager().getByMember(player.getUniqueId()) != null) {
            player.sendMessage("§cYa perteneces a un clan.");
            return;
        }
        String name = args[1];
        if (plugin.getClanManager().exists(name)) {
            player.sendMessage("§cYa existe un clan con ese nombre.");
            return;
        }
        String color = args[2];
        if (!ColorUtil.isValidColorArg(color)) {
            player.sendMessage("§cColor invalido. Usa un color con nombre, un hex (#RRGGBB) o 'rainbow'.");
            return;
        }
        String tagText = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "[" + name + "]";

        Clan clan = plugin.getClanManager().create(name, player.getUniqueId(), tagText, color);
        player.sendMessage(Component.text("§aClan creado: ").append(ColorUtil.render(clan.getTagText(), clan.getTagColor())));

        plugin.getWebhookManager().sendNews("\uD83C\uDFF0 Se ha fundado un nuevo clan: **" + name + "**, liderado por " + player.getName());
    }

    private void handleDisband(Player player) {
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage("§cSolo el lider puede disolver el clan.");
            return;
        }
        plugin.getClanManager().disband(clan);
        player.sendMessage("§aClan disuelto.");
        plugin.getWebhookManager().sendNews("\u26B0\uFE0F El clan **" + clan.getName() + "** se ha disuelto.");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUso: /clan invite <jugador>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.canPerform(player.getUniqueId(), "invite")) {
            player.sendMessage("§cNo tienes permiso para invitar gente a este clan.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cEse jugador no esta conectado.");
            return;
        }
        if (plugin.getClanManager().getByMember(target.getUniqueId()) != null) {
            player.sendMessage("§cEse jugador ya esta en un clan.");
            return;
        }
        plugin.getClanManager().invite(target.getUniqueId(), clan.getName());
        player.sendMessage("§aInvitacion enviada a " + target.getName() + ".");
        target.sendMessage("§6Has sido invitado al clan §f" + clan.getName() + "§6. Usa §f/clan accept §6o §f/clan deny");
    }

    private void handleAccept(Player player) {
        String pending = plugin.getClanManager().getPendingInvite(player.getUniqueId());
        if (pending == null) {
            player.sendMessage("§cNo tienes ninguna invitacion pendiente.");
            return;
        }
        Clan clan = plugin.getClanManager().getByName(pending);
        plugin.getClanManager().clearInvite(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cEse clan ya no existe.");
            return;
        }
        if (plugin.getClanManager().getByMember(player.getUniqueId()) != null) {
            player.sendMessage("§cYa perteneces a un clan.");
            return;
        }
        clan.getMembers().put(player.getUniqueId(), ClanRole.MEMBER);
        plugin.getClanManager().save();
        player.sendMessage(Component.text("§aTe has unido al clan ").append(ColorUtil.render(clan.getTagText(), clan.getTagColor())));
        plugin.getWebhookManager().sendLog("\u2795 " + player.getName() + " se unio al clan " + clan.getName());
    }

    private void handleDeny(Player player) {
        if (plugin.getClanManager().getPendingInvite(player.getUniqueId()) == null) {
            player.sendMessage("§cNo tienes ninguna invitacion pendiente.");
            return;
        }
        plugin.getClanManager().clearInvite(player.getUniqueId());
        player.sendMessage("§7Invitacion rechazada.");
    }

    private void handleLeave(Player player) {
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (clan.getLeader().equals(player.getUniqueId()) && clan.size() > 1) {
            player.sendMessage("§cEres el lider. Asciende a otro miembro antes de irte, o usa /clan disband.");
            return;
        }
        clan.getMembers().remove(player.getUniqueId());
        if (clan.size() == 0) {
            plugin.getClanManager().disband(clan);
        } else {
            plugin.getClanManager().save();
        }
        player.sendMessage("§7Has salido del clan " + clan.getName() + ".");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUso: /clan kick <jugador>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.canPerform(player.getUniqueId(), "kick")) {
            player.sendMessage("§cNo tienes permiso para expulsar miembros.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (clan.getRole(target.getUniqueId()) == null) {
            player.sendMessage("§cEse jugador no esta en tu clan.");
            return;
        }
        if (target.getUniqueId().equals(clan.getLeader())) {
            player.sendMessage("§cNo puedes expulsar al lider.");
            return;
        }
        clan.getMembers().remove(target.getUniqueId());
        plugin.getClanManager().save();
        player.sendMessage("§aExpulsaste a " + target.getName() + " del clan.");
        plugin.getWebhookManager().sendLog("\uD83D\uDC62 " + player.getName() + " expulso a " + target.getName() + " del clan " + clan.getName());
    }

    private void handleTag(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eUso: /clan tag <color> <texto...>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.canPerform(player.getUniqueId(), "tag")) {
            player.sendMessage("§cNo tienes permiso para cambiar el tag del clan.");
            return;
        }
        String color = args[1];
        if (!ColorUtil.isValidColorArg(color)) {
            player.sendMessage("§cColor invalido. Usa un color con nombre, un hex (#RRGGBB) o 'rainbow'.");
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        clan.setTagText(text);
        clan.setTagColor(color);
        plugin.getClanManager().save();
        player.sendMessage(Component.text("§aTag del clan actualizado: ").append(ColorUtil.render(text, color)));
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUso: /clan promote <jugador>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.canPerform(player.getUniqueId(), "promote")) {
            player.sendMessage("§cNo tienes permiso para ascender miembros.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        ClanRole role = clan.getRole(target.getUniqueId());
        if (role == null) {
            player.sendMessage("§cEse jugador no esta en tu clan.");
            return;
        }
        if (role == ClanRole.MEMBER) {
            clan.getMembers().put(target.getUniqueId(), ClanRole.OFFICER);
            plugin.getClanManager().save();
            player.sendMessage("§a" + target.getName() + " ahora es oficial del clan.");
        } else {
            player.sendMessage("§cEse jugador ya es oficial o lider.");
        }
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUso: /clan demote <jugador>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.canPerform(player.getUniqueId(), "demote")) {
            player.sendMessage("§cNo tienes permiso para degradar miembros.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        ClanRole role = clan.getRole(target.getUniqueId());
        if (role == ClanRole.OFFICER) {
            clan.getMembers().put(target.getUniqueId(), ClanRole.MEMBER);
            plugin.getClanManager().save();
            player.sendMessage("§a" + target.getName() + " ya no es oficial.");
        } else {
            player.sendMessage("§cSolo puedes degradar a un oficial.");
        }
    }

    private void handlePerm(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eUso: /clan perm <" + String.join("|", PERM_ACTIONS) + "> <true|false>");
            return;
        }
        Clan clan = requireClan(player);
        if (clan == null) return;
        if (!clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage("§cSolo el lider puede cambiar los permisos de los oficiales.");
            return;
        }
        String action = args[1].toLowerCase();
        if (!PERM_ACTIONS.contains(action)) {
            player.sendMessage("§cAccion invalida. Opciones: " + String.join(", ", PERM_ACTIONS));
            return;
        }
        boolean value = Boolean.parseBoolean(args[2]);
        clan.getOfficerPermissions().put(action, value);
        plugin.getClanManager().save();
        player.sendMessage("§aLos oficiales ahora " + (value ? "SI" : "NO") + " pueden usar '" + action + "'.");
        plugin.getWebhookManager().sendLog("\u2699\uFE0F " + player.getName() + " cambio el permiso '" + action + "' a " + value + " en el clan " + clan.getName());
    }

    private void handleInfo(CommandSender sender, Clan clan) {
        if (clan == null) {
            sender.sendMessage("§cClan no encontrado.");
            return;
        }
        sender.sendMessage(Component.text("§6Clan: ").append(ColorUtil.render(clan.getTagText(), clan.getTagColor())));
        sender.sendMessage("§7Nombre: §f" + clan.getName());
        sender.sendMessage("§7Kills: §f" + clan.getKills());
        sender.sendMessage("§7Miembros (" + clan.size() + "):");
        for (Map.Entry<UUID, ClanRole> e : clan.getMembers().entrySet()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
            sender.sendMessage("  §7- §f" + p.getName() + " §8(" + e.getValue() + ")");
        }
    }

    private void handleList(CommandSender sender) {
        if (plugin.getClanManager().all().isEmpty()) {
            sender.sendMessage("§eTodavia no hay clanes.");
            return;
        }
        sender.sendMessage("§6Clanes:");
        for (Clan clan : plugin.getClanManager().all()) {
            sender.sendMessage(Component.text("§7- ").append(ColorUtil.render(clan.getTagText(), clan.getTagColor()))
                    .append(Component.text(" §f" + clan.getName() + " §8(" + clan.size() + " miembros, " + clan.getKills() + " kills)")));
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        int limit = plugin.getConfig().getInt("leaderboard-size", 5);
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        List<Clan> top = plugin.getClanManager().topByKills(limit);
        if (top.isEmpty()) {
            sender.sendMessage("§eTodavia no hay clanes con kills.");
            return;
        }
        sender.sendMessage("§6Ranking de clanes por kills:");
        int pos = 1;
        for (Clan clan : top) {
            sender.sendMessage("§7" + pos + ". §f" + clan.getName() + " §7- §f" + clan.getKills() + " kills");
            pos++;
        }
    }

    // ---------------- helpers ----------------

    private Clan requireClan(Player player) {
        Clan clan = plugin.getClanManager().getByMember(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cNo perteneces a ningun clan.");
        }
        return clan;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6--- Comandos de clan ---");
        sender.sendMessage("§f/clan create <nombre> <color> [texto] §7- crea un clan");
        sender.sendMessage("§f/clan invite <jugador> §7- invita a alguien");
        sender.sendMessage("§f/clan accept|deny §7- responde a una invitacion");
        sender.sendMessage("§f/clan kick <jugador> §7- expulsa a un miembro");
        sender.sendMessage("§f/clan leave §7- abandona el clan");
        sender.sendMessage("§f/clan disband §7- disuelve el clan (solo lider)");
        sender.sendMessage("§f/clan tag <color> <texto> §7- cambia el tag del clan");
        sender.sendMessage("§f/clan promote|demote <jugador> §7- gestiona oficiales");
        sender.sendMessage("§f/clan perm <accion> <true|false> §7- permisos de oficiales (solo lider)");
        sender.sendMessage("§f/clan info [nombre] §7- info del clan");
        sender.sendMessage("§f/clan list §7- lista de clanes");
        sender.sendMessage("§f/clan top [n] §7- ranking por kills");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2 && List.of("invite", "kick", "promote", "demote").contains(sub)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && sub.equals("perm")) {
            return filter(PERM_ACTIONS, args[1]);
        }
        if (args.length == 3 && (sub.equals("create") || sub.equals("tag"))) {
            return filter(Arrays.asList("white", "red", "gold", "green", "aqua", "blue", "light_purple", "rainbow"), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
