package kat.blasted.music_player.data;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.mixin.ClientLevelAccessor;
import kat.blasted.music_player.mixin.LevelEventHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SoundListener implements SoundEventListener {
	private static final Set<SoundInstance> activeJukeboxSounds = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public static void addJukeboxSound(SoundInstance sound) {
		activeJukeboxSounds.add(sound);
	}

	public static void removeJukeboxSound(SoundInstance sound) {
		activeJukeboxSounds.remove(sound);
	}

	public static void clearJukeboxSounds() {
		activeJukeboxSounds.clear();
	}

	public static boolean isJukeboxPlaying(SoundManager soundManager) {
		activeJukeboxSounds.removeIf(s -> !soundManager.isActive(s));
		if (!activeJukeboxSounds.isEmpty()) {
			return true;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			try {
				LevelEventHandler handler = ((ClientLevelAccessor) client.level).getLevelEventHandler();
				Map<BlockPos, SoundInstance> map = ((LevelEventHandlerAccessor) handler).getPlayingJukeboxSongs();
				if (map != null && !map.isEmpty()) {
					for (SoundInstance sound : map.values()) {
						if (soundManager.isActive(sound)) {
							return true;
						}
					}
				}
			} catch (Throwable ignored) {}
		}

		return false;
	}

	@Override
	public void onPlaySound(SoundInstance soundInstance, WeighedSoundEvents weighedSoundEvents, float f1) {
		if (soundInstance.getSource() == SoundSource.RECORDS) {
			activeJukeboxSounds.add(soundInstance);
			MusicPlayerMod.LOGGER.info("SoundListener: Jukebox record started playing: {}", soundInstance.getIdentifier());
		}

		if (soundInstance.getSource() != SoundSource.MUSIC && soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		Identifier soundEventLocation = soundInstance.getIdentifier();
		Sound sound = soundInstance.getSound();
		Identifier soundLocation = sound != null ? sound.getLocation() : soundEventLocation;

		TrackData td = TrackDataManager.getInstance().getTrackData(soundLocation, soundEventLocation);
		TrackHistory.getInstance().addTrack(td);
	}
}
