package me.mklv.handshaker.paper;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HandShakerPlugin extends JavaPlugin implements Listener {
    public static final String CHANNEL = "hand-shaker:mods"; // must match Fabric side (namespace matches mod id)

    private final Map<UUID, ClientInfo> clients = new ConcurrentHashMap<>();
    private BlacklistConfig blacklistConfig;

    @Override
    public void onEnable() {
        blacklistConfig = new BlacklistConfig(this);
        blacklistConfig.load();
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, (channel, player, message) -> handleModList(player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getCommand("handshaker");
        if (cmd != null) cmd.setExecutor(new HandShakerCommand(this));
        getLogger().info("HandShaker plugin enabled");
    }

    public BlacklistConfig getBlacklistConfig() { return blacklistConfig; }

    public Set<String> getClientMods(UUID uuid) {
        ClientInfo info = clients.get(uuid);
        return info != null ? info.mods : null;
    }

    // New method to check all online players
    public void checkAllPlayers() {
        getLogger().info("Re-checking all online players against the mod blacklist...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            check(player);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        clients.clear();
    }

    private void handleModList(Player player, byte[] data) {
        String payload = decodeLengthPrefixedString(data);
        Set<String> mods = new HashSet<>();
        if (payload != null && !payload.isBlank()) {
            for (String s : payload.split(",")) {
                if (!s.isBlank()) mods.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }
        clients.put(player.getUniqueId(), new ClientInfo(true, mods));
        check(player);
    }

    private String decodeLengthPrefixedString(byte[] data) {
        try {
            int idx = 0;
            int numRead = 0;
            int result = 0;
            byte read;
            do {
                read = data[idx++];
                int value = (read & 0b01111111);
                result |= (value << (7 * numRead));
                numRead++;
                if (numRead > 5) return null; // VarInt too big
            } while ((read & 0b10000000) != 0);
            int length = result;
            if (length < 0 || idx + length > data.length) return null;
            return new String(data, idx, length, StandardCharsets.UTF_8);
        } catch (Exception e) {
            getLogger().warning("Failed to decode mods payload: " + e.getMessage());
            return null;
        }
    }

    private void check(Player player) {
        ClientInfo info = clients.get(player.getUniqueId());
        if (info == null) return; // Player data not yet received, do nothing.

        BlacklistConfig.Behavior behavior = blacklistConfig.getBehavior();

        if (behavior == BlacklistConfig.Behavior.STRICT && !info.fabric()) {
            player.kick(Component.text(blacklistConfig.getNoHandshakeKickMessage()).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        Set<String> mods = info.mods();
        Set<String> blacklisted = blacklistConfig.getBlacklistedMods();

        List<String> hits = new ArrayList<>();
        for (String mod : blacklisted) {
            if (mods.contains(mod)) hits.add(mod);
        }
        if (!hits.isEmpty()) {
            String msg = blacklistConfig.getKickMessage().replace("{mod}", String.join(", ", hits));
            player.kick(Component.text(msg).color(net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            clients.putIfAbsent(e.getPlayer().getUniqueId(), new ClientInfo(false, Collections.emptySet()));
            check(e.getPlayer());
        }, 100L); // 5 seconds
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clients.remove(e.getPlayer().getUniqueId());
    }

    private record ClientInfo(boolean fabric, Set<String> mods) {}
}