package me.mklv.handshaker.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlacklistConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Behavior.class, new BehaviorDeserializer())
            .create();
    private final HandShakerServer server;
    private File configFile;
    private ConfigData configData;

    public BlacklistConfig(HandShakerServer server) {
        this.server = server;
    }

    private static class ConfigData {
        Behavior behavior = Behavior.VANILLA;
        @SerializedName("kick_message")
        String kickMessage = "You are using a blacklisted mod: {mod}. Please remove it to join this server.";
        @SerializedName("missing_mod_message")
        String noHandshakeKickMessage = "To connect to this server please download 'Hand-shaker' mod.";
        @SerializedName("blacklisted_mods")
        Set<String> blacklistedMods = new LinkedHashSet<>();

        // For backwards compatibility
        @SerializedName("kickMode")
        KickMode oldKickMode = null;
    }

    public enum Behavior { STRICT, VANILLA }
    public enum KickMode { ALL, FABRIC } // For backwards compatibility

    public static class BehaviorDeserializer implements JsonDeserializer<Behavior> {
        @Override
        public Behavior deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return Behavior.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return Behavior.VANILLA; // default value
            }
        }
    }

    public void load() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "hand-shaker.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                configData = GSON.fromJson(reader, ConfigData.class);
                if (configData == null) {
                    configData = new ConfigData();
                }
                // Backwards compatibility
                if (configData.oldKickMode != null) {
                    configData.behavior = configData.oldKickMode == KickMode.ALL ? Behavior.STRICT : Behavior.VANILLA;
                    configData.oldKickMode = null; // Don't need it anymore
                    save();
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

    public Behavior getBehavior() {
        return configData.behavior;
    }

    public String getKickMessage() {
        return configData.kickMessage;
    }

    public String getNoHandshakeKickMessage() {
        return configData.noHandshakeKickMessage;
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
        if (getBehavior() == Behavior.STRICT && !isFabric) {
            player.networkHandler.disconnect(Text.of(getNoHandshakeKickMessage()));
            return;
        }

        List<String> hits = new ArrayList<>();
        for (String mod : getBlacklistedMods()) {
            if (mods.contains(mod)) {
                hits.add(mod);
            }
        }
        if (!hits.isEmpty()) {
            String msg = getKickMessage().replace("{mod}", String.join(", ", hits));
            player.networkHandler.disconnect(Text.of(msg));
        }
    }
}
