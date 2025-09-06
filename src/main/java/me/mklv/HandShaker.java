package me.mklv;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandShaker implements ClientModInitializer {
	public static final String MOD_ID = "hand-shaker";
	public static final Identifier CHANNEL = Identifier.of(MOD_ID, "mods");

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("HandShaker client initializing");
		// Register payload type for 1.21 custom payload system so server treats it as plugin message (will fall back to unknown server-side gracefully)
		PayloadTypeRegistry.playC2S().register(ModsListPayload.ID, ModsListPayload.CODEC);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendModList());
	}

	private void sendModList() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null) return;
		String payload = FabricLoader.getInstance().getAllMods().stream()
				.map(m -> m.getMetadata().getId())
				.sorted()
				.reduce((a,b) -> a + "," + b)
				.orElse("");
		ClientPlayNetworking.send(new ModsListPayload(payload));
		LOGGER.info("Sent mod list ({} chars)", payload.length());
	}

	public record ModsListPayload(String mods) implements CustomPayload {
		public static final CustomPayload.Id<ModsListPayload> ID = new CustomPayload.Id<>(CHANNEL);
		public static final PacketCodec<PacketByteBuf, ModsListPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, ModsListPayload::mods, ModsListPayload::new);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}
}