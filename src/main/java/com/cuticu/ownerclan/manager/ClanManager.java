package com.cuticu.ownerclan.manager;

import com.cuticu.ownerclan.model.Clan;
import com.cuticu.ownerclan.model.ClanRole;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Holds every clan in memory, persists them to clans.yml, and tracks
 * pending clan invites (player -> clan name they've been invited to).
 */
public class ClanManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Clan> clansByName = new LinkedHashMap<>(); // key = lowercase name
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public ClanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    public Clan create(String name, UUID leader, String tagText, String tagColor) {
        Clan clan = new Clan(name, leader, tagText, tagColor);
        clansByName.put(name.toLowerCase(), clan);
        save();
        return clan;
    }

    public void disband(Clan clan) {
        clansByName.remove(clan.getName().toLowerCase());
        save();
    }

    public Clan getByName(String name) {
        if (name == null) return null;
        return clansByName.get(name.toLowerCase());
    }

    public Clan getByMember(UUID uuid) {
        for (Clan clan : clansByName.values()) {
            if (clan.getMembers().containsKey(uuid)) return clan;
        }
        return null;
    }

    public boolean exists(String name) {
        return clansByName.containsKey(name.toLowerCase());
    }

    public Collection<Clan> all() {
        return clansByName.values();
    }

    public List<Clan> topByKills(int limit) {
        List<Clan> list = new ArrayList<>(clansByName.values());
        list.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    // ---- invites ----

    public void invite(UUID player, String clanName) {
        pendingInvites.put(player, clanName.toLowerCase());
    }

    public String getPendingInvite(UUID player) {
        return pendingInvites.get(player);
    }

    public void clearInvite(UUID player) {
        pendingInvites.remove(player);
    }

    // ---- persistence ----

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection section = cfg.getConfigurationSection(key);
            if (section == null) continue;

            String name = section.getString("name", key);
            String tagText = section.getString("tagText", "[" + name + "]");
            String tagColor = section.getString("tagColor", "white");
            int kills = section.getInt("kills", 0);

            ConfigurationSection membersSection = section.getConfigurationSection("members");
            UUID leader = null;
            Map<UUID, ClanRole> members = new LinkedHashMap<>();
            if (membersSection != null) {
                for (String uuidStr : membersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        ClanRole role = ClanRole.valueOf(membersSection.getString(uuidStr, "MEMBER"));
                        members.put(uuid, role);
                        if (role == ClanRole.LEADER) leader = uuid;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            if (leader == null && !members.isEmpty()) {
                leader = members.keySet().iterator().next();
                members.put(leader, ClanRole.LEADER);
            }
            if (leader == null) continue; // corrupt entry, skip

            Clan clan = new Clan(name, leader, tagText, tagColor);
            clan.getMembers().clear();
            clan.getMembers().putAll(members);
            clan.setKills(kills);

            ConfigurationSection permsSection = section.getConfigurationSection("officerPermissions");
            if (permsSection != null) {
                for (String permKey : permsSection.getKeys(false)) {
                    clan.getOfficerPermissions().put(permKey, permsSection.getBoolean(permKey));
                }
            }

            clansByName.put(name.toLowerCase(), clan);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Clan clan : clansByName.values()) {
            String base = clan.getName().toLowerCase();
            cfg.set(base + ".name", clan.getName());
            cfg.set(base + ".tagText", clan.getTagText());
            cfg.set(base + ".tagColor", clan.getTagColor());
            cfg.set(base + ".kills", clan.getKills());
            for (Map.Entry<UUID, ClanRole> e : clan.getMembers().entrySet()) {
                cfg.set(base + ".members." + e.getKey(), e.getValue().name());
            }
            for (Map.Entry<String, Boolean> e : clan.getOfficerPermissions().entrySet()) {
                cfg.set(base + ".officerPermissions." + e.getKey(), e.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar clans.yml: " + e.getMessage());
        }
    }
}
