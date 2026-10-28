package kat.blasted.music_player.mixin;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.NeverMusicMod;
import kat.blasted.music_player.data.SoundListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
	@Shadow
	private Minecraft minecraft;

	@Shadow
	private MusicManager.MusicFrequency gameMusicFrequency;

	@Shadow
	private SoundInstance currentMusic;

	@Shadow
	private float currentGain;

	@Shadow
	private int nextSongDelay;

	@Shadow
	public abstract void stopPlaying();

	/**
	 * Do not allow situational music to replace music manually started by the player.
	 */
	@Inject(method = "canReplace", at = @At("HEAD"), cancellable = true)
	private static void onCanReplace(Music music, SoundInstance sound, CallbackInfoReturnable<Boolean> cir) {
		if (sound != null && sound == MusicPlayerMod.currentlyPlaying) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * When the music frequency is NEVER, ensure vanilla NEVER starts playing automatic / situational music.
	 */
	@Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
	private void onStartPlaying(Music music, CallbackInfo ci) {
		if (this.gameMusicFrequency == NeverMusicMod.NEVER) {
			this.nextSongDelay = 10000;
			ci.cancel();
		}
	}

	/**
	 * Clear currentlyPlaying reference whenever music is stopped (e.g. after fading out or user action).
	 */
	@Inject(method = "stopPlaying()V", at = @At("HEAD"))
	private void onStopPlaying(CallbackInfo ci) {
		MusicPlayerMod.currentlyPlaying = null;
	}

	/**
	 * In tick():
	 * 1. If a jukebox is playing anywhere in the world, smoothly fade out any playing music (over ~1.25s),
	 *    then stop it completely and restore category volume to 1.0F.
	 * 2. When frequency is NEVER, prevent situational music from starting.
	 */
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void onTick(CallbackInfo ci) {
		if (SoundListener.isJukeboxPlaying(this.minecraft.getSoundManager())) {
			this.nextSongDelay = Math.max(this.nextSongDelay, 100);
			if (this.currentMusic != null) {
				// Smoothly fade out music track at 0.04F per tick (~1.25 seconds total)
				this.currentGain = Math.max(0.0F, this.currentGain - 0.04F);
				this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, this.currentGain);
				if (this.currentGain <= 0.001F) {
					MusicPlayerMod.LOGGER.info("Music track faded out completely due to jukebox playing. Stopping track.");
					this.stopPlaying();
					this.currentGain = 1.0F;
					this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, 1.0F);
					MusicPlayerMod.currentlyPlaying = null;
				}
				ci.cancel();
				return;
			}
			ci.cancel();
			return;
		}

		if (this.gameMusicFrequency == NeverMusicMod.NEVER) {
			if (this.currentMusic != null && this.currentMusic != MusicPlayerMod.currentlyPlaying) {
				this.stopPlaying();
			} else if (this.currentMusic != null && !this.minecraft.getSoundManager().isActive(this.currentMusic)) {
				this.currentMusic = null;
				MusicPlayerMod.currentlyPlaying = null;
			}
			this.nextSongDelay = 10000;
		}
	}

	@Inject(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/sounds/Music;"),
		cancellable = true
	)
	private void onGetSituationalMusic(CallbackInfo ci) {
		if (this.gameMusicFrequency == NeverMusicMod.NEVER) {
			this.nextSongDelay = 10000;
			ci.cancel();
		}
	}
}
