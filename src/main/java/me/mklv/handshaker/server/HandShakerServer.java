package me.mklv.handshaker.server;

import me.mklv.HandShaker;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HandShakerServer implements DedicatedServerModInitializer {
    public static final String MOD_ID = "hand-shaker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-server");
    private final Map<UUID, Set<String>> clientMods = new ConcurrentHashMap<>();
    private BlacklistConfig blacklistConfig;
    private MinecraftServer server;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onInitializeServer() {
        LOGGER.info("HandShaker server initializing");
        blacklistConfig = new BlacklistConfig(this);
        blacklistConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> scheduler.shutdown());

        PayloadTypeRegistry.playC2S().register(HandShaker.ModsListPayload.ID, HandShaker.ModsListPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HandShaker.ModsListPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Set<String> mods = new HashSet<>(Arrays.asList(payload.mods().split(",")));
            if (payload.mods().isEmpty()) {
                mods.clear();
            }
            clientMods.put(player.getUuid(), mods);
            LOGGER.info("Received mod list from {}", player.getName().getString());
            blacklistConfig.checkPlayer(player, mods);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            scheduler.schedule(() -> {
                server.execute(() -> {
                    if (handler.player.networkHandler == null) return; // Player disconnected
                    if (!clientMods.containsKey(handler.player.getUuid())) {
                        blacklistConfig.checkPlayer(handler.player, Collections.emptySet());
                    }
                });
            }, 5, TimeUnit.SECONDS);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            clientMods.remove(handler.player.getUuid());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HandShakerCommand.register(dispatcher, this);
        });
    }

    public BlacklistConfig getBlacklistConfig() {
        return blacklistConfig;
    }

    public Map<UUID, Set<String>> getClientMods() {
        return clientMods;
    }

    public void checkAllPlayers() {
        if (server == null) return;
        LOGGER.info("Re-checking all online players against the mod blacklist...");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            blacklistConfig.checkPlayer(player, clientMods.getOrDefault(player.getUuid(), Collections.emptySet()));
        }
    }
}
