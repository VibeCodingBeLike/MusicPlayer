package kat.blasted.music_player.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kat.blasted.music_player.MusicPlayerMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public record TrackData(
	Identifier key,
	Component title,
	Component author,
	Optional<Component> album,
	Optional<String> cover,
	Optional<Identifier> item,
	Optional<Identifier> customId,
	Optional<Integer> track
) {
	public static final Codec<Component> COMPONENT_CODEC = Codec.STRING.xmap(Component::literal, Component::getString);

	public static final Codec<TrackData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.optionalFieldOf("key").forGetter(t -> Optional.ofNullable(t.key)),
		COMPONENT_CODEC.fieldOf("title").forGetter(TrackData::title),
		COMPONENT_CODEC.fieldOf("author").forGetter(TrackData::author),
		COMPONENT_CODEC.optionalFieldOf("album").forGetter(TrackData::album),
		Codec.STRING.optionalFieldOf("cover").forGetter(TrackData::cover),
		Identifier.CODEC.optionalFieldOf("item").forGetter(TrackData::item),
		Identifier.CODEC.optionalFieldOf("customId").forGetter(TrackData::customId),
		Codec.INT.optionalFieldOf("track").forGetter(TrackData::track)
	).apply(i, (keyOpt, title, author, album, cover, item, customId, track) ->
		new TrackData(keyOpt.orElse(Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "generic")), title, author, album, cover, item, customId, track)
	));

	public static final Codec<Map<Identifier, TrackData>> MAP_CODEC = ExtraCodecs.strictUnboundedMap(Identifier.CODEC, CODEC)
		.xmap(TrackData::injectKeysFromMap, TrackData::injectKeysFromMap);

	private static final Map<Identifier, AlbumCover> ALBUM_CACHE = new ConcurrentHashMap<>();

	private static Map<Identifier, TrackData> injectKeysFromMap(Map<Identifier, TrackData> input) {
		Map<Identifier, TrackData> out = new LinkedHashMap<>();
		for (Map.Entry<Identifier, TrackData> e : input.entrySet()) {
			Identifier mapKey = e.getKey();
			TrackData value = e.getValue();
			if (!value.key().equals(mapKey)) {
				value = new TrackData(mapKey, value.title(), value.author(), value.album(), value.cover(), value.item(), value.customId(), value.track());
			}
			out.put(mapKey, value);
		}
		return out;
	}

	public static void clearAlbumCache() {
		ALBUM_CACHE.clear();
	}

	public Identifier getResolvedId() {
		return this.customId().orElse(this.key());
	}

	public AlbumCover getAlbumCover() {
		return ALBUM_CACHE.computeIfAbsent(this.key, k -> AlbumCover.fromSprite(k.getNamespace(), this.cover.orElse(null)));
	}
}
