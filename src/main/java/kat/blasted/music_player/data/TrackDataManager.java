package kat.blasted.music_player.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import kat.blasted.music_player.MusicPlayerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.item.JukeboxSong;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TrackDataManager {
	private static TrackDataManager instance;
	public Map<Identifier, TrackData> tracks = new LinkedHashMap<>();

	public static TrackDataManager getInstance() {
		if (instance == null) {
			instance = new TrackDataManager();
		}
		return instance;
	}

	public static TrackData getEmpty(Identifier key, String title, String author) {
		return new TrackData(
			key,
			Component.literal(title),
			Component.literal(author),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.of(key),
			Optional.empty()
		);
	}

	public void loadFromJar() {
		try (InputStream is = MusicPlayerMod.class.getResourceAsStream("/assets/music_player/musics.json")) {
			if (is != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					JsonElement jsonElement = StrictJsonParser.parse(reader);
					Map<Identifier, TrackData> map = TrackData.MAP_CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(Collections.emptyMap());
					if (!map.isEmpty()) {
						this.tracks = new LinkedHashMap<>(map);
					}
				}
			}
		} catch (Exception e) {
			MusicPlayerMod.LOGGER.error("Failed to load embedded musics.json", e);
		}
	}

	public void reload(ResourceManager resourceManager) {
		Map<Identifier, TrackData> map = new LinkedHashMap<>();
		for (Resource resource : resourceManager.getResourceStack(Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "musics.json"))) {
			try (BufferedReader bufferedReader = resource.openAsReader()) {
				JsonElement jsonElement = StrictJsonParser.parse(bufferedReader);
				map.putAll(TrackData.MAP_CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(Collections.emptyMap()));
			} catch (Exception exception) {
				MusicPlayerMod.LOGGER.error("Failed to parse musics.json from resource pack", exception);
			}
		}
		if (!map.isEmpty()) {
			TrackData.clearAlbumCache();
			this.tracks = map;
		}
	}

	public TrackData getTrackData(Identifier soundId, Identifier soundEventId) {
		TrackData td = tracks.get(soundId);
		if (td != null) return td;

		td = tracks.get(soundEventId);
		if (td != null) return td;

		if (Minecraft.getInstance().level != null) {
			try {
				Registry<JukeboxSong> registryLookup = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
				Optional<JukeboxSong> songOpt = registryLookup.stream()
					.filter(e -> e.soundEvent().value().location().equals(soundEventId))
					.findFirst();
				if (songOpt.isPresent()) {
					JukeboxSong song = songOpt.get();
					String title = song.description().getString();
					String author = "Unknown";
					String[] split = title.split(" - ");
					if (split.length >= 2) {
						author = split[0];
						title = split[1];
					}
					return getEmpty(soundId, title, author);
				}
			} catch (Exception ignored) {}
		}

		String path = soundId.getPath();
		String endPath = path.substring(path.lastIndexOf('/') + 1);
		return getEmpty(soundId, endPath, soundId.getNamespace());
	}
}
