package kat.blasted.music_player.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicManagerAccessor {
	@Accessor
	@Nullable
	SoundInstance getCurrentMusic();

	@Accessor
	void setCurrentMusic(@Nullable SoundInstance soundInstance);

	@Accessor
	float getCurrentGain();

	@Accessor
	void setCurrentGain(float currentGain);
}
