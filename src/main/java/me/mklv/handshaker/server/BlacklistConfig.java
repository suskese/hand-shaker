package me.mklv.handshaker.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlacklistConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final HandShakerServer server;
    private File configFile;
    private ConfigData configData;

    public BlacklistConfig(HandShakerServer server) {
        this.server = server;
    }

    private static class ConfigData {
        KickMode kickMode = KickMode.FABRIC;
        String kickMessage = "You are using a blacklisted mod: {mod}. Please remove it to join this server.";
        Set<String> blacklistedMods = new LinkedHashSet<>();
    }

    public enum KickMode { ALL, FABRIC }

    public void load() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "hand-shaker.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                configData = GSON.fromJson(reader, ConfigData.class);
                if (configData == null) {
                    configData = new ConfigData();
                }
            } catch (IOException e) {
                HandShakerServer.LOGGER.error("Failed to read blacklist config", e);
                configData = new ConfigData();
            }
        } else {
            configData = new ConfigData();
            save();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(configData, writer);
        } catch (IOException e) {
            HandShakerServer.LOGGER.error("Failed to save blacklist config", e);
        }
    }

    public KickMode getKickMode() {
        return configData.kickMode;
    }

    public String getKickMessage() {
        return configData.kickMessage;
    }

    public Set<String> getBlacklistedMods() {
        return Collections.unmodifiableSet(configData.blacklistedMods);
    }

    public boolean addMod(String modId) {
        boolean added = configData.blacklistedMods.add(modId.toLowerCase(Locale.ROOT));
        if (added) {
            save();
        }
        return added;
    }

    public boolean removeMod(String modId) {
        boolean removed = configData.blacklistedMods.remove(modId.toLowerCase(Locale.ROOT));
        if (removed) {
            save();
        }
        return removed;
    }

    public void checkPlayer(ServerPlayerEntity player, Set<String> mods) {
        boolean isFabric = !mods.isEmpty();
        boolean shouldCheck = (configData.kickMode == KickMode.ALL) || (configData.kickMode == KickMode.FABRIC && isFabric);
        if (!shouldCheck) return;

        List<String> hits = new ArrayList<>();
        for (String mod : configData.blacklistedMods) {
            if (mods.contains(mod)) {
                hits.add(mod);
            }
        }
        if (!hits.isEmpty()) {
            String msg = configData.kickMessage.replace("{mod}", String.join(", ", hits));
            player.networkHandler.disconnect(Text.of(msg));
        }
    }
}
