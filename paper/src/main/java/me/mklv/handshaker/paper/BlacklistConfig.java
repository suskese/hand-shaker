package me.mklv.handshaker.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public class BlacklistConfig {
    private final HandShakerPlugin plugin;
    private File file;
    private YamlConfiguration config;
    public enum KickMode { ALL, FABRIC }
    private KickMode kickMode = KickMode.FABRIC;
    private final Set<String> blacklistedMods = new LinkedHashSet<>();
    private String kickMessage = "Remove: {mod}";

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
        // Parse KickMode
        String mode = config.getString("Kick Mode", config.getString("KickMode", "Fabric")).toUpperCase(Locale.ROOT);
        kickMode = mode.startsWith("ALL") ? KickMode.ALL : KickMode.FABRIC;
        // Parse Kick Message
        kickMessage = config.getString("Kick Message", "Remove: {mod}");
        // Parse Blacklisted Mods
        if (config.isList("Blacklisted Mods")) {
            for (Object o : config.getList("Blacklisted Mods")) {
                if (o != null) blacklistedMods.add(o.toString().toLowerCase(Locale.ROOT));
            }
        }
    }

    public KickMode getKickMode() { return kickMode; }
    public Set<String> getBlacklistedMods() { return Collections.unmodifiableSet(blacklistedMods); }
    public String getKickMessage() { return kickMessage; }

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
        config.set("Kick Mode", kickMode == KickMode.ALL ? "All" : "Fabric");
        config.set("Kick Message", kickMessage);
        config.set("Blacklisted Mods", new ArrayList<>(blacklistedMods));
        try { config.save(file); } catch (Exception ignored) {}
    }
}
