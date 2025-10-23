package me.mklv.handshaker.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class BlacklistConfig {
    private final HandShakerPlugin plugin;
    private File file;
    private YamlConfiguration config;

    public enum Mode {BLACKLIST, WHITELIST}
    public enum Behavior { STRICT, VANILLA }
    public enum IntegrityMode { SIGNED, DEV }

    private Mode mode = Mode.BLACKLIST;
    private Behavior behavior = Behavior.STRICT;
    private IntegrityMode integrityMode = IntegrityMode.SIGNED;
    private final Set<String> blacklistedMods = new LinkedHashSet<>();
    private final Set<String> whitelistedMods = new LinkedHashSet<>();
    private String kickMessage = "You are using a blacklisted mod: {mod}. Please remove it to join this server.";
    private String noHandshakeKickMessage = "To connect to this server please download 'Hand-shaker' mod.";
    private String missingWhitelistModMessage = "You are missing required mods: {mod}. Please install them to join this server.";
    private String extraWhitelistModMessage = "You have mods that are not on the whitelist: {mod}. Please remove them to join.";
    private String invalidSignatureKickMessage = "Invalid client signature. Please use the official client.";

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
        whitelistedMods.clear();

        // Parse Integrity Mode
        String integrityString = config.getString("Integrity", "signed").toUpperCase(Locale.ROOT);
        integrityMode = integrityString.equals("SIGNED") ? IntegrityMode.SIGNED : IntegrityMode.DEV;

        // Parse Mode
        String modeString = config.getString("Operation Mode", "blacklist").toUpperCase(Locale.ROOT);
        mode = modeString.equals("WHITELIST") ? Mode.WHITELIST : Mode.BLACKLIST;

        // Parse Behavior
        String behaviorString = config.getString("Behavior", "strict").toUpperCase(Locale.ROOT);
        if (behaviorString.isEmpty()) {
            // Backwards compatibility
            String oldMode = config.getString("Kick Mode", config.getString("KickMode", "Fabric")).toUpperCase(Locale.ROOT);
            behavior = oldMode.startsWith("ALL") ? Behavior.STRICT : Behavior.VANILLA;
        } else {
            behavior = behaviorString.startsWith("STRICT") ? Behavior.STRICT : Behavior.VANILLA;
        }

        // Parse Kick Message
        kickMessage = config.getString("Kick Message", "You are using a blacklisted mod: {mod}. Please remove it to join this server.");
        noHandshakeKickMessage = config.getString("Missing mod message", "To connect to this server please download 'Hand-shaker' mod.");
        missingWhitelistModMessage = config.getString("Missing whitelist mod message", "You are missing required mods: {mod}. Please install them to join this server.");
        extraWhitelistModMessage = config.getString("Extra whitelist mod message", "You have mods that are not on the whitelist: {mod}. Please remove them to join.");
        invalidSignatureKickMessage = config.getString("Invalid signature kick message", "Invalid client signature. Please use the official client.");

        // Parse Blacklisted Mods
        if (config.isList("Blacklisted Mods")) {
            for (Object o : config.getList("Blacklisted Mods")) {
                if (o != null) blacklistedMods.add(o.toString().toLowerCase(Locale.ROOT));
            }
        }

        // Parse Whitelisted Mods
        if (config.isList("Whitelisted Mods")) {
            for (Object o : config.getList("Whitelisted Mods")) {
                if (o != null) whitelistedMods.add(o.toString().toLowerCase(Locale.ROOT));
            }
        }
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; save(); }
    public Behavior getBehavior() { return behavior; }
    public IntegrityMode getIntegrityMode() { return integrityMode; }
    public Set<String> getBlacklistedMods() { return Collections.unmodifiableSet(blacklistedMods); }
    public Set<String> getWhitelistedMods() { return Collections.unmodifiableSet(whitelistedMods); }
    public String getKickMessage() { return kickMessage; }
    public String getNoHandshakeKickMessage() { return noHandshakeKickMessage; }
    public String getMissingWhitelistModMessage() { return missingWhitelistModMessage; }
    public String getExtraWhitelistModMessage() { return extraWhitelistModMessage; }
    public String getInvalidSignatureKickMessage() { return invalidSignatureKickMessage; }

    public boolean addMod(String modId) {
        boolean added = blacklistedMods.add(modId.toLowerCase(Locale.ROOT));
        if(added) save();
        return added;
    }
    public boolean removeMod(String modId) {
        boolean removed = blacklistedMods.remove(modId.toLowerCase(Locale.ROOT));
        if(removed) save();
        return removed;
    }

    public void setWhitelistedMods(Set<String> mods) {
        whitelistedMods.clear();
        for (String mod : mods) {
            whitelistedMods.add(mod.toLowerCase(Locale.ROOT));
        }
        save();
    }

    public void save() {
        config.set("Integrity", integrityMode == IntegrityMode.SIGNED ? "Signed" : "Dev");
        config.set("Operation Mode", mode == Mode.WHITELIST ? "whitelist" : "blacklist");
        config.set("Behavior", behavior == Behavior.STRICT ? "Strict" : "Vanilla");
        config.set("Kick Message", kickMessage);
        config.set("Missing mod message", noHandshakeKickMessage);
        config.set("Missing whitelist mod message", missingWhitelistModMessage);
        config.set("Extra whitelist mod message", extraWhitelistModMessage);
        config.set("Invalid signature kick message", invalidSignatureKickMessage);
        config.set("Blacklisted Mods", new ArrayList<>(blacklistedMods));
        config.set("Whitelisted Mods", new ArrayList<>(whitelistedMods));
        try { config.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save config.yml!"); }
    }
}