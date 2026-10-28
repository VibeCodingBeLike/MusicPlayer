package kat.blasted.music_player.util;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.data.SoundListener;
import kat.blasted.music_player.data.TrackData;
import kat.blasted.music_player.mixin.MusicManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

public class MusicPlayerHelper {
	public static boolean isVolumeZero() {
		Minecraft client = Minecraft.getInstance();
		return client.options.getSoundSourceVolume(SoundSource.MUSIC) == 0f ||
		       client.options.getSoundSourceVolume(SoundSource.MASTER) == 0f;
	}

	public static void playTrack(Minecraft client, TrackData track) {
		Identifier id = track.getResolvedId();
		SoundEvent event = getSoundEvent(client.getSoundManager(), id);
		if (event == null) {
			event = getSoundEvent(client.getSoundManager(), track.key());
		}
		if (event == null) {
			event = SoundEvent.createVariableRangeEvent(id);
		}
		MusicPlayerMod.LOGGER.info("Playing track '{}' by '{}' with sound event '{}'", track.title().getString(), track.author().getString(), event.location());
		playSong(client, event);
	}

	public static void playSong(Minecraft client, SoundEvent event) {
		stopMusic(client);
		// If a jukebox record was playing, stop it so user's manual selection plays cleanly
		client.getSoundManager().stop(null, SoundSource.RECORDS);
		SoundListener.clearJukeboxSounds();

		float volume = client.getMusicVolume();
		MusicManagerAccessor mm = (MusicManagerAccessor) client.getMusicManager();
		mm.setCurrentGain(volume);
		client.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, volume);

		SimpleSoundInstance sound = SimpleSoundInstance.forMusic(event);
		client.getSoundManager().play(sound);
		mm.setCurrentMusic(sound);
		MusicPlayerMod.currentlyPlaying = sound;
		client.gui.toastManager().showNowPlayingToast();
	}

	public static void stopMusic(Minecraft client) {
		client.getSoundManager().stop(null, SoundSource.MUSIC);
		((MusicManagerAccessor) client.getMusicManager()).setCurrentMusic(null);
		MusicPlayerMod.currentlyPlaying = null;
		client.gui.toastManager().hideNowPlayingToast();
	}

	private static @Nullable SoundEvent getSoundEvent(SoundManager soundManager, Identifier location) {
		if (soundManager.getSoundEvent(location) != null) {
			return SoundEvent.createVariableRangeEvent(location);
		}
		return null;
	}
}
