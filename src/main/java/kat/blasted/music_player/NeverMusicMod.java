package kat.blasted.music_player;

import net.minecraft.client.sounds.MusicManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main helper class for the Never Music mod.
 *
 * <p>Mixins cannot declare new static fields on target classes due to classloader
 * and transformer limitations. Therefore, the injected {@code NEVER} enum constant
 * reference is held here in a static field for easy and reliable access across mixins.
 */
public class NeverMusicMod {
	public static final String MOD_ID = "music_player";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Reference to the custom NEVER constant added to {@link MusicManager.MusicFrequency}.
	 */
	public static MusicManager.MusicFrequency NEVER;
}
