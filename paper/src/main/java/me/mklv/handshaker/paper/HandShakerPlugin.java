package me.mklv.handshaker.paper;

import me.mklv.handshaker.paper.BlacklistConfig.IntegrityMode;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HandShakerPlugin extends JavaPlugin implements Listener {
    public static final String MODS_CHANNEL = "hand-shaker:mods";
    public static final String INTEGRITY_CHANNEL = "hand-shaker:integrity";

    private final Map<UUID, ClientInfo> clients = new ConcurrentHashMap<>();
    private BlacklistConfig blacklistConfig;
    private byte[] serverCertificate;

    @Override
    public void onEnable() {
        blacklistConfig = new BlacklistConfig(this);
        blacklistConfig.load();

        loadServerCertificate();

        Bukkit.getMessenger().registerIncomingPluginChannel(this, MODS_CHANNEL, (channel, player, message) -> handleModList(player, message));
        Bukkit.getMessenger().registerIncomingPluginChannel(this, INTEGRITY_CHANNEL, (channel, player, message) -> handleIntegrityPayload(player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, MODS_CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, INTEGRITY_CHANNEL);

        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getCommand("handshaker");
        if (cmd != null) cmd.setExecutor(new HandShakerCommand(this));
        getLogger().info("HandShaker plugin enabled");
    }

    private void loadServerCertificate() {
        try (InputStream is = getResource("public.cer")) {
            if (is == null) {
                if (blacklistConfig.getIntegrityMode() == IntegrityMode.SIGNED) {
                    getLogger().severe("Could not find 'public.cer' in the plugin JAR. Integrity checking will fail.");
                }
                this.serverCertificate = new byte[0];
                return;
            }
            this.serverCertificate = is.readAllBytes();
            getLogger().info("Successfully loaded embedded server certificate (" + this.serverCertificate.length + " bytes)");
        } catch (IOException e) {
            getLogger().severe("Failed to load embedded server certificate: " + e.getMessage());
            this.serverCertificate = new byte[0];
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, MODS_CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, INTEGRITY_CHANNEL);
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
        clients.compute(player.getUniqueId(), (uuid, oldInfo) -> oldInfo == null
                ? new ClientInfo(true, mods, false)
                : new ClientInfo(true, mods, oldInfo.signatureVerified()));
    }

    private void handleIntegrityPayload(Player player, byte[] data) {
        byte[] clientCertificate = decodeLengthPrefixedByteArray(data);
        boolean verified = false;
        if (clientCertificate != null && clientCertificate.length > 0 && this.serverCertificate.length > 0) {
            verified = Arrays.equals(clientCertificate, this.serverCertificate);
        }
        getLogger().info("Integrity check for " + player.getName() + ": " + (verified ? "PASSED" : "FAILED"));

        final boolean finalVerified = verified;
        clients.compute(player.getUniqueId(), (uuid, oldInfo) -> oldInfo == null
                ? new ClientInfo(false, Collections.emptySet(), finalVerified)
                : new ClientInfo(oldInfo.fabric(), oldInfo.mods(), finalVerified));
        check(player);
    }

    private void check(Player player) {
        ClientInfo info = clients.get(player.getUniqueId());
        if (info == null) return; // Player data not yet received, do nothing.

        // Handshake presence check
        if (blacklistConfig.getBehavior() == BlacklistConfig.Behavior.STRICT && !info.fabric()) {
            player.kick(Component.text(blacklistConfig.getNoHandshakeKickMessage()).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        // Integrity Check
        if (blacklistConfig.getIntegrityMode() == IntegrityMode.SIGNED) {
            if (!info.signatureVerified()) {
                player.kick(Component.text(blacklistConfig.getInvalidSignatureKickMessage()).color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
        }

        // Blacklist/Whitelist mod check
        Set<String> mods = info.mods();
        if (blacklistConfig.getMode() == BlacklistConfig.Mode.BLACKLIST) {
            List<String> hits = new ArrayList<>();
            for (String mod : blacklistConfig.getBlacklistedMods()) {
                if (mods.contains(mod)) hits.add(mod);
            }
            if (!hits.isEmpty()) {
                String msg = blacklistConfig.getKickMessage().replace("{mod}", String.join(", ", hits));
                player.kick(Component.text(msg).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        } else { // WHITELIST
            if (info.fabric() || !blacklistConfig.getWhitelistedMods().isEmpty()) {
                Set<String> whitelistedMods = blacklistConfig.getWhitelistedMods();
                List<String> missing = new ArrayList<>();
                for (String mod : whitelistedMods) {
                    if (!mods.contains(mod)) {
                        missing.add(mod);
                    }
                }
                if (!missing.isEmpty()) {
                    String msg = blacklistConfig.getMissingWhitelistModMessage().replace("{mod}", String.join(", ", missing));
                    player.kick(Component.text(msg).color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    return;
                }

                List<String> extra = new ArrayList<>();
                for (String mod : mods) {
                    if (!whitelistedMods.contains(mod)) {
                        extra.add(mod);
                    }
                }
                if (!extra.isEmpty()) {
                    String msg = blacklistConfig.getExtraWhitelistModMessage().replace("{mod}", String.join(", ", extra));
                    player.kick(Component.text(msg).color(net.kyori.adventure.text.format.NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            clients.putIfAbsent(e.getPlayer().getUniqueId(), new ClientInfo(false, Collections.emptySet(), false));
            check(e.getPlayer());
        }, 100L); // 5 seconds
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clients.remove(e.getPlayer().getUniqueId());
    }

    public BlacklistConfig getBlacklistConfig() { return blacklistConfig; }

    public Set<String> getClientMods(UUID uuid) {
        ClientInfo info = clients.get(uuid);
        return info != null ? info.mods : null;
    }

    public void checkAllPlayers() {
        getLogger().info("Re-checking all online players...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            check(player);
        }
    }

    private byte[] decodeLengthPrefixedByteArray(byte[] data) {
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
            byte[] bytes = new byte[length];
            System.arraycopy(data, idx, bytes, 0, length);
            return bytes;
        } catch (Exception e) {
            getLogger().warning("Failed to decode byte array payload: " + e.getMessage());
            return null;
        }
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

    private record ClientInfo(boolean fabric, Set<String> mods, boolean signatureVerified) {}
}
