package kat.blasted.music_player.mixin;

import java.util.Arrays;
import kat.blasted.music_player.NeverMusicMod;
import net.minecraft.client.sounds.MusicManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into MusicManager.MusicFrequency enum to append the NEVER constant
 * to the enum's $VALUES array after it is initialized in the static initializer (<clinit>),
 * but before StringRepresentable.fromEnum builds CODEC.
 *
 * <p>Why this is necessary:
 * Java enums cannot have new constants added through standard Java language features.
 * In Minecraft 26.2, MusicFrequency is an enum with a static $VALUES array and builds
 * its Codec via {@code StringRepresentable.fromEnum(MusicManager.MusicFrequency::values)}.
 * By injecting immediately after $VALUES is assigned but before CODEC is constructed,
 * the Codec and the options cycle button automatically recognize NEVER without any
 * additional patches.
 */
@Mixin(MusicManager.MusicFrequency.class)
public abstract class MusicFrequencyMixin {
	/**
	 * Shadow the synthetic $VALUES array containing all enum constants.
	 * {@link Mutable} allows reassignment of this final field at runtime.
	 */
	@Shadow
	@Final
	@Mutable
	private static MusicManager.MusicFrequency[] $VALUES;

	/**
	 * Invoker for the enum's private constructor.
	 *
	 * <p>Enum constructors always take two synthetic compiler-generated arguments first:
	 * 1. {@code String internalName} - the name passed to {@link Enum#Enum(String, int)}
	 * 2. {@code int ordinal} - the ordinal index passed to {@link Enum#Enum(String, int)}
	 * Followed by the explicit parameters declared in {@code MusicFrequency}:
	 * 3. {@code String name} - serialized name used by {@link net.minecraft.util.StringRepresentable}
	 * 4. {@code String translationKey} - translation key for {@link net.minecraft.network.chat.Component#translatable}
	 * 5. {@code int maxFrequencyMinutes} - delay multiplier (0 for NEVER)
	 */
	@Invoker("<init>")
	private static MusicManager.MusicFrequency callInit(String internalName, int ordinal, String name, String translationKey, int maxFrequencyMinutes) {
		throw new AssertionError();
	}

	/**
	 * Inject into the static initializer (<clinit>) of MusicFrequency.
	 *
	 * <p>Target instruction:
	 * {@code PUTSTATIC net/minecraft/client/sounds/MusicManager$MusicFrequency.$VALUES : [Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;}
	 *
	 * <p>Using {@code shift = At.Shift.AFTER} ensures our code runs right after
	 * the default $VALUES array is created and assigned, but before the subsequent
	 * invokedynamic call that constructs CODEC via {@code StringRepresentable.fromEnum(MusicFrequency::values)}.
	 */
	@Inject(
		method = "<clinit>",
		at = @At(
			value = "FIELD",
			opcode = Opcodes.PUTSTATIC,
			target = "Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;$VALUES:[Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;",
			shift = At.Shift.AFTER
		)
	)
	private static void onClinit(CallbackInfo ci) {
		MusicManager.MusicFrequency[] oldValues = $VALUES;
		int ordinal = oldValues.length;

		// Create the new NEVER constant:
		// maxFrequencyMinutes is 0 as required (logic handled in MusicManagerMixin)
		MusicManager.MusicFrequency never = callInit("NEVER", ordinal, "NEVER", "options.music_frequency.never", 0);

		// Expand the enum $VALUES array to include NEVER
		MusicManager.MusicFrequency[] newValues = Arrays.copyOf(oldValues, ordinal + 1);
		newValues[ordinal] = never;
		$VALUES = newValues;

		// Save the constant to our mod helper class (Mixins cannot add new static fields to target classes)
		NeverMusicMod.NEVER = never;
		NeverMusicMod.LOGGER.info("Successfully added NEVER constant to MusicFrequency ($VALUES length: {})", newValues.length);
	}
}
