package com.cuticu.ownerclan.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores a custom chat tag (text + color) per player UUID, persisted to tags.yml.
 * These tags take priority over clan tags when rendering chat.
 */
public class TagManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String[]> tags = new HashMap<>(); // uuid -> [text, color]

    public TagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tags.yml");
        load();
    }

    public void setTag(UUID uuid, String text, String color) {
        tags.put(uuid, new String[]{text, color});
        save();
    }

    public void removeTag(UUID uuid) {
        tags.remove(uuid);
        save();
    }

    public boolean hasTag(UUID uuid) {
        return tags.containsKey(uuid);
    }

    public String getTagText(UUID uuid) {
        String[] t = tags.get(uuid);
        return t == null ? null : t[0];
    }

    public String getTagColor(UUID uuid) {
        String[] t = tags.get(uuid);
        return t == null ? null : t[1];
    }

    public Map<UUID, String[]> all() {
        return tags;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String text = cfg.getString(key + ".text");
                String color = cfg.getString(key + ".color");
                if (text != null && color != null) {
                    tags.put(uuid, new String[]{text, color});
                }
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID key, skip it.
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String[]> entry : tags.entrySet()) {
            String base = entry.getKey().toString();
            cfg.set(base + ".text", entry.getValue()[0]);
            cfg.set(base + ".color", entry.getValue()[1]);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar tags.yml: " + e.getMessage());
        }
    }
}
