package kat.blasted.music_player;

import kat.blasted.music_player.data.TrackDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicPlayerMod implements ClientModInitializer {
	public static final String MOD_ID = "music_player";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static @Nullable SoundInstance currentlyPlaying;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Music Player mod");

		// Load tracks from classpath
		TrackDataManager.getInstance().loadFromJar();

		// Register resource reload listener for dynamic packs
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(MOD_ID, "tracks_reloader"),
			new SimpleReloadListener<Void>() {
				@Override
				protected @NonNull Void prepare(SharedState state) {
					return null;
				}

				@Override
				protected void apply(@NonNull Void prepared, SharedState state) {
					TrackDataManager.getInstance().reload(state.resourceManager());
				}
			}
		);
	}
}
