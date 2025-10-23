package me.mklv.handshaker.server;

import me.mklv.HandShaker;
import me.mklv.handshaker.server.BlacklistConfig.IntegrityMode;
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

import java.io.IOException;
import java.io.InputStream;
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
    private final Map<UUID, ClientInfo> clients = new ConcurrentHashMap<>();
    private BlacklistConfig blacklistConfig;
    private MinecraftServer server;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private byte[] serverCertificate;

    public record ClientInfo(Set<String> mods, boolean signatureVerified) {}

    @Override
    public void onInitializeServer() {
        LOGGER.info("HandShaker server initializing");
        blacklistConfig = new BlacklistConfig(this);
        blacklistConfig.load();

        loadServerCertificate();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> scheduler.shutdown());

        // Register payload types
        PayloadTypeRegistry.playC2S().register(HandShaker.ModsListPayload.ID, HandShaker.ModsListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HandShaker.IntegrityPayload.ID, HandShaker.IntegrityPayload.CODEC);

        // Register payload handlers
        ServerPlayNetworking.registerGlobalReceiver(HandShaker.ModsListPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Set<String> mods = new HashSet<>(Arrays.asList(payload.mods().split(",")));
            if (payload.mods().isEmpty()) {
                mods.clear();
            }
            LOGGER.info("Received mod list from {}", player.getName().getString());
            clients.compute(player.getUuid(), (uuid, oldInfo) ->
                    new ClientInfo(mods, oldInfo != null && oldInfo.signatureVerified()));
        });

        ServerPlayNetworking.registerGlobalReceiver(HandShaker.IntegrityPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            byte[] clientCertificate = payload.signature();
            boolean verified = false;
            if (clientCertificate != null && clientCertificate.length > 0 && this.serverCertificate.length > 0) {
                verified = Arrays.equals(clientCertificate, this.serverCertificate);
            }
            LOGGER.info("Integrity check for {}: {}", player.getName().getString(), verified ? "PASSED" : "FAILED");

            final boolean finalVerified = verified;
            clients.compute(player.getUuid(), (uuid, oldInfo) ->
                    new ClientInfo(oldInfo != null ? oldInfo.mods() : Collections.emptySet(), finalVerified));
            blacklistConfig.checkPlayer(player, clients.get(player.getUuid()));
        });

        // Register player lifecycle events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            scheduler.schedule(() -> {
                server.execute(() -> {
                    if (handler.player.networkHandler == null) return; // Player disconnected
                    // If the player is still in the map, re-run the check.
                    // If they are not (e.g. vanilla client), create a default entry and check that.
                    ClientInfo info = clients.computeIfAbsent(handler.player.getUuid(), uuid -> new ClientInfo(Collections.emptySet(), false));
                    blacklistConfig.checkPlayer(handler.player, info);
                });
            }, 5, TimeUnit.SECONDS);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            clients.remove(handler.player.getUuid());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HandShakerCommand.register(dispatcher, this);
        });
    }

    private void loadServerCertificate() {
        try (InputStream is = HandShakerServer.class.getClassLoader().getResourceAsStream("public.cer")) {
            if (is == null) {
                if (blacklistConfig.getIntegrityMode() == IntegrityMode.SIGNED) {
                    LOGGER.error("Could not find 'public.cer' in the mod JAR. Integrity checking will fail.");
                }
                this.serverCertificate = new byte[0];
                return;
            }
            this.serverCertificate = is.readAllBytes();
            LOGGER.info("Successfully loaded embedded server certificate ({} bytes)", this.serverCertificate.length);
        } catch (IOException e) {
            LOGGER.error("Failed to load embedded server certificate", e);
            this.serverCertificate = new byte[0];
        }
    }

    public BlacklistConfig getBlacklistConfig() {
        return blacklistConfig;
    }

    public Map<UUID, ClientInfo> getClients() {
        return clients;
    }

    public void checkAllPlayers() {
        if (server == null) return;
        LOGGER.info("Re-checking all online players...");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            blacklistConfig.checkPlayer(player, clients.getOrDefault(player.getUuid(), new ClientInfo(Collections.emptySet(), false)));
        }
    }
}