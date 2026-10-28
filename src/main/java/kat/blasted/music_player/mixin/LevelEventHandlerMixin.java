package kat.blasted.music_player.mixin;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.data.SoundListener;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelEventHandler.class)
public class LevelEventHandlerMixin {
	@Shadow
	@Final
	private Map<BlockPos, SoundInstance> playingJukeboxSongs;

	@Inject(method = "playJukeboxSong", at = @At("TAIL"))
	private void onPlayJukeboxSong(Holder<JukeboxSong> songHolder, BlockPos pos, CallbackInfo ci) {
		SoundInstance sound = this.playingJukeboxSongs.get(pos);
		if (sound != null) {
			SoundListener.addJukeboxSound(sound);
			MusicPlayerMod.LOGGER.info("LevelEventHandler: Jukebox started at {} with sound {}", pos, sound.getIdentifier());
		}
	}

	@Inject(method = "stopJukeboxSong", at = @At("HEAD"))
	private void onStopJukeboxSong(BlockPos pos, CallbackInfo ci) {
		SoundInstance sound = this.playingJukeboxSongs.get(pos);
		if (sound != null) {
			SoundListener.removeJukeboxSound(sound);
			MusicPlayerMod.LOGGER.info("LevelEventHandler: Jukebox stopped at {}", pos);
		}
	}
}
