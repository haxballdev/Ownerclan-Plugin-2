package com.cuticu.ownerclan.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single clan and everything about it: members, roles,
 * chat tag, per-role command permissions and its kill counter.
 */
public class Clan {

    private String name;
    private String tagText;
    private String tagColor;
    private final Map<UUID, ClanRole> members = new LinkedHashMap<>();
    // Which sub-commands an OFFICER is allowed to use. LEADER can always do everything.
    private final Map<String, Boolean> officerPermissions = new LinkedHashMap<>();
    private int kills = 0;

    public Clan(String name, UUID leader, String tagText, String tagColor) {
        this.name = name;
        this.tagText = tagText;
        this.tagColor = tagColor;
        this.members.put(leader, ClanRole.LEADER);

        // Sensible defaults: officers can invite and set the tag, but not kick or promote.
        officerPermissions.put("invite", true);
        officerPermissions.put("kick", false);
        officerPermissions.put("tag", false);
        officerPermissions.put("promote", false);
        officerPermissions.put("demote", false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public Map<UUID, ClanRole> getMembers() {
        return members;
    }

    public Map<String, Boolean> getOfficerPermissions() {
        return officerPermissions;
    }

    public boolean officerCan(String action) {
        return officerPermissions.getOrDefault(action, false);
    }

    public UUID getLeader() {
        for (Map.Entry<UUID, ClanRole> e : members.entrySet()) {
            if (e.getValue() == ClanRole.LEADER) return e.getKey();
        }
        return null;
    }

    public ClanRole getRole(UUID uuid) {
        return members.get(uuid);
    }

    /**
     * Returns whether the given member is allowed to perform the given clan action,
     * based on their role and, for officers, the clan's configured permissions.
     */
    public boolean canPerform(UUID uuid, String action) {
        ClanRole role = members.get(uuid);
        if (role == null) return false;
        if (role == ClanRole.LEADER) return true;
        if (role == ClanRole.OFFICER) return officerCan(action);
        return false;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int size() {
        return members.size();
    }
}
