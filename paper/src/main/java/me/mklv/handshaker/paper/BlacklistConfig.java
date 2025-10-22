package me.mklv.handshaker.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public class BlacklistConfig {
    private final HandShakerPlugin plugin;
    private File file;
    private YamlConfiguration config;
    public enum Behavior { STRICT, VANILLA }
    private Behavior behavior = Behavior.VANILLA;
    private final Set<String> blacklistedMods = new LinkedHashSet<>();
    private String kickMessage = "Remove: {mod}";
    private String noHandshakeKickMessage = "This server requires Hand-shaker mod.";

    public BlacklistConfig(HandShakerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        blacklistedMods.clear();

        // Parse Behavior
        String behaviorString = config.getString("Behavior", "").toUpperCase(Locale.ROOT);
        if (behaviorString.isEmpty()) {
            // Backwards compatibility
            String mode = config.getString("Kick Mode", config.getString("KickMode", "Fabric")).toUpperCase(Locale.ROOT);
            behavior = mode.startsWith("ALL") ? Behavior.STRICT : Behavior.VANILLA;
        } else {
            behavior = behaviorString.startsWith("STRICT") ? Behavior.STRICT : Behavior.VANILLA;
        }

        // Parse Kick Message
        kickMessage = config.getString("Kick Message", "Remove: {mod}");
        noHandshakeKickMessage = config.getString("Missing mod message", "This server requires Hand-shaker mod.");

        // Parse Blacklisted Mods
        if (config.isList("Blacklisted Mods")) {
            for (Object o : config.getList("Blacklisted Mods")) {
                if (o != null) blacklistedMods.add(o.toString().toLowerCase(Locale.ROOT));
            }
        }
    }

    public Behavior getBehavior() { return behavior; }
    public Set<String> getBlacklistedMods() { return Collections.unmodifiableSet(blacklistedMods); }
    public String getKickMessage() { return kickMessage; }
    public String getNoHandshakeKickMessage() { return noHandshakeKickMessage; }

    public boolean addMod(String modId) {
        boolean added = blacklistedMods.add(modId.toLowerCase(Locale.ROOT));
        save();
        return added;
    }
    public boolean removeMod(String modId) {
        boolean removed = blacklistedMods.remove(modId.toLowerCase(Locale.ROOT));
        save();
        return removed;
    }
    public void save() {
        config.set("Behavior", behavior == Behavior.STRICT ? "Strict" : "Vanilla");
        config.set("Kick Message", kickMessage);
        config.set("Missing mod message", noHandshakeKickMessage);
        config.set("Blacklisted Mods", new ArrayList<>(blacklistedMods));
        try { config.save(file); } catch (Exception ignored) {}
    }
}
